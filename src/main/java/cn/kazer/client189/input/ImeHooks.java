package cn.kazer.client189.input;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.input.Keyboard;
import java.util.List;

/** Version adapters only; commit/candidate/fullscreen behavior is shared with 1.7.10. */
public final class ImeHooks {
    private ImeHooks() {}
    public static boolean shouldDispatch() {
        return ImeKeyboardEvent.shouldDispatch(Keyboard.getEventKeyState(), Keyboard.getEventKey(), Keyboard.getEventCharacter());
    }
    public static boolean shouldDispatchInGui() {
        return Minecraft.getMinecraft().currentScreen==null?Keyboard.getEventKeyState():shouldDispatch();
    }
    public static void textField(Object object) {
        GuiTextField field=(GuiTextField)object;
        if (!field.isFocused() || !field.getVisible()) return;
        Minecraft mc=Minecraft.getMinecraft();
        int scroll=ReflectionHelper.getPrivateValue(GuiTextField.class,field,"lineScrollOffset","field_146225_q");
        FontRenderer font=ReflectionHelper.getPrivateValue(GuiTextField.class,field,"fontRendererInstance","field_146211_a");
        String visible=font.trimStringToWidth(field.getText().substring(scroll),field.getWidth());
        int cursor=field.getCursorPosition()-scroll;
        if (cursor<0 || cursor>visible.length()) return;
        int x=field.xPosition+(field.getEnableBackgroundDrawing()?4:0)+font.getStringWidth(visible.substring(0,cursor));
        if (field.getCursorPosition()<field.getText().length() || field.getText().length()>=field.getMaxStringLength()) x--;
        int y=field.yPosition+(field.getEnableBackgroundDrawing()?(field.height-8)/2:0);
        WindowsImeSupport.updateCaretPosition(x,y-1,font.FONT_HEIGHT+2);
    }
    public static void screen(GuiScreen screen) {
        Minecraft mc=Minecraft.getMinecraft(); FontRenderer font=mc.fontRendererObj;
        ImeCaretAnchor caret=null;
        if (screen instanceof GuiEditSign) {
            GuiEditSign sign=(GuiEditSign)screen;
            TileEntitySign tile=ReflectionHelper.getPrivateValue(GuiEditSign.class,sign,"tileSign","field_146848_f");
            int line=ReflectionHelper.getPrivateValue(GuiEditSign.class,sign,"editLine","field_146851_h");
            caret=ImeCaretAnchor.forSign(screen.width,screen.height,line,font.getStringWidth(tile.signText[line].getUnformattedText()),font.FONT_HEIGHT);
        } else if (screen instanceof GuiScreenBook) {
            GuiScreenBook book=(GuiScreenBook)screen;
            boolean editable=ReflectionHelper.getPrivateValue(GuiScreenBook.class,book,"bookIsUnsigned","field_146475_i");
            if (!editable) return;
            boolean signing=ReflectionHelper.getPrivateValue(GuiScreenBook.class,book,"bookGettingSigned","field_146480_s");
            int left=(screen.width-192)/2;
            if (signing) {
                String title=ReflectionHelper.getPrivateValue(GuiScreenBook.class,book,"bookTitle","field_146482_z");
                caret=ImeCaretAnchor.forCenteredTextEnd(left+94,50,font.getStringWidth(title),font.FONT_HEIGHT);
            } else {
                NBTTagList pages=ReflectionHelper.getPrivateValue(GuiScreenBook.class,book,"bookPages","field_146483_y");
                int page=ReflectionHelper.getPrivateValue(GuiScreenBook.class,book,"currPage","field_146484_x");
                String text=pages!=null&&page>=0&&page<pages.tagCount()?pages.getStringTagAt(page):"";
                List<String> wrapped=font.listFormattedStringToWidth(text+"\u00a70_",116);
                int last=Math.max(0,font.getStringWidth(wrapped.get(wrapped.size()-1))-font.getStringWidth("_"));
                caret=ImeCaretAnchor.forWrappedTextEnd(left+36,34,last,wrapped.size(),font.FONT_HEIGHT);
            }
        }
        if(caret!=null)WindowsImeSupport.updateCaretPosition(caret.getX(),caret.getTop(),caret.getHeight());
    }
}
