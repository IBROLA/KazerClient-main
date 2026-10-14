package cn.kazer.client189.ui;

import net.minecraft.client.gui.*;
import net.minecraft.client.gui.achievement.GuiAchievements;
import net.minecraft.client.gui.achievement.GuiStats;
import java.io.IOException;

/** Same button geometry and actions as the 1.7 Kazer pause screen. */
public final class KazerPauseMenu extends GuiIngameMenu {
    @Override public void initGui() {
        buttonList.clear();
        int x=width/2-100, y=height/4+8;
        boolean single=mc.isSingleplayer();
        buttonList.add(new GuiButton(4,x,y,200,20,"Back to Game"));
        buttonList.add(new GuiButton(5,x,y+24,98,20,"Achievements"));
        buttonList.add(new GuiButton(6,x+102,y+24,98,20,"Statistics"));
        GuiButton world=new GuiButton(7,x,y+48,200,20,single?"Host World":"Multiplayer");
        world.enabled=!single||!mc.getIntegratedServer().getPublic();
        buttonList.add(world);
        buttonList.add(new GuiButton(0,x,y+72,98,20,"Options..."));
        buttonList.add(new GuiButton(10,x+102,y+72,98,20,"Kazer Options..."));
        buttonList.add(new GuiButton(1,x,y+96,200,20,single?"Save and Quit to Title":"Disconnect"));
    }
    @Override protected void actionPerformed(GuiButton button) throws IOException {
        switch(button.id) {
            case 4: mc.displayGuiScreen(null);mc.setIngameFocus();break;
            case 5: mc.displayGuiScreen(new GuiAchievements(this,mc.thePlayer.getStatFileWriter()));break;
            case 6: mc.displayGuiScreen(new GuiStats(this,mc.thePlayer.getStatFileWriter()));break;
            case 7: mc.displayGuiScreen(mc.isSingleplayer()?new GuiShareToLan(this):new GuiMultiplayer(this));break;
            case 0: mc.displayGuiScreen(new GuiOptions(this,mc.gameSettings));break;
            case 10: mc.displayGuiScreen(new com.cheatbreaker.client.ui.module.CBModulesGui());break;
            case 1:
                button.enabled=false;mc.theWorld.sendQuittingDisconnectingPacket();mc.loadWorld(null);
                mc.displayGuiScreen(new GuiMainMenu());break;
            default: break;
        }
    }
}
