package cn.kazer.client189.runtime;

import java.io.*;
import java.util.*;
import org.junit.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import static org.junit.Assert.*;

public class SevenVisualTransformerTest {
    private byte[] original(String target) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (InputStream in = getClass().getResourceAsStream("/" + target.replace('.','/') + ".class")) {
            assertNotNull(target,in);
            byte[] buffer = new byte[8192]; for(int n;(n=in.read(buffer))>0;) out.write(buffer,0,n);
        }
        return out.toByteArray();
    }
    @Test public void realForgeClassesWireEveryRequestedHookAndAreIdempotent() throws Exception {
        String[] targets = {"net.minecraft.client.particle.EntityFX","net.minecraft.client.particle.EntityDiggingFX",
            "net.minecraft.client.particle.EntityBreakingFX","net.minecraft.client.particle.EffectRenderer",
            "net.minecraft.client.renderer.EntityRenderer","net.minecraft.client.renderer.RenderGlobal",
            "net.minecraft.client.renderer.entity.RenderItem","net.minecraft.client.renderer.entity.RenderEntityItem",
            "net.minecraft.client.renderer.entity.layers.LayerArmorBase","net.minecraft.client.gui.GuiScreen","net.minecraft.client.audio.SoundHandler"};
        Set<String> hooks = new HashSet<String>();
        SevenVisualTransformer transformer = new SevenVisualTransformer();
        for(String target : targets) {
            byte[] input = original(target), output = transformer.transform(target,target,input);
            assertNotSame("Actually wire " + target,input,output);
            assertSame(output,transformer.transform(target,target,output));
            ClassNode type = new ClassNode(); new ClassReader(output).accept(type,0);
            for(MethodNode method:type.methods) {
                if(method.instructions.size()>0) new Analyzer<BasicValue>(new BasicInterpreter()).analyze(type.name,method);
                for(AbstractInsnNode insn:method.instructions.toArray()) if(insn instanceof MethodInsnNode) {
                    MethodInsnNode call=(MethodInsnNode)insn;
                    if(call.owner.contains("SevenVisualHooks")||call.owner.contains("ModuleParticleChanger")||call.owner.contains("ModuleWeatherChanger")||call.owner.contains("ModuleGlintColorizer")) hooks.add(call.name);
                }
            }
        }
        for(String hook:new String[]{"keep","scale","channel","itemColor","armorColor","itemTranslate","itemRotate","tooltip","strength","rainSoundStrength","precipitation","temperature","muteSound","thunder"})
            assertTrue("Missing " + hook,hooks.contains(hook));
    }
    @Test public void composesAfterExistingRuntimeAndOverlayAdapters() throws Exception {
        for(String target:new String[]{"net.minecraft.client.renderer.EntityRenderer","net.minecraft.client.renderer.entity.RenderEntityItem","net.minecraft.client.renderer.entity.layers.LayerArmorBase","net.minecraft.client.gui.GuiScreen"}) {
            byte[] input=original(target);
            input=new RuntimeTransformer().transform(target,target,input);
            input=new OverlayTransformer().transform(target,target,input);
            byte[] output=new SevenVisualTransformer().transform(target,target,input);
            assertNotSame(target,input,output);
            ClassNode type=new ClassNode();new ClassReader(output).accept(type,0);
            for(MethodNode method:type.methods)if(method.instructions.size()>0)new Analyzer<BasicValue>(new BasicInterpreter()).analyze(type.name,method);
        }
    }
    @Test public void changedMethodsAndUnrelatedClassesStayOriginal() {
        ClassWriter writer=new ClassWriter(0);writer.visit(52,Opcodes.ACC_PUBLIC,"fixture/Changed",null,"java/lang/Object",null);
        MethodVisitor method=writer.visitMethod(Opcodes.ACC_PUBLIC,"renderParticle","(I)V",null,null);
        method.visitCode();method.visitInsn(Opcodes.RETURN);method.visitMaxs(0,2);method.visitEnd();writer.visitEnd();
        byte[] input=writer.toByteArray();SevenVisualTransformer transformer=new SevenVisualTransformer();
        assertSame(input,transformer.transform("changed","net.minecraft.client.particle.EntityFX",input));
        assertSame(input,transformer.transform("changed","net.minecraft.world.World",input));
    }
}
