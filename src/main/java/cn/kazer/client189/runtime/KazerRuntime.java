package cn.kazer.client189.runtime;

import com.cheatbreaker.client.CheatBreaker;
import com.cheatbreaker.client.event.type.*;
import com.cheatbreaker.client.ui.mainmenu.MainMenu;
import com.cheatbreaker.client.ui.module.CBModulesGui;
import com.cheatbreaker.client.ui.module.CBModulePlaceGui;
import cn.kazer.client189.compat.KazerCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.*;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

/** Adapts Forge lifecycle to the unchanged 1.7 Kazer module and configuration model. */
public final class KazerRuntime {
    private final Minecraft mc = Minecraft.getMinecraft();
    private WorldClient world;
    private final ParitySmoke smoke = new ParitySmoke();
    private boolean ownsGuiBlur;
    private boolean cosmeticsInstalled;
    private boolean handFov;
    private CheatBreaker client() { return CheatBreaker.getInstance(); }
    @SubscribeEvent public void attack(net.minecraftforge.event.entity.player.AttackEntityEvent event) {
        if(event.entityPlayer==mc.thePlayer)com.cheatbreaker.client.kazerui.module.modules.hud.CombatHud.attack(event.target);
    }
    @SubscribeEvent public void models(ModelBakeEvent event) {
        cn.kazer.client189.runtime.RedStringModels.install(event);
    }

    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if(!cosmeticsInstalled) {
            cosmeticsInstalled=true;
            com.cheatbreaker.client.util.MinecraftWindowIcon.apply();
            KazerCosmeticLayer.install();
        }
        KazerNametags.tick();
        cn.kazer.client189.input.WindowsFullscreenImeSupport.onClientTick();
        cn.kazer.client189.animation.OldAnimationsAdapter.tick();
        if (world != mc.theWorld) {
            if (world != null && mc.theWorld == null) client().netHandler.onDisconnect();
            world = mc.theWorld;
            client().eventBus.callEvent(new LoadWorldEvent(world));
        }
        if (mc.thePlayer != null && !(mc.thePlayer.movementInput instanceof KazerMovementInput))
            mc.thePlayer.movementInput = new KazerMovementInput(mc.gameSettings);
        client().eventBus.callEvent(new com.cheatbreaker.client.event.type.TickEvent());
        if (mc.currentScreen == null) KazerCompat.vignette().returnedToWorld();
        boolean blur = mc.currentScreen != null && mc.theWorld != null && (Boolean)client().globalSettings.guiBlur.getValue();
        if (blur && !ownsGuiBlur && net.minecraft.client.renderer.OpenGlHelper.shadersSupported) {
            KazerCompat.loadGuiBlur(); ownsGuiBlur = true;
        } else if (!blur && ownsGuiBlur) {mc.entityRenderer.stopUseShader(); ownsGuiBlur = false;}
    }

    @SubscribeEvent public void key(InputEvent.KeyInputEvent event) {
        if (Keyboard.getEventKeyState() && mc.currentScreen == null)
            client().eventBus.callEvent(new KeyboardEvent(Keyboard.getEventKey()));
    }
    @SubscribeEvent public void mouse(MouseEvent event) {
        if(com.cheatbreaker.client.kazerui.module.modules.mechanic.ModuleZoom.scroll(event.dwheel))event.setCanceled(true);
        if (event.button >= 0 && event.buttonstate && mc.currentScreen == null)
            client().eventBus.callEvent(new ClickEvent(event.button));
    }
    @SubscribeEvent public void renderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {KazerCompat.partialTicks = event.renderTickTime;KazerNametags.beginFrame();}
        else {handFov=false;smoke.frame();}
    }
    @SubscribeEvent public void playerFov(FOVUpdateEvent event){event.newfov=com.cheatbreaker.client.kazerui.module.modules.mechanic.ModuleFov.playerModifier(event.newfov);}
    @SubscribeEvent public void handFov(RenderHandEvent event){handFov=true;}
    @SubscribeEvent public void cameraFov(EntityViewRenderEvent.FOVModifier event){
        float fov=event.getFOV();if(handFov)handFov=false;else fov=com.cheatbreaker.client.kazerui.module.modules.mechanic.ModuleFov.fov(fov);
        event.setFOV(com.cheatbreaker.client.kazerui.module.modules.mechanic.ModuleZoom.fov(fov,(float)event.renderPartialTicks));
    }
    @SubscribeEvent public void open(GuiOpenEvent event) {
        StartupSplashHooks.finish();
        if(event.gui!=null && event.gui.getClass()==net.minecraft.client.gui.GuiMultiplayer.class) {
            net.minecraft.client.gui.GuiScreen parent=net.minecraftforge.fml.relauncher.ReflectionHelper.getPrivateValue(
                net.minecraft.client.gui.GuiMultiplayer.class,(net.minecraft.client.gui.GuiMultiplayer)event.gui,"parentScreen","field_146798_g");
            event.gui=new cn.kazer.client189.ui.KazerMultiplayer(parent);
        }
        if(event.gui instanceof net.minecraft.client.gui.GuiChat)
            com.cheatbreaker.client.kazerui.gui.ChatSubmitGate.opened(event.gui);
        if (event.gui instanceof net.minecraft.client.gui.GuiIngameMenu
                && !(event.gui instanceof cn.kazer.client189.ui.KazerPauseMenu))
            event.gui = new cn.kazer.client189.ui.KazerPauseMenu();
        if ((event.gui instanceof GuiMainMenu || event.gui instanceof MainMenu) && client().ownedUi != null)
            event.gui = client().ownedUi.home();
        else if (event.gui instanceof CBModulesGui && client().ownedUi != null)
            event.gui = client().ownedUi.menu(mc.theWorld==null ? client().ownedUi.home() : null);
        else if (event.gui instanceof CBModulePlaceGui && client().ownedUi != null)
            event.gui = client().ownedUi.hudEditor(null);
        if (event.gui instanceof net.minecraft.client.gui.GuiScreenResourcePacks
                && !(event.gui instanceof cn.kazer.client189.ui.KazerResourcePacks)) {
            net.minecraft.client.gui.GuiScreen parent = net.minecraftforge.fml.relauncher.ReflectionHelper.getPrivateValue(
                    net.minecraft.client.gui.GuiScreenResourcePacks.class,
                    (net.minecraft.client.gui.GuiScreenResourcePacks)event.gui,"parentScreen","field_146965_f");
            event.gui = new cn.kazer.client189.ui.KazerResourcePacks(parent);
        }
    }
    @SubscribeEvent public void overlay(RenderGameOverlayEvent.Post event) {
        if(event.type==RenderGameOverlayEvent.ElementType.FOOD)foodDrawn=true;
        if(event.type==RenderGameOverlayEvent.ElementType.ALL) {
            com.cheatbreaker.client.kazerui.module.modules.hud.ModuleSaturation module=(com.cheatbreaker.client.kazerui.module.modules.hud.ModuleSaturation)com.cheatbreaker.client.kazerui.module.ModuleManager.get("saturation");
            if(module!=null)module.render(event.resolution,foodDrawn);foodDrawn=false;
        }
        if (event.type != RenderGameOverlayEvent.ElementType.ALL || mc.thePlayer == null) return;
        if (mc.gameSettings.showDebugInfo && !(Boolean)client().globalSettings.showHudInDebug.getValue()) return;
        client().eventBus.callEvent(mc.currentScreen instanceof CBModulesGui || mc.currentScreen instanceof CBModulePlaceGui
                ? new RenderPreviewEvent(event.resolution) : new GuiDrawEvent(event.resolution));
    }
    private boolean foodDrawn;
    @SubscribeEvent public void tooltip(net.minecraftforge.event.entity.player.ItemTooltipEvent event) {
        com.cheatbreaker.client.kazerui.module.render.TooltipHooks.food(event.itemStack,event.toolTip);
    }
    @SubscribeEvent public void overlayPre(RenderGameOverlayEvent.Pre event) {
        if (event.type == RenderGameOverlayEvent.ElementType.BOSSHEALTH && client().ownedUi.bossPre())
            event.setCanceled(true);
        if (event.type==RenderGameOverlayEvent.ElementType.ARMOR && client().ownedUi.renderArmor(event.resolution,net.minecraftforge.client.GuiIngameForge.left_height)) {
            net.minecraftforge.client.GuiIngameForge.left_height=com.cheatbreaker.client.kazerui.event.UiOverlayMetrics.left_height;
            event.setCanceled(true);
        }
        if (event.type == RenderGameOverlayEvent.ElementType.CROSSHAIRS) {
            if (mc.gameSettings.thirdPersonView != 0 && !(Boolean)client().globalSettings.showCrosshairThirdPerson.getValue()) {
                event.setCanceled(true);
            } else if ((Boolean)client().globalSettings.customCrosshair.getValue()) {
                event.setCanceled(true); RuntimeHooks.customCrosshair(event.resolution);
            }
        }
    }
    @SubscribeEvent public void namePre(RenderLivingEvent.Specials.Pre event) {
        if(event.entity instanceof com.cheatbreaker.client.kazerui.cosmetics.render.Mannequin){event.setCanceled(true);return;}
        if (event.entity instanceof EntityPlayer && client().moduleManager.nametagModule.isEnabled()) event.setCanceled(true);
    }
    @SubscribeEvent public void namePost(RenderPlayerEvent.Post event) {
        KazerCosmeticLayer.restore(event.renderer);
        KazerNametags.collect(event);
    }
    @SubscribeEvent(priority=net.minecraftforge.fml.common.eventhandler.EventPriority.LOWEST)
    public void cosmeticParts(RenderLivingEvent.Pre event){KazerCosmeticLayer.hide(event);}
    @SubscribeEvent public void menuFooter(GuiScreenEvent.DrawScreenEvent.Post event) {
        com.cheatbreaker.client.kazerui.module.modules.mechanic.ChatHooks.screen(event.gui,event.mouseX,event.mouseY);
        cn.kazer.client189.input.ImeHooks.screen(event.gui);
        com.cheatbreaker.client.ui.util.KazerMenuBranding.draw(event.gui.width,event.gui.height);
        com.cheatbreaker.client.kazerui.gui.KazerNotifications.render();
    }
    @SubscribeEvent public void chatMouse(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (com.cheatbreaker.client.kazerui.module.modules.visual.ModuleScrollableTooltips.wheel(event.gui,
                org.lwjgl.input.Mouse.getEventX()*event.gui.width/mc.displayWidth,
                event.gui.height-org.lwjgl.input.Mouse.getEventY()*event.gui.height/mc.displayHeight-1,org.lwjgl.input.Mouse.getEventDWheel())) {
            event.setCanceled(true);return;
        }
        if (com.cheatbreaker.client.kazerui.module.modules.mechanic.ChatHooks.mouse(event.gui)) event.setCanceled(true);
    }
    @SubscribeEvent public void chatKey(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if(event.gui instanceof net.minecraft.client.gui.GuiChat && Keyboard.getEventKeyState()
                && (Keyboard.getEventKey()==Keyboard.KEY_RETURN || Keyboard.getEventKey()==Keyboard.KEY_NUMPADENTER)
                && !com.cheatbreaker.client.kazerui.gui.ChatSubmitGate.submit(event.gui)) {
            event.setCanceled(true);return;
        }
        com.cheatbreaker.client.kazerui.module.modules.mechanic.ChatHooks.key(event.gui);
    }
    @SubscribeEvent public void worldRender(RenderWorldLastEvent event) {
        com.cheatbreaker.client.kazerui.module.modules.visual.ModuleLightOverlay.render(event.partialTicks);
        client().eventBus.callEvent(new com.cheatbreaker.client.event.type.RenderWorldEvent(event.partialTicks));
        KazerNametags.finishWorld();
    }

    private final class KazerMovementInput extends net.minecraft.MinecraftMovementInputHelper {
        KazerMovementInput(net.minecraft.client.settings.GameSettings settings) { super(settings); }
        @Override public void updatePlayerMoveState() {
            if (mc.thePlayer == null) { super.updatePlayerMoveState(); return; }
            if(client().ownedUi!=null) {
                com.cheatbreaker.client.kazerui.module.modules.mechanic.ModuleToggleSneak.updateMovement(mc.thePlayer,this);
                return;
            }
            lIIIIlIIllIIlIIlIIIlIIllI(mc, this, mc.thePlayer);
            if (client().moduleManager.toggleSprint.isEnabled() && isSprinting && moveForward >= .8F
                    && !sneak && !mc.thePlayer.isUsingItem()
                    && !mc.thePlayer.isCollidedHorizontally
                    && (mc.thePlayer.getFoodStats().getFoodLevel() > 6 || mc.thePlayer.capabilities.allowFlying))
                mc.thePlayer.setSprinting(true);
        }
    }
}
