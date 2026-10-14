package com.cheatbreaker.client.kazerui.module.render;

import java.io.*;
import java.nio.charset.StandardCharsets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.opengl.*;

/** Donor post shader with a native, Fast Render compatible transport.
 * Raw fixed-function calls stay within push/popAttrib: MC's unchanged cache is
 * accurate again when driver state is restored. Buffers are reused, not per-frame. */
public final class PostShader {
    private static final String VERTEX = "#version 120\nvarying vec2 uv;\n"
        + "void main(){gl_Position=vec4(gl_Vertex.xy*2.0-1.0,0.2,1.0);uv=gl_Vertex.xy;}\n";
    private final String name;
    private int program, phosphor, diffuse, prevSampler;
    private int previous, scratch, capture, previousFbo, scratchFbo;
    private int width, height;
    private boolean ready, failed;
    public PostShader(String name,String[] uniforms,String... samplers){this.name=name;}
    public static boolean supported(){return OpenGlHelper.framebufferSupported && GLContext.getCapabilities().OpenGL20;}
    private String read() throws IOException {
        try(InputStream input=PostShader.class.getResourceAsStream("/assets/kazerui/shaders/post/"+name+".glsl")) {
            if(input==null)throw new FileNotFoundException(name);
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[4096];
            for(int n;(n=input.read(buffer))!=-1;)bytes.write(buffer,0,n);
            return new String(bytes.toByteArray(),StandardCharsets.UTF_8);
        }
    }
    private static int compileShader(int type,String source) {
        int shader=GL20.glCreateShader(type);GL20.glShaderSource(shader,source);GL20.glCompileShader(shader);
        if(GL20.glGetShaderi(shader,GL20.GL_COMPILE_STATUS)==GL11.GL_FALSE) {
            String message=GL20.glGetShaderInfoLog(shader,4096);GL20.glDeleteShader(shader);throw new IllegalStateException(message);
        }
        return shader;
    }
    private boolean compile() {
        if(program!=0)return true;if(failed)return false;
        int vertex=0,fragment=0,candidate=0;
        try {
            vertex=compileShader(GL20.GL_VERTEX_SHADER,VERTEX);
            fragment=compileShader(GL20.GL_FRAGMENT_SHADER,
                "#version 120\nvarying vec2 uv;\nuniform sampler2D DiffuseSampler;\nuniform sampler2D PrevSampler;\nuniform vec3 Phosphor;\n#define SAMPLE texture2D\n#define OUT_COLOR gl_FragColor\n"+read());
            candidate=GL20.glCreateProgram();GL20.glAttachShader(candidate,vertex);GL20.glAttachShader(candidate,fragment);GL20.glLinkProgram(candidate);
            if(GL20.glGetProgrami(candidate,GL20.GL_LINK_STATUS)==GL11.GL_FALSE)throw new IllegalStateException(GL20.glGetProgramInfoLog(candidate,4096));
            program=candidate;candidate=0;phosphor=GL20.glGetUniformLocation(program,"Phosphor");
            diffuse=GL20.glGetUniformLocation(program,"DiffuseSampler");prevSampler=GL20.glGetUniformLocation(program,"PrevSampler");return true;
        } catch(Exception error) {
            failed=true;org.apache.logging.log4j.LogManager.getLogger("KazerUi").warn("Post shader {} unavailable: {}",name,error.toString());return false;
        } finally {
            if(candidate!=0)GL20.glDeleteProgram(candidate);if(vertex!=0)GL20.glDeleteShader(vertex);if(fragment!=0)GL20.glDeleteShader(fragment);
        }
    }
    private static int texture(int w,int h) {
        int texture=GL11.glGenTextures();GL11.glBindTexture(GL11.GL_TEXTURE_2D,texture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_S,GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_T,GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL11.GL_RGBA8,w,h,0,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,(java.nio.ByteBuffer)null);return texture;
    }
    private static int framebuffer(int texture) {
        int fbo=OpenGlHelper.glGenFramebuffers();OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER,fbo);
        OpenGlHelper.glFramebufferTexture2D(OpenGlHelper.GL_FRAMEBUFFER,OpenGlHelper.GL_COLOR_ATTACHMENT0,GL11.GL_TEXTURE_2D,texture,0);
        if(OpenGlHelper.glCheckFramebufferStatus(OpenGlHelper.GL_FRAMEBUFFER)!=OpenGlHelper.GL_FRAMEBUFFER_COMPLETE) {
            OpenGlHelper.glDeleteFramebuffers(fbo);throw new IllegalStateException("Incomplete motion blur framebuffer");
        }
        return fbo;
    }
    private void match(int w,int h) {
        if(width==w && height==h && previous!=0)return;
        deleteBuffers();width=w;height=h;previous=texture(w,h);scratch=texture(w,h);capture=texture(w,h);
        previousFbo=framebuffer(previous);scratchFbo=framebuffer(scratch);
    }
    private static void quad(boolean textured) {
        GL11.glBegin(GL11.GL_QUADS);
        if(textured)GL11.glTexCoord2f(0,0);GL11.glVertex2f(0,0);
        if(textured)GL11.glTexCoord2f(1,0);GL11.glVertex2f(1,0);
        if(textured)GL11.glTexCoord2f(1,1);GL11.glVertex2f(1,1);
        if(textured)GL11.glTexCoord2f(0,1);GL11.glVertex2f(0,1);GL11.glEnd();
    }
    private void copy(int source,int target) {
        OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER,target);GL11.glBindTexture(GL11.GL_TEXTURE_2D,source);quad(true);
    }
    /** Four state queries only when enabled; no closures, varargs arrays or frame-guard allocations. */
    public void render(Minecraft mc,float accumulation,int type) {
        if(failed || !supported() || mc.displayWidth<=0 || mc.displayHeight<=0)return;
        if(!compile())return;
        int target=GL11.glGetInteger(36006),active=GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        int oldProgram=GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM),matrixMode=GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPushMatrix();GL11.glLoadIdentity();GL11.glOrtho(0,1,0,1,-1,1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPushMatrix();GL11.glLoadIdentity();
        try {
            // Fixed-function copies must not multiply by a stale lightmap or
            // secondary texture. Driver-only changes are restored by popAttrib.
            GL13.glActiveTexture(OpenGlHelper.defaultTexUnit+1);GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL13.glActiveTexture(OpenGlHelper.defaultTexUnit+2);GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL13.glActiveTexture(OpenGlHelper.defaultTexUnit);match(mc.displayWidth,mc.displayHeight);
            GL11.glViewport(0,0,width,height);GL11.glDisable(GL11.GL_DEPTH_TEST);GL11.glDepthMask(false);
            GL11.glDisable(GL11.GL_BLEND);GL11.glDisable(GL11.GL_ALPHA_TEST);GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_LIGHTING);GL11.glDisable(GL11.GL_FOG);GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);GL11.glDisable(GL11.GL_STENCIL_TEST);
            GL11.glTexEnvi(GL11.GL_TEXTURE_ENV,GL11.GL_TEXTURE_ENV_MODE,GL11.GL_REPLACE);
            GL11.glColor4f(1,1,1,1);GL11.glColorMask(true,true,true,true);
            OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER,target);
            int current;
            if(OpenGlHelper.isFramebufferEnabled())current=mc.getFramebuffer().framebufferTexture;
            else {GL11.glBindTexture(GL11.GL_TEXTURE_2D,capture);GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D,0,0,0,0,0,width,height);current=capture;}
            GL20.glUseProgram(0);
            if(!ready){copy(current,previousFbo);ready=true;return;}
            OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER,scratchFbo);
            GL20.glUseProgram(program);GL11.glBindTexture(GL11.GL_TEXTURE_2D,current);GL20.glUniform1i(diffuse,0);
            GL13.glActiveTexture(OpenGlHelper.defaultTexUnit+2);GL11.glBindTexture(GL11.GL_TEXTURE_2D,previous);GL20.glUniform1i(prevSampler,2);
            GL13.glActiveTexture(OpenGlHelper.defaultTexUnit);GL20.glUniform3f(phosphor,accumulation,type,0);quad(false);
            GL20.glUseProgram(0);copy(scratch,previousFbo);copy(scratch,target);
        } catch(RuntimeException error) {
            failed=true;deleteBuffers();org.apache.logging.log4j.LogManager.getLogger("KazerUi").warn("Motion blur stopped after a render failure",error);
        } finally {
            GL20.glUseProgram(oldProgram);GL13.glActiveTexture(active);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPopMatrix();GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPopMatrix();GL11.glMatrixMode(matrixMode);
            OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER,target);GL11.glPopAttrib();
        }
    }
    public void reset(){ready=false;}
    private void deleteBuffers() {
        if(previousFbo!=0)OpenGlHelper.glDeleteFramebuffers(previousFbo);if(scratchFbo!=0)OpenGlHelper.glDeleteFramebuffers(scratchFbo);
        if(previous!=0)GL11.glDeleteTextures(previous);if(scratch!=0)GL11.glDeleteTextures(scratch);if(capture!=0)GL11.glDeleteTextures(capture);
        previousFbo=scratchFbo=previous=scratch=capture=width=height=0;ready=false;
    }
    public void delete(){deleteBuffers();if(program!=0)GL20.glDeleteProgram(program);program=0;failed=false;}
    public boolean hasResources(){return program!=0 || previous!=0 || scratch!=0 || capture!=0;}
}
