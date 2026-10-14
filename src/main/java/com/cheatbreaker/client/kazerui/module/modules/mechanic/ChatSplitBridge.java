package com.cheatbreaker.client.kazerui.module.modules.mechanic;
import java.util.List;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.IChatComponent;
final class ChatSplitBridge {
    static List<IChatComponent> split(IChatComponent component,int width,FontRenderer font,boolean a,boolean b) {
        return net.minecraft.client.gui.GuiUtilRenderComponents.splitText(component,width,font,a,b);
    }
}
