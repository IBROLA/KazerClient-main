package com.cheatbreaker.client.kazerui.module.modules.mechanic;

import com.cheatbreaker.client.kazerui.module.Module;
import com.cheatbreaker.client.kazerui.module.Page;
import com.cheatbreaker.client.kazerui.module.setting.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.MovementInputFromOptions;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** Donor toggle/hold semantics, called before native movement rather than by
 * reflective field writes. Vanilla retains hunger, item-use and collision rules. */
public final class ModuleToggleSneak extends Module {
    private static ModuleToggleSneak instance;
    final BoolSetting toggleSprint=bool("toggleSprint",true);
    private final BoolSetting alwaysSprint=bool("alwaysSprint",false);
    private final BoolSetting sprintKeybindOverride=bool("sprintKeybindOverride",false);
    private final KeySetting keybindSprint=keybind("keybindSprint");
    private final BoolSetting toggleSneak=bool("toggleSneak",false);
    private final BoolSetting sneakKeybindOverride=bool("sneakKeybindOverride",false);
    private final KeySetting keybindSneak=keybind("keybindSneak");
    private final BoolSetting toggleSneakContainer=bool("toggleSneakContainer",false);
    final BoolSetting flyBoost=bool("flyBoost",true);
    final NumberSetting flyBoostAmount=integer("flyBoostAmount",4,2,8);
    private final BoolSetting restrictFlyBoost=bool("restrictFlyBoost",true).noWidget();
    private final BoolSetting doubleTap=bool("doubleTap",true);
    boolean sprinting,sneaking,sprintHeld;
    private boolean wasSprint,wasSneak,active;
    private long sprintDown,sneakDown;
    private boolean sprintGesture,sneakGesture;
    private EntityPlayerSP player;
    private Float flySpeed;
    public ModuleToggleSneak(){super("TOGGLE_SNEAK",true);instance=this;child(new ModuleToggleSneakHud(this),"generalOptions");}
    @Override protected void layout(Page page) {
        page.section("generalOptions",s->{
            s.group(toggleSprint,g->{g.add(alwaysSprint);g.group(sprintKeybindOverride,k->k.add(keybindSprint));});
            s.group(toggleSneak,g->{g.group(sneakKeybindOverride,k->k.add(keybindSneak));g.add(toggleSneakContainer);});s.add(doubleTap);
        });
        page.section("flyBoostOptions",s->s.add(flyBoost,flyBoostAmount));page.add(restrictFlyBoost);
    }
    public static boolean doubleTapEnabled(){return instance==null || !instance.isEnabled() || instance.doubleTap.on();}
    private static GameSettings gameSettings(){return mc().gameSettings;}
    private boolean containerSneak(){
        boolean hypixel=mc().getCurrentServerData()!=null && mc().getCurrentServerData().serverIP.toLowerCase(java.util.Locale.ROOT).contains("hypixel.net");
        return toggleSneakContainer.on() && !hypixel;
    }
    private static void pressed(KeyBinding key,boolean state){((KazerKeyState)(Object)key).kazerSetPressed(state);}
    static boolean physicallyDown(KeyBinding key){return physicallyDown(key.getKeyCode());}
    private static boolean physicallyDown(int key){
        return key<0 ? key>=-100 && key+100<Mouse.getButtonCount() && Mouse.isButtonDown(key+100)
            : key>0 && key<Keyboard.KEYBOARD_SIZE && Keyboard.isKeyDown(key);
    }
    private void restoreFly(){if(flySpeed!=null && player!=null){player.capabilities.setFlySpeed(flySpeed);flySpeed=null;}}
    private void release(){
        restoreFly();sprinting=sneaking=sprintHeld=wasSprint=wasSneak=false;active=false;
        if(Minecraft.getMinecraft().gameSettings!=null){pressed(gameSettings().keyBindSprint,physicallyDown(gameSettings().keyBindSprint));pressed(gameSettings().keyBindSneak,physicallyDown(gameSettings().keyBindSneak));}
    }
    @Override protected void onDisable(){release();}
    @Override public void tick(com.cheatbreaker.client.kazerui.event.UiTick.ClientTickEvent event){
        if(event.phase==com.cheatbreaker.client.kazerui.event.UiTick.Phase.END && active && (!isEnabled() || mc().thePlayer!=player))release();
    }
    private void before(EntityPlayerSP current) {
        if(player!=current){release();player=current;}
        if(!isEnabled()){if(active)release();return;}
        if(!active){sprinting=toggleSprint.on();active=true;}
        boolean sprint=physicallyDown(sprintKeybindOverride.on()?keybindSprint.code():gameSettings().keyBindSprint.getKeyCode());
        boolean sneak=physicallyDown(sneakKeybindOverride.on()?keybindSneak.code():gameSettings().keyBindSneak.getKeyCode());
        boolean screen=mc().currentScreen!=null;
        long now=System.currentTimeMillis();
        if(screen){sprintHeld=false;sprintDown=sneakDown=0;sprintGesture=sneakGesture=false;}
        else {
            if(sprint && !wasSprint){sprintDown=now;sprintHeld=true;sprintGesture=true;}
            if(sneak && !wasSneak){sneakDown=now;sneakGesture=true;}
            if(!sprint && wasSprint){sprintHeld=false;if(!current.capabilities.isFlying && toggleSprint.on() && shortPress(sprintGesture,sprintDown,now,400) && !alwaysSprint.on())sprinting=!sprinting;sprintGesture=false;}
            if(!sneak && wasSneak){if(!current.capabilities.isFlying && toggleSneak.on() && shortPress(sneakGesture,sneakDown,now,200))sneaking=!sneaking;sneakGesture=false;}
        }
        wasSprint=sprint;wasSneak=sneak;
        if(toggleSprint.on())pressed(gameSettings().keyBindSprint,!screen && (sprinting || alwaysSprint.on() || sprint));
        else pressed(gameSettings().keyBindSprint,!screen && physicallyDown(gameSettings().keyBindSprint));
        if(toggleSneak.on())pressed(gameSettings().keyBindSneak,(!screen || mc().currentScreen instanceof GuiChat || containerSneak()) && (sneaking || sneak));
        else pressed(gameSettings().keyBindSneak,!screen && physicallyDown(gameSettings().keyBindSneak));
    }
    public static boolean shortPress(boolean started,long down,long now,long maximum){return started && now>=down && now-down<=maximum;}
    private void after(EntityPlayerSP current,MovementInputFromOptions movement){
        if(!isEnabled())return;
        boolean allowed=!restrictFlyBoost.on() || current.capabilities.isCreativeMode;
        boolean boost=flyBoost.on() && allowed && current.capabilities.isFlying && physicallyDown(gameSettings().keyBindSprint);
        if(boost){
            if(flySpeed==null)flySpeed=current.capabilities.getFlySpeed();int amount=flyBoostAmount.intValue();
            current.capabilities.setFlySpeed(.05f*amount);if(movement.sneak)current.motionY-=.15*amount;if(movement.jump)current.motionY+=.15*amount;
        } else restoreFly();
    }
    /** Both hosts feed the same input before their original player rules. */
    public static void updateMovement(EntityPlayerSP player,MovementInputFromOptions movement){
        if(instance==null){movement.updatePlayerMoveState();return;}
        instance.before(player);
        GameSettings gs=gameSettings();movement.moveForward=movement.moveStrafe=0;
        if(gs.keyBindForward.isKeyDown())movement.moveForward++;
        if(gs.keyBindBack.isKeyDown())movement.moveForward--;
        if(gs.keyBindLeft.isKeyDown())movement.moveStrafe++;
        if(gs.keyBindRight.isKeyDown())movement.moveStrafe--;
        movement.jump=gs.keyBindJump.isKeyDown();movement.sneak=gs.keyBindSneak.isKeyDown();
        if(movement.sneak){movement.moveForward*=.3f;movement.moveStrafe*=.3f;}
        instance.after(player,movement);
    }
}
