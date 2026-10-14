package cn.kazer.client189.animation;

import cn.kazer.client189.runtime.RuntimeTransformer;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import java.io.*;
import static org.junit.Assert.*;

public class BlockHitTransformerTest {
    private ClassNode patched(String target) throws Exception {
        return patched(target,false);
    }
    private ClassNode patched(String target,boolean productionNames) throws Exception {
        byte[] bytes;
        try(InputStream in=getClass().getResourceAsStream("/"+target.replace('.','/')+".class")) {
            assertNotNull(target,in);ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;
            while((n=in.read(b))!=-1)out.write(b,0,n);bytes=out.toByteArray();
        }
        if(productionNames) {
            final java.util.Properties mapping=new java.util.Properties();
            try(InputStream in=getClass().getResourceAsStream("/oldanimations-stable22.properties")) {mapping.load(in);}
            org.objectweb.asm.ClassWriter renamed=new org.objectweb.asm.ClassWriter(0);
            new ClassReader(bytes).accept(new org.objectweb.asm.commons.RemappingClassAdapter(renamed,
                    new org.objectweb.asm.commons.Remapper() {
                        @Override public String mapMethodName(String owner,String name,String desc) {return mapping.getProperty(name+desc,name);}
                        @Override public String mapFieldName(String owner,String name,String desc) {return mapping.getProperty(name,name);}
                    }),ClassReader.EXPAND_FRAMES);
            bytes=renamed.toByteArray();
        }
        byte[] changed=new BlockHitTransformer().transform(target,target,bytes);assertNotSame(target,bytes,changed);
        changed=new RuntimeTransformer().transform(target,target,changed);
        ClassNode type=new ClassNode();new ClassReader(changed).accept(type,0);
        for(MethodNode m:type.methods)if(m.instructions.size()>0)new Analyzer<BasicValue>(new BasicInterpreter()).analyze(type.name,m);
        return type;
    }
    private int calls(ClassNode type,String bridge) {
        int count=0;for(MethodNode m:type.methods)for(AbstractInsnNode n:m.instructions.toArray())if(n instanceof MethodInsnNode) {
            MethodInsnNode c=(MethodInsnNode)n;
            if(c.owner.equals("cn/kazer/client189/animation/OldAnimationsAdapter")&&c.name.equals(bridge))count++;
        }return count;
    }
    @Test public void exactUpstreamHookCoverageAndValidCombinedStacks() throws Exception {
        ClassNode renderer=patched("net.minecraft.client.renderer.ItemRenderer");
        assertEquals(3,calls(renderer,"useSwing"));assertEquals(1,calls(renderer,"skipNoneTransform"));
        assertEquals(1,calls(renderer,"renderFirstPerson"));assertEquals(1,calls(renderer,"renderedUseCount"));
        assertEquals(1,calls(renderer,"bowScale"));assertEquals(1,calls(renderer,"equipClamp"));
        // Actual drinking/bow use counters MUST remain native, outside the first-person branch predicate.
        int nativeTimers=0;
        for(MethodNode m:renderer.methods)if(m.name.equals("performDrinking")||m.name.equals("doBowTransformations"))
            for(AbstractInsnNode n:m.instructions.toArray())if(n instanceof MethodInsnNode
                    &&((MethodInsnNode)n).name.equals("getItemInUseCount"))nativeTimers++;
        assertEquals(2,nativeTimers);
        ClassNode mc=patched("net.minecraft.client.Minecraft");assertEquals(1,calls(mc,"beforeRightClick"));assertEquals(2,calls(mc,"cancelBlockClick"));
        assertEquals(1,calls(patched("net.minecraft.client.model.ModelBiped"),"blockingArmYaw"));
        assertEquals(1,calls(patched("net.minecraft.client.renderer.entity.RenderItem"),"thirdPersonPose"));
        assertEquals(1,calls(patched("net.minecraft.client.renderer.entity.layers.LayerArmorBase"),"redArmor"));
        ClassNode camera=patched("net.minecraft.client.renderer.EntityRenderer");
        assertEquals(1,calls(camera,"cameraEye"));assertEquals(1,calls(camera,"directionEye"));
        assertEquals(1,calls(patched("net.minecraftforge.client.GuiIngameForge"),"healthFlash"));
    }
    @Test public void untouchedTargetsStayNative() {
        byte[] b={1,2,3};assertSame(b,new BlockHitTransformer().transform("other","other",b));
        assertNull(new BlockHitTransformer().transform("other","other",null));
    }
    @Test public void productionSrgNamesHaveIdenticalAnimationHookCoverage() throws Exception {
        for(String target:new String[]{"net.minecraft.client.renderer.ItemRenderer","net.minecraft.client.Minecraft",
                "net.minecraft.client.model.ModelBiped","net.minecraft.client.renderer.entity.RenderItem",
                "net.minecraft.client.renderer.entity.layers.LayerArmorBase","net.minecraft.client.renderer.EntityRenderer",
                "net.minecraftforge.client.GuiIngameForge"}) {
            ClassNode dev=patched(target),production=patched(target,true);
            for(String hook:new String[]{"useSwing","skipNoneTransform","renderFirstPerson","renderedUseCount","equipClamp",
                    "bowScale","thirdPersonPose","blockingArmYaw","beforeRightClick","cameraEye","directionEye","redArmor","healthFlash"})
                assertEquals(target+" "+hook,calls(dev,hook),calls(production,hook));
        }
    }
    @Test public void swingFinishesAfterReleaseWithoutNewAirLoop() {
        OldSwingState state=new OldSwingState();state.tick(true,6);assertEquals(0,state.interpolate(.5F,.7F),0);
        state.tick(false,6);assertEquals(1F/12,state.interpolate(.5F,.7F),.00001F);
        for(int i=0;i<6;i++)state.tick(false,6);
        assertFalse(state.isSwinging());assertEquals(.7F,state.interpolate(.5F,.7F),0);
        for(int i=0;i<100;i++)state.tick(false,6);
        assertFalse(state.isSwinging());
    }
    @Test public void heldBlockAttackRestartsAtHalfAndInterpolatesWrap() {
        OldSwingState state=new OldSwingState();
        for(int i=0;i<4;i++)state.tick(true,6);
        assertEquals(5F/12,state.interpolate(.5F,.9F),.00001F);
        state.tick(true,6);assertEquals(.75F,state.interpolate(.5F,.9F),.00001F);
        state.reset();assertEquals(.9F,state.interpolate(.5F,.9F),0);
    }
    @Test public void hasteAndFatigueDurationsUseTheSameStateMachine() {
        for(int duration:new int[]{3,4,5,6,8,10,12}) {
            OldSwingState state=new OldSwingState();state.tick(true,duration);
            for(int i=1;i<duration;i++) { state.tick(false,duration);assertTrue(state.isSwinging()); }
            state.tick(false,duration);assertFalse(state.isSwinging());
        }
    }
    @Test public void oldVersionPreferencesMigrateWithoutRetainingFakeSwingOption() {
        assertTrue(BlockHitModule.preference("1.7"));assertFalse(BlockHitModule.preference("1.8"));
        assertEquals("Use Item While Digging",BlockHitModule.previousLabel("Punching During Usage"));
        assertNull(BlockHitModule.previousLabel("Always Swing"));
    }
    @Test public void animationHotPathsDoNotAllocateOrTraverseTheModuleSettings() throws Exception {
        try(InputStream in=getClass().getResourceAsStream("/cn/kazer/client189/animation/OldAnimationsAdapter.class")) {
            ClassNode type=new ClassNode();new ClassReader(in).accept(type,0);
            for(MethodNode m:type.methods)if(!m.name.equals("<clinit>")&&!m.name.equals("alwaysEdible"))for(AbstractInsnNode n:m.instructions.toArray()) {
                assertFalse(m.name+" must not allocate animation state",n.getOpcode()==org.objectweb.asm.Opcodes.NEW);
                if(n instanceof MethodInsnNode) {
                    MethodInsnNode c=(MethodInsnNode)n;
                    assertFalse(m.name+" must use the option snapshot",c.name.equals("module")||c.owner.equals("com/cheatbreaker/client/config/Setting"));
                }
            }
        }
    }
}
