package cn.kazer.client189.runtime;
import java.io.*;
import org.junit.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import static org.junit.Assert.*;
public class OverlayTransformerTest {
    @Test public void realForgeRenderClassesVerifyAndRepeatIsIdempotent() throws Exception {
        String[] targets={"net.minecraft.client.renderer.entity.Render","net.minecraft.client.renderer.entity.RenderArrow","net.minecraft.entity.projectile.EntityArrow",
            "net.minecraft.client.renderer.entity.layers.LayerArrow","net.minecraft.client.renderer.entity.layers.LayerArmorBase","net.minecraft.client.renderer.entity.layers.LayerCustomHead",
            "net.minecraft.client.renderer.entity.RendererLivingEntity","net.minecraft.client.renderer.tileentity.TileEntitySkullRenderer",
            "net.minecraft.client.renderer.tileentity.TileEntityEndPortalRenderer","net.minecraft.client.gui.GuiIngame","net.minecraft.client.renderer.EntityRenderer",
            "net.minecraft.client.renderer.ItemRenderer","net.minecraft.client.model.ModelHorse","net.minecraft.client.resources.ResourcePackListEntry",
            "net.minecraft.client.renderer.BlockRendererDispatcher","net.minecraft.client.renderer.texture.SimpleTexture","net.minecraft.client.renderer.entity.RenderFish","net.minecraft.block.BlockBarrier"};
        OverlayTransformer transformer=new OverlayTransformer();
        for(String target:targets) {
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            try(InputStream in=getClass().getResourceAsStream("/"+target.replace('.','/')+".class")){assertNotNull(target,in);byte[] buffer=new byte[8192];for(int n;(n=in.read(buffer))>0;)out.write(buffer,0,n);}
            byte[] original=out.toByteArray(),changed=transformer.transform(target,target,original);
            assertNotSame("Adapter must actually wire "+target,original,changed);
            ClassNode node=new ClassNode();new ClassReader(changed).accept(node,0);
            for(MethodNode method:node.methods)if(method.instructions.size()>0)new Analyzer<BasicValue>(new BasicInterpreter()).analyze(node.name,method);
            assertSame("No duplicate scaling/guards "+target,changed,transformer.transform(target,target,changed));
        }
    }
    @Test public void unrelatedAndChangedSignaturesRemainUnmodified() {
        ClassWriter writer=new ClassWriter(0);writer.visit(52,Opcodes.ACC_PUBLIC,"verification/Changed",null,"java/lang/Object",null);
        MethodVisitor method=writer.visitMethod(Opcodes.ACC_PUBLIC,"renderTileEntityAt","(I)V",null,null);method.visitCode();method.visitInsn(Opcodes.RETURN);method.visitMaxs(0,2);method.visitEnd();writer.visitEnd();
        byte[] bytes=writer.toByteArray();OverlayTransformer transformer=new OverlayTransformer();
        assertSame(bytes,transformer.transform("changed","net.minecraft.client.renderer.tileentity.TileEntitySkullRenderer",bytes));
        assertSame(bytes,transformer.transform("changed","net.minecraft.client.renderer.entity.RenderZombie",bytes));
    }
}
