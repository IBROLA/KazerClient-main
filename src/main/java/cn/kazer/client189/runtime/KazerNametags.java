package cn.kazer.client189.runtime;

import com.cheatbreaker.client.CheatBreaker;
import com.cheatbreaker.client.module.type.nametag.NametagLayout;
import com.cheatbreaker.client.module.type.nametag.NametagModule;
import cn.kazer.client189.compat.KazerCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderPlayerEvent;
import org.lwjgl.opengl.GL11;
import java.util.*;

public final class KazerNametags {
    private static final ResourceLocation BADGE = new ResourceLocation("client/kazer_badge.png");
    private static final List<RenderPlayerEvent.Post> PENDING=new ArrayList<>();
    private static final Map<EntityPlayer, LabelText> TEXT = new WeakHashMap<>();
    private static Object textWorld;
    private static int textTick;
    public static void tick(){textTick++;}
    private KazerNametags() { }
    public static void beginFrame(){
        PENDING.clear();
        Object world=Minecraft.getMinecraft().theWorld;
        if(textWorld!=world){TEXT.clear();textWorld=world;}
    }
    public static void collect(RenderPlayerEvent.Post event) {
        Minecraft mc=Minecraft.getMinecraft();
        CheatBreaker client=CheatBreaker.getInstance();
        if(!client.moduleManager.nametagModule.isEnabled() || event.entityPlayer.isInvisible())return;
        if(mc.currentScreen instanceof net.minecraft.client.gui.inventory.GuiContainer
                &&event.entityPlayer==mc.thePlayer&&event.x==0D&&event.y==0D&&event.z==0D)return;
        else if(PENDING.size()<512)PENDING.add(event);
    }
    public static void finishWorld() {
        if (PENDING.isEmpty()) return;
        // These labels are one last-world pass. Preserve the native driver/cache
        // once for the batch, not hundreds of reflected fields per player.
        cn.kazer.client189.compat.LegacyGL.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try {for(RenderPlayerEvent.Post event:PENDING)renderLabel(event);}
        finally {PENDING.clear();cn.kazer.client189.compat.LegacyGL.glPopAttrib();}
    }

    /** Legacy/Apollo wire lines are bottom-to-top. Only the actual name row owns a badge. */
    public static List<String> displayLines(List<String> supplied, String username, String fallback) {
        return NametagLayout.displayLines(supplied, username, fallback);
    }
    public static void render(RenderPlayerEvent.Post event) {
        cn.kazer.client189.compat.LegacyGL.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try { renderLabel(event); }
        finally { cn.kazer.client189.compat.LegacyGL.glPopAttrib(); }
    }
    private static void renderLabel(RenderPlayerEvent.Post event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen instanceof com.cheatbreaker.client.ui.module.CBModulesGui
                || mc.currentScreen instanceof com.cheatbreaker.client.ui.module.KazerModulesGui) return;
        CheatBreaker client = CheatBreaker.getInstance();
        NametagModule module = client.moduleManager.nametagModule;
        EntityPlayer player = event.entityPlayer;
        boolean inventory = mc.currentScreen instanceof net.minecraft.client.gui.inventory.GuiContainer
                && player == mc.thePlayer && event.x == 0D && event.y == 0D && event.z == 0D;
        if (inventory) return;
        if (!module.isEnabled() || player.isInvisible() || (!inventory && mc.thePlayer == null)) return;
        if (!inventory && mc.gameSettings.hideGUI && (Boolean)module.hideInFirstPerson.getValue()) return;
        if (player == mc.thePlayer && !inventory && (mc.gameSettings.thirdPersonView == 0
                || !(Boolean)module.thirdPersonNametag.getValue())) return;
        if (!inventory && player.getDistanceSqToEntity(mc.thePlayer) > (player.isSneaking()?1024:4096)) return;
        List<String> wire=client.netHandler.nametagOverride(player.getUniqueID());
        boolean healthVisible=player.getDistanceSqToEntity(mc.thePlayer)<100D;
        LabelText cached=TEXT.get(player);
        if(cached==null || cached.tick!=textTick || cached.wire!=wire
                || cached.font!=mc.fontRendererObj || cached.healthVisible!=healthVisible) {
            List<String> lines = displayLines(wire,player.getName(),player.getDisplayName().getFormattedText());
            if (healthVisible && !lines.isEmpty()) {
                net.minecraft.scoreboard.Scoreboard board = player.getWorldScoreboard();
                net.minecraft.scoreboard.ScoreObjective objective = board.getObjectiveInDisplaySlot(2);
                if (objective != null) {
                    String health = board.getValueFromObjective(player.getName(), objective).getScorePoints()
                            + " " + objective.getDisplayName();
                    if (!lines.contains(health)) lines.add(health);
                }
            }
            cached=new LabelText(textTick,wire,lines,mc.fontRendererObj,healthVisible,player.getName());
            TEXT.put(player,cached);
        }
        List<String> lines=cached.lines;
        if(lines.isEmpty())return;
        int primary=cached.primary;
        GlStateManager.pushMatrix();
        try {
            // Inventory models are magnified by the GUI. Keep the entire label stack
            // inside the preview instead of putting its first row on the frame edge.
            float labelScale = inventory ? NametagLayout.inventoryScale(lines.size()) : NametagLayout.WORLD_SCALE;
            double headGap = inventory ? NametagLayout.inventoryHeadGap(lines.size()) : .5D;
            GlStateManager.translate(event.x, event.y + player.height + headGap + (lines.size()-1) * NametagLayout.LINE_HEIGHT * labelScale, event.z);
            KazerCompat.beginTextNameplate(inventory);
            GlStateManager.scale(-labelScale, -labelScale, labelScale);
            if (player == mc.thePlayer && !inventory)
                GlStateManager.translate(0, -NametagLayout.selfLift(mc.getRenderManager().playerViewX), 0);
            GlStateManager.disableLighting();
            GlStateManager.disableFog();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.depthMask(false);
            if(player.isSneaking())GlStateManager.enableDepth();else GlStateManager.disableDepth();
            for (int i = 0; i < lines.size(); i++) {
                String text = lines.get(i);
                boolean badge = i == primary && cached.nameLines[i]
                        && (Boolean)module.showNametagIcon.getValue()
                        && (player == mc.thePlayer || client.getPresenceClient().isKazerClient(player.getUniqueID()));
                NametagLayout layout = badge?cached.badged[i]:cached.plain[i];
                int y = i * NametagLayout.LINE_HEIGHT;
                GlStateManager.disableTexture2D();
                cn.kazer.client189.compat.LegacyGui.drawRect(layout.left - 1, y - 1, layout.right + 1, y + 9,
                        ((int)(255F * NametagLayout.BACKGROUND_ALPHA * ((Number)module.opacity.getValue()).floatValue())) << 24);
                GlStateManager.enableTexture2D();
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                GlStateManager.color(1,1,1,1);
                if(player.isSneaking())GlStateManager.enableDepth();else GlStateManager.disableDepth();
                mc.fontRendererObj.drawString(text, layout.textX, y, 0x20FFFFFF, (Boolean)module.textShadow.getValue());
                if (badge) {
                    mc.getTextureManager().bindTexture(BADGE);
                    int color = client.getPresenceClient().badgeColor(player.getUniqueID());
                    GlStateManager.color((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F, .125F);
                    net.minecraft.client.gui.Gui.drawModalRectWithCustomSizedTexture(layout.badgeX, y - 1, 0, 0,
                            NametagLayout.BADGE_SIZE, NametagLayout.BADGE_SIZE, NametagLayout.BADGE_SIZE, NametagLayout.BADGE_SIZE);
                }
                // Vanilla's second pass is opaque only where the world depth allows it.
                GlStateManager.enableDepth();GlStateManager.depthMask(true);
                GlStateManager.color(1,1,1,1);
                mc.fontRendererObj.drawString(text,layout.textX,y,player.isSneaking()?0x20FFFFFF:0xFFFFFFFF,(Boolean)module.textShadow.getValue());
                if(badge) {
                    mc.getTextureManager().bindTexture(BADGE);
                    int color=client.getPresenceClient().badgeColor(player.getUniqueID());
                    GlStateManager.color((color>>16&255)/255F,(color>>8&255)/255F,(color&255)/255F,player.isSneaking()?.125F:1);
                    net.minecraft.client.gui.Gui.drawModalRectWithCustomSizedTexture(layout.badgeX,y-1,0,0,
                            NametagLayout.BADGE_SIZE,NametagLayout.BADGE_SIZE,NametagLayout.BADGE_SIZE,NametagLayout.BADGE_SIZE);
                }
                if(player.isSneaking())GlStateManager.enableDepth();else GlStateManager.disableDepth();GlStateManager.depthMask(false);
            }
        } finally {
            GlStateManager.popMatrix();
        }
    }
    private static final class LabelText {
        final int tick,primary;
        final List<String> wire,lines;
        final net.minecraft.client.gui.FontRenderer font;
        final boolean healthVisible;
        final int[] widths;
        final boolean[] nameLines;
        final NametagLayout[] plain,badged;
        LabelText(int tick,List<String> wire,List<String> lines,net.minecraft.client.gui.FontRenderer font,boolean health,String name) {
            this.tick=tick;this.wire=wire;this.lines=lines;this.font=font;healthVisible=health;
            primary=NametagLayout.primaryLine(lines,name);widths=new int[lines.size()];
            nameLines=new boolean[widths.length];plain=new NametagLayout[widths.length];badged=new NametagLayout[widths.length];
            for(int i=0;i<widths.length;i++) {
                widths[i]=font.getStringWidth(lines.get(i));nameLines[i]=NametagLayout.isPlayerNameLine(lines.get(i),name);
                plain[i]=new NametagLayout(widths[i],false);badged[i]=new NametagLayout(widths[i],true);
            }
        }
    }
}
