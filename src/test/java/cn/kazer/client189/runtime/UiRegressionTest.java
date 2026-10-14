package cn.kazer.client189.runtime;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import java.io.*;
import java.util.*;
import static org.junit.Assert.*;

public class UiRegressionTest {
    private ClassNode transformed(String target) throws IOException {
        try(InputStream input=getClass().getResourceAsStream("/"+target.replace('.','/')+".class")) {
            assertNotNull(input);ByteArrayOutputStream buffer=new ByteArrayOutputStream();
            byte[] chunk=new byte[8192];int count;while((count=input.read(chunk))>0)buffer.write(chunk,0,count);
            ClassNode result=new ClassNode();new ClassReader(new RuntimeTransformer().transform(target,target,buffer.toByteArray())).accept(result,0);
            return result;
        }
    }
    @Test public void menusRetainVanillaMouseCentering() throws Exception {
        ClassNode mouse=transformed("net.minecraft.util.MouseHelper");
        int warps=0;
        for(MethodNode method:mouse.methods) for(AbstractInsnNode insn:method.instructions.toArray())
            if(insn instanceof MethodInsnNode&&((MethodInsnNode)insn).name.equals("setCursorPosition"))warps++;
        assertEquals(1,warps);
    }
    @Test public void tabHasNoShadowIncludingScoreColumnsAndGuardsPing() throws Exception {
        ClassNode tab=transformed("net.minecraft.client.gui.GuiPlayerTabOverlay");int guards=0;
        for(MethodNode method:tab.methods)for(AbstractInsnNode insn:method.instructions.toArray())
            if(insn instanceof MethodInsnNode) {
                MethodInsnNode call=(MethodInsnNode)insn;
                assertFalse(call.name.equals("drawStringWithShadow")||call.name.equals("func_175063_a"));
                if(call.name.equals("tabPing"))guards++;
            }
        assertEquals(1,guards);
    }
    @Test public void onlyNegativeLatencyHidesNativePingBars() {
        net.minecraft.client.network.NetworkPlayerInfo player=new net.minecraft.client.network.NetworkPlayerInfo(
                new com.mojang.authlib.GameProfile(UUID.randomUUID(),"CANRX"));
        net.minecraftforge.fml.relauncher.ReflectionHelper.setPrivateValue(
                net.minecraft.client.network.NetworkPlayerInfo.class,player,-1,"responseTime","field_78829_b");
        assertFalse(RuntimeHooks.tabPing(player));
        net.minecraftforge.fml.relauncher.ReflectionHelper.setPrivateValue(
                net.minecraft.client.network.NetworkPlayerInfo.class,player,40,"responseTime","field_78829_b");
        assertTrue(RuntimeHooks.tabPing(player));
        player.setDisplayName(new net.minecraft.util.ChatComponentText("Leaderboard"));
        assertTrue("Display-name overrides must not hide real native ping bars",RuntimeHooks.tabPing(player));
    }
    @Test public void inventoryLabelsScaleWithModelWithoutExtraShrinking() {
        assertEquals(com.cheatbreaker.client.module.type.nametag.NametagLayout.WORLD_SCALE,
                com.cheatbreaker.client.module.type.nametag.NametagLayout.INVENTORY_SCALE,0F);
        assertEquals(.025F,com.cheatbreaker.client.module.type.nametag.NametagLayout.inventoryScale(2),0F);
        assertTrue(com.cheatbreaker.client.module.type.nametag.NametagLayout.inventoryScale(3)>.020F);
        assertTrue(com.cheatbreaker.client.module.type.nametag.NametagLayout.inventoryScale(3)*33<=.701F);
        assertEquals(.35F,com.cheatbreaker.client.module.type.nametag.NametagLayout.inventoryHeadGap(1),0F);
        assertEquals(.20F,com.cheatbreaker.client.module.type.nametag.NametagLayout.inventoryHeadGap(3),0F);
    }
    @Test public void chatClicksAreAdaptedAlongsideChatDrawing() throws Exception {
        ClassNode chat=transformed("net.minecraft.client.gui.GuiNewChat");int hits=0;
        for(MethodNode method:chat.methods)for(AbstractInsnNode insn:method.instructions.toArray())
            if(insn instanceof MethodInsnNode&&((MethodInsnNode)insn).name.equals("chatComponent"))hits++;
        assertEquals(1,hits);
    }
    @Test public void unicodeCommitsAndCandidatePositioningAreConnectedToProductionMethods() throws Exception {
        int screen=0,loop=0,caret=0,sprint=0;
        for(MethodNode method:transformed("net.minecraft.client.gui.GuiScreen").methods)
            for(AbstractInsnNode insn:method.instructions.toArray()) if(insn instanceof MethodInsnNode
                &&((MethodInsnNode)insn).name.equals("shouldDispatch"))screen++;
        for(MethodNode method:transformed("net.minecraft.client.Minecraft").methods)
            for(AbstractInsnNode insn:method.instructions.toArray()) if(insn instanceof MethodInsnNode
                &&((MethodInsnNode)insn).name.equals("shouldDispatchInGui"))loop++;
        for(MethodNode method:transformed("net.minecraft.client.gui.GuiTextField").methods)
            for(AbstractInsnNode insn:method.instructions.toArray()) if(insn instanceof MethodInsnNode
                &&((MethodInsnNode)insn).name.equals("textField"))caret++;
        for(MethodNode method:transformed("net.minecraft.client.entity.EntityPlayerSP").methods)
            for(AbstractInsnNode insn:method.instructions.toArray()) if(insn instanceof MethodInsnNode
                &&((MethodInsnNode)insn).owner.equals("cn/kazer/client189/animation/LegacyVisuals")
                &&((MethodInsnNode)insn).name.equals("setSprinting"))sprint++;
        assertEquals(1,screen);assertEquals(1,loop);assertTrue(caret>0);
        assertEquals("Item use/sprint transitions must remain vanilla",0,sprint);
    }
}
