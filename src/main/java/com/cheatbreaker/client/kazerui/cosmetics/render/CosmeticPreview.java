package com.cheatbreaker.client.kazerui.cosmetics.render;

import com.cheatbreaker.client.kazerui.cosmetics.Cosmetic;
import java.util.Collections;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import org.lwjgl.opengl.GL11;

/** Reusable walking wearer. Browsing never mutates the real player. */
public final class CosmeticPreview {
    private static Mannequin wearer;
    private static net.minecraft.world.World world;
    private static int selection=-1;
    private static String profile="";
    private static float yaw=20, zoom=1;
    private static final long START=System.nanoTime();
    public static void rotate(float delta) { yaw=(yaw+delta)%360; }
    public static void zoom(int direction) { zoom=Math.max(.55f,Math.min(1.65f,zoom+direction*.08f)); }
    public static void draw(Cosmetic cosmetic) {
        Minecraft mc=Minecraft.getMinecraft();
        String id=mc.getSession().getPlayerID();
        if(wearer==null || world!=mc.theWorld || !profile.equals(id)){wearer=Mannequin.self();world=mc.theWorld;profile=id;selection=-1;}
        int next=cosmetic==null?-1:cosmetic.id;
        if(selection!=next){selection=next;wearer.wear(cosmetic==null?Collections.<Cosmetic>emptyList():Collections.singletonList(cosmetic));}
        float phase=(System.nanoTime()-START)/1_000_000_000f*4;
        wearer.limbSwing=phase;wearer.prevLimbSwingAmount=wearer.limbSwingAmount=.65f;
        wearer.ticksExisted=(int)(phase*5);
        RenderManager manager=mc.getRenderManager();
        net.minecraft.world.World oldWorld=manager.worldObj;float oldView=manager.playerViewY;
        net.minecraft.entity.Entity oldViewer=manager.livingPlayer;
        net.minecraft.client.settings.GameSettings oldOptions=manager.options;
        cn.kazer.client189.compat.LegacyGL.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try {
            manager.worldObj=wearer.worldObj;
            manager.livingPlayer=wearer;manager.options=mc.gameSettings;
            drawWearer(manager);
        }finally{manager.worldObj=oldWorld;manager.playerViewY=oldView;manager.livingPlayer=oldViewer;manager.options=oldOptions;cn.kazer.client189.compat.LegacyGL.glPopAttrib();}
    }
    private static void drawWearer(RenderManager manager) {
        // Vanilla's mouse-yaw is limited by atan; passing 180 does not turn it around.
        GL11.glPushMatrix();
        try {
            cn.kazer.client189.compat.LegacyGL.glEnable(GL11.GL_COLOR_MATERIAL);
            cn.kazer.client189.compat.LegacyGL.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glTranslatef(421,250,50); GL11.glScalef(-62*zoom,62*zoom,62*zoom);
            GL11.glRotatef(180,0,0,1); GL11.glRotatef(135,0,1,0);
            net.minecraft.client.renderer.RenderHelper.enableStandardItemLighting();
            GL11.glRotatef(-135,0,1,0);
            wearer.renderYawOffset=wearer.prevRenderYawOffset=yaw;
            wearer.rotationYaw=wearer.prevRotationYaw=yaw;
            wearer.rotationYawHead=wearer.prevRotationYawHead=yaw;
            wearer.prevChasingPosX=wearer.chasingPosX=wearer.posX;
            wearer.prevChasingPosY=wearer.chasingPosY=wearer.posY;
            wearer.prevChasingPosZ=wearer.chasingPosZ=wearer.posZ;
            wearer.prevDistanceWalkedModified=wearer.distanceWalkedModified=0;
            wearer.prevCameraYaw=wearer.cameraYaw=0;
            manager.playerViewY=180;
            manager.renderEntityWithPosYaw(wearer,0,0,0,0,1);
        }finally{net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();GL11.glPopMatrix();}
    }
    private CosmeticPreview(){}
}
