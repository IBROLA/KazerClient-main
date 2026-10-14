package cn.kazer.client189.runtime;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import java.io.*;
import static org.junit.Assert.*;

public class RuntimeTransformerTest {
    @Test public void optifineSkyAndRadialAdaptersRestoreMatricesAndAreIdempotent() throws Exception {
        org.objectweb.asm.ClassWriter writer=new org.objectweb.asm.ClassWriter(0);
        String owner="net/minecraft/client/renderer/RenderGlobal";
        writer.visit(52,org.objectweb.asm.Opcodes.ACC_PUBLIC,owner,null,"java/lang/Object",null);
        writer.visitField(org.objectweb.asm.Opcodes.ACC_PRIVATE,"renderDistanceSq","I",null,null).visitEnd();
        org.objectweb.asm.MethodVisitor sky=writer.visitMethod(org.objectweb.asm.Opcodes.ACC_PUBLIC,"renderSky","(FI)V",null,null);
        sky.visitCode();sky.visitInsn(org.objectweb.asm.Opcodes.ACONST_NULL);sky.visitInsn(org.objectweb.asm.Opcodes.ACONST_NULL);sky.visitVarInsn(org.objectweb.asm.Opcodes.FLOAD,1);
        sky.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESTATIC,"net/optifine/CustomSky","renderSky","(Lnet/minecraft/world/World;Lnet/minecraft/client/renderer/texture/TextureManager;F)V",false);
        sky.visitInsn(org.objectweb.asm.Opcodes.RETURN);sky.visitMaxs(3,3);sky.visitEnd();
        org.objectweb.asm.MethodVisitor chunks=writer.visitMethod(org.objectweb.asm.Opcodes.ACC_PRIVATE,"getRenderChunkOffset","(Lnet/minecraft/util/BlockPos;Lnet/minecraft/client/renderer/chunk/RenderChunk;Lnet/minecraft/util/EnumFacing;ZI)Lnet/minecraft/client/renderer/chunk/RenderChunk;",null,null);
        org.objectweb.asm.Label end=new org.objectweb.asm.Label();chunks.visitCode();chunks.visitVarInsn(org.objectweb.asm.Opcodes.ILOAD,4);chunks.visitJumpInsn(org.objectweb.asm.Opcodes.IFEQ,end);
        chunks.visitInsn(org.objectweb.asm.Opcodes.ICONST_0);chunks.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD,0);chunks.visitFieldInsn(org.objectweb.asm.Opcodes.GETFIELD,owner,"renderDistanceSq","I");chunks.visitJumpInsn(org.objectweb.asm.Opcodes.IF_ICMPLE,end);
        chunks.visitInsn(org.objectweb.asm.Opcodes.ACONST_NULL);chunks.visitInsn(org.objectweb.asm.Opcodes.ARETURN);chunks.visitLabel(end);chunks.visitFrame(org.objectweb.asm.Opcodes.F_SAME,0,null,0,null);
        chunks.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD,2);chunks.visitInsn(org.objectweb.asm.Opcodes.ARETURN);chunks.visitMaxs(2,6);chunks.visitEnd();writer.visitEnd();
        RuntimeTransformer transformer=new RuntimeTransformer();String target=owner.replace('/','.');
        byte[] once=transformer.transform(target,target,writer.toByteArray());ClassNode type=new ClassNode();new ClassReader(transformer.transform(target,target,once)).accept(type,0);
        int wrappers=0,restores=0,visibility=0,radialLoads=0;
        for(MethodNode method:type.methods) {
            if(method.name.equals("kazerCustomSky")) { wrappers++;assertEquals(1,method.tryCatchBlocks.size()); }
            for(AbstractInsnNode insn:method.instructions.toArray()) {
                if(insn instanceof MethodInsnNode) { String name=((MethodInsnNode)insn).name;if(name.equals("customSkyEnd"))restores++;if(name.equals("visibleChunk"))visibility++; }
                if(method.name.equals("getRenderChunkOffset")&&insn instanceof VarInsnNode&&insn.getOpcode()==org.objectweb.asm.Opcodes.ILOAD&&((VarInsnNode)insn).var==4)radialLoads++;
            }
            new Analyzer<BasicValue>(new BasicInterpreter()).analyze(type.name,method);
        }
        assertEquals(1,wrappers);assertEquals(2,restores);assertEquals(2,visibility);assertEquals(0,radialLoads);
    }
    @Test public void ordinaryFogHookPreservesNativeBranchesAndIsNotDuplicated() throws Exception {
        String target="net.minecraft.client.renderer.EntityRenderer";
        RuntimeTransformer transformer=new RuntimeTransformer();
        byte[] once=transformer.transform(target,target,read("/"+target.replace('.','/')+".class"));
        ClassNode type=new ClassNode();new ClassReader(transformer.transform(target,target,once)).accept(type,0);
        int fog=0,hooks=0;
        for(MethodNode method:type.methods)if(method.name.equals("setupFog")||method.name.equals("func_78468_a")) {
            for(AbstractInsnNode insn:method.instructions.toArray())if(insn instanceof MethodInsnNode) {
                MethodInsnNode call=(MethodInsnNode)insn;
                if(call.name.equals("clearDistanceFog"))hooks++;
                if(call.owner.equals("net/minecraft/client/renderer/GlStateManager")&&call.name.toLowerCase().contains("fog"))fog++;
            }
            new Analyzer<BasicValue>(new BasicInterpreter()).analyze(type.name,method);
        }
        assertEquals(1,hooks);
        // Native water, lava and blindness setup stays present; the owned hook
        // gates on the normal-air fogStandard state only.
        assertTrue(fog>0);
    }
    @Test public void squareChunkBoundaryNeverAddsExtraDistance() {
        for(int distance:new int[]{2,4,8,16}) {
            assertTrue(RuntimeHooks.chunkVisible(-30+distance,20+distance,-30,20,distance));
            assertTrue(RuntimeHooks.chunkVisible(-30-distance,20-distance,-30,20,distance));
            assertFalse(RuntimeHooks.chunkVisible(-30+distance+1,20,-30,20,distance));
        }
        assertFalse(RuntimeHooks.chunkVisible(Integer.MAX_VALUE,0,Integer.MIN_VALUE,0,2));
    }
    @Test public void optifineDisplayRepairIsSkippedOnlyForOwnedBorderlessWindow() throws Exception {
        org.objectweb.asm.ClassWriter writer=new org.objectweb.asm.ClassWriter(0);
        writer.visit(52,org.objectweb.asm.Opcodes.ACC_PUBLIC,"net/minecraft/client/renderer/EntityRenderer",null,"java/lang/Object",null);
        org.objectweb.asm.MethodVisitor method=writer.visitMethod(org.objectweb.asm.Opcodes.ACC_PRIVATE,"frameInit","()V",null,null);
        method.visitCode();method.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESTATIC,"Config","checkDisplayMode","()V",false);
        method.visitInsn(org.objectweb.asm.Opcodes.RETURN);method.visitMaxs(0,1);method.visitEnd();writer.visitEnd();
        String target="net.minecraft.client.renderer.EntityRenderer";ClassNode type=new ClassNode();
        new ClassReader(new RuntimeTransformer().transform(target,target,writer.toByteArray())).accept(type,0);
        MethodNode changed=type.methods.get(0);int guards=0,repairs=0;
        for(AbstractInsnNode insn:changed.instructions.toArray())if(insn instanceof MethodInsnNode) {
            MethodInsnNode call=(MethodInsnNode)insn;if(call.name.equals("active"))guards++;if(call.name.equals("checkDisplayMode"))repairs++;
        }
        assertEquals(1,guards);assertEquals("Exclusive OptiFine repair stays intact",1,repairs);
        new Analyzer<BasicValue>(new BasicInterpreter()).analyze(type.name,changed);
    }
    @Test public void lowViewHorizonGuardMatchesRealMinecraftSkyBytecode() throws Exception {
        String name="net.minecraft.client.renderer.RenderGlobal";
        byte[] transformed=new RuntimeTransformer().transform(name,name,read("/"+name.replace('.','/')+".class"));
        ClassNode type=new ClassNode();new ClassReader(transformed).accept(type,0);int matches=0;
        for(MethodNode method:type.methods)for(AbstractInsnNode instruction:method.instructions.toArray())if(instruction instanceof MethodInsnNode){
            MethodInsnNode call=(MethodInsnNode)instruction;
            if(call.name.equals("horizonBoxHeight")){assertEquals("(D)D",call.desc);matches++;}
        }
        assertEquals("Exactly the obsolete horizon-box comparison is adapted",1,matches);
    }
    @Test public void adaptersHaveValidStacksOnOfficialForgeClasses() throws Exception {
        String[] targets = {"net.minecraft.client.gui.GuiIngame", "net.minecraft.client.gui.GuiNewChat", "net.minecraft.client.gui.GuiChat", "net.minecraft.client.gui.ChatLine",
                "net.minecraft.client.network.NetHandlerPlayClient", "net.minecraft.client.renderer.EntityRenderer", "net.minecraft.client.renderer.RenderGlobal",
                "net.minecraft.client.particle.EffectRenderer", "net.minecraft.client.Minecraft",
                "net.minecraft.client.gui.GuiScreen", "net.minecraft.client.gui.GuiScreenResourcePacks", "net.minecraftforge.fml.client.config.GuiUtils", "net.minecraft.client.gui.GuiTextField", "net.minecraft.client.renderer.InventoryEffectRenderer",
                "net.minecraft.client.gui.achievement.GuiAchievement", "net.minecraft.entity.player.InventoryPlayer",
                "net.minecraft.client.renderer.entity.RenderManager",
                "net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher",
                "net.minecraft.client.renderer.entity.RenderItem", "net.minecraft.client.gui.GuiPlayerTabOverlay",
                "net.minecraft.client.renderer.BlockRendererDispatcher", "net.minecraft.client.settings.GameSettings",
                "net.minecraft.client.multiplayer.ServerList","net.minecraft.client.gui.ServerSelectionList","net.minecraft.client.gui.GuiMultiplayer",
                "net.minecraft.client.settings.KeyBinding", "net.minecraft.client.entity.EntityPlayerSP"};
        for (String target : targets) {
            byte[] original = read("/" + target.replace('.', '/') + ".class");
            byte[] patched = new RuntimeTransformer().transform(target, target, original);
            assertNotSame(target + " must be adapted", original, patched);
            ClassNode type = new ClassNode(); new ClassReader(patched).accept(type, 0);
            for (MethodNode method : type.methods) if (method.instructions.size() > 0)
                new Analyzer<BasicValue>(new BasicInterpreter()).analyze(type.name, method);
        }
    }
    @Test public void keyStateAdapterTargetsBindingInstanceNotCodeLookup() throws Exception {
        String name="net.minecraft.client.settings.KeyBinding";
        byte[] original=read("/"+name.replace('.','/')+".class");
        ClassNode type=new ClassNode();
        new ClassReader(new RuntimeTransformer().transform(name,name,original)).accept(type,0);
        assertTrue(type.interfaces.contains("com/cheatbreaker/client/kazerui/module/modules/mechanic/KazerKeyState"));
        MethodNode setter=null;
        for(MethodNode method:type.methods)if(method.name.equals("kazerSetPressed"))setter=method;
        assertNotNull(setter);assertEquals("(Z)V",setter.desc);
        int stores=0;
        for(AbstractInsnNode instruction=setter.instructions.getFirst();instruction!=null;instruction=instruction.getNext()) {
            assertFalse("No static code-keyed lookup",instruction instanceof MethodInsnNode);
            if(instruction instanceof FieldInsnNode) {
                FieldInsnNode field=(FieldInsnNode)instruction;
                assertEquals(org.objectweb.asm.Opcodes.PUTFIELD,field.getOpcode());
                assertEquals(type.name,field.owner);assertEquals("Z",field.desc);stores++;
            }
        }
        assertEquals(1,stores);
    }
    @Test public void chatAdaptersUseDirectNativeFieldsAndAreIdempotent() throws Exception {
        for(String name:new String[]{"net.minecraft.client.gui.GuiNewChat","net.minecraft.client.gui.ChatLine"}) {
            RuntimeTransformer transformer=new RuntimeTransformer();
            byte[] once=transformer.transform(name,name,read("/"+name.replace('.','/')+".class"));
            byte[] twice=transformer.transform(name,name,once);
            ClassNode type=new ClassNode();new ClassReader(twice).accept(type,0);
            String bridge=name.endsWith("ChatLine")?"ChatLineTag":"KazerChatAccess";
            assertTrue(type.interfaces.contains("com/cheatbreaker/client/kazerui/module/render/"+bridge));
            java.util.Set<String> names=new java.util.HashSet<String>();int getters=0,setters=0;
            for(MethodNode method:type.methods) if(method.name.startsWith("kazer")) {
                assertTrue("No duplicate adapter methods",names.add(method.name+method.desc));
                for(AbstractInsnNode node:method.instructions.toArray()) {
                    assertFalse("No reflection or allocation in getters",node instanceof MethodInsnNode || node instanceof TypeInsnNode);
                    if(node instanceof FieldInsnNode) {
                        FieldInsnNode field=(FieldInsnNode)node;assertEquals(type.name,field.owner);
                        if(field.getOpcode()==org.objectweb.asm.Opcodes.GETFIELD)getters++;
                        if(field.getOpcode()==org.objectweb.asm.Opcodes.PUTFIELD)setters++;
                    }
                }
            }
            assertEquals(name.endsWith("ChatLine")?1:4,getters);
            assertEquals("Both tag IDs and the native scrolled flag have one direct setter",1,setters);
        }
    }
    private byte[] read(String name) throws IOException {
        try (InputStream input = getClass().getResourceAsStream(name)) {
            assertNotNull(name, input); ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192]; int count;
            while ((count = input.read(buffer)) >= 0) bytes.write(buffer, 0, count);
            return bytes.toByteArray();
        }
    }
    @Test public void statusRowsFollowNameWithoutMutatingProtocolOrder() {
        java.util.List<String> wire = java.util.Arrays.asList("\u00a77[Mod Mode]", "\u00a7cCANRX");
        assertEquals(java.util.Arrays.asList("\u00a7cCANRX", "\u00a77[Mod Mode]"),
                KazerNametags.displayLines(wire, "CANRX", "CANRX"));
        assertEquals("\u00a77[Mod Mode]", wire.get(0));
        assertEquals(java.util.Collections.singletonList("CANRX"), KazerNametags.displayLines(null, "CANRX", "CANRX"));
        assertTrue(KazerNametags.displayLines(java.util.Collections.<String>emptyList(),"CANRX","CANRX").isEmpty());
    }
    @Test public void shaderMatrixPreservesVanillaRowMajorUniformContract() {
        javax.vecmath.Matrix4f matrix=new javax.vecmath.Matrix4f();matrix.setIdentity();
        matrix.m03=-1F;matrix.m13=1F;matrix.m23=-1.0001999F;
        org.lwjgl.util.vector.Matrix4f uniform=cn.kazer.client189.compat.KazerCompat.shaderMatrix(matrix);
        assertEquals(-1F,uniform.m03,0F);assertEquals(1F,uniform.m13,0F);
        assertEquals(-1.0001999F,uniform.m23,0F);assertEquals(0F,uniform.m30,0F);
    }
    @Test public void apolloThreeLinesRetainBottomToTopOrderAndNameAnchor() {
        java.util.List<String> wire=java.util.Arrays.asList("[Mod Mode]","CANRX","20 Health");
        java.util.List<String> display=KazerNametags.displayLines(wire,"CANRX","CANRX");
        assertEquals(java.util.Arrays.asList("CANRX","[Mod Mode]","20 Health"),display);
        assertEquals(0,com.cheatbreaker.client.module.type.nametag.NametagLayout.primaryLine(display,"CANRX"));
        assertEquals("[Mod Mode]",wire.get(0));
    }
}
