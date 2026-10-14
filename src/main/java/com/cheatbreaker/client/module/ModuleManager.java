package com.cheatbreaker.client.module;

import com.cheatbreaker.client.module.staff.XRayModule;
import com.cheatbreaker.client.module.type.blockhitsound.BlockhitSoundModule;
import com.cheatbreaker.client.module.type.bossbar.BossBarModule;
import com.cheatbreaker.client.module.type.freelook.FreelookModule;
import com.cheatbreaker.client.module.type.togglesprint.ToggleSprintModule;
import com.cheatbreaker.client.module.type.*;
import com.cheatbreaker.client.module.type.armourstatus.ArmourStatusModule;
import com.cheatbreaker.client.module.type.cooldowns.CooldownsModule;
import com.cheatbreaker.client.module.type.keystrokes.KeystrokesModule;
import com.cheatbreaker.client.module.type.motionblur.MotionBlurModule;
import com.cheatbreaker.client.module.type.nametag.NametagModule;
import com.cheatbreaker.client.module.type.notifications.CBNotificationsModule;

import java.util.ArrayList;
import java.util.List;

public class ModuleManager {

    public List<AbstractModule> modules;
    public List<AbstractModule> staffModules;
    public AbstractModule llIIlllIIIIlllIllIlIlllIl;

    public ToggleSprintModule toggleSprint;
    public CBNotificationsModule notifications;
    public ArmourStatusModule armourStatus;
    public CooldownsModule cooldowns;
    public ScoreboardModule scoreboard;
    public XRayModule xray;
    public PotionStatusModule potionStatus;
    public BossBarModule bossBar;
    public DirectionHudModule directionHud;
    public KeystrokesModule keyStrokes;
    public FPSModule fpsModule;
    public CPSModule cpsModule;
    public SaturationModule saturationModule;
    public ClockModule clockModule;
    public PackDisplayModule packDisplayModule;
    public ItemCountModule itemCountModule;
    public com.cheatbreaker.client.module.type.customtext.CustomTextModule customTextModule;
    public com.cheatbreaker.client.module.type.autotexthotkey.AutoTextHotkeyModule autoTextHotkeyModule;
    public CoordinatesModule coordinatesModule;
    public FreelookModule freelook;
    public ChatModule chatModule;
    public BlockhitSoundModule blockhitSound;
    public MotionBlurModule motionBlurModule;
    public NametagModule nametagModule;
    public cn.kazer.client189.animation.BlockHitModule blockHitAnimation;
    public ScreenshotModule screenshotModule;

    public ModuleManager() {
        modules = new ArrayList<>();
        staffModules = new ArrayList<>();

        // Authoritative Kazer list: the original ten NEW UI modules plus Chat.
        // references and the only modules persisted in profiles.
        modules.add(armourStatus = new ArmourStatusModule());
        modules.add(toggleSprint = new ToggleSprintModule());
        modules.add(potionStatus = new PotionStatusModule());
        modules.add(keyStrokes = new KeystrokesModule());
        modules.add(fpsModule = new FPSModule());
        modules.add(cpsModule = new CPSModule());
        modules.add(nametagModule = new NametagModule());
        modules.add(bossBar = new BossBarModule());
        modules.add(motionBlurModule = new MotionBlurModule());
        modules.add(scoreboard = new ScoreboardModule());
        modules.add(chatModule = new ChatModule());
        modules.add(blockHitAnimation = new cn.kazer.client189.animation.BlockHitModule());

        // Old rendering paths still dereference these fields.  Keep inert compatibility
        // instances, but do not register them in the menu/event/profile module list.
        modules.add(freelook = new FreelookModule());
        packDisplayModule = disabled(new PackDisplayModule());
        saturationModule = disabled(new SaturationModule());
        clockModule = disabled(new ClockModule());
        autoTextHotkeyModule = disabled(new com.cheatbreaker.client.module.type.autotexthotkey.AutoTextHotkeyModule());
        itemCountModule = disabled(new ItemCountModule());
        coordinatesModule = disabled(new CoordinatesModule());
        directionHud = disabled(new DirectionHudModule());
        customTextModule = disabled(new com.cheatbreaker.client.module.type.customtext.CustomTextModule());
        notifications = disabled(new CBNotificationsModule());
        cooldowns = disabled(new CooldownsModule());
        screenshotModule = disabled(new ScreenshotModule());
        blockhitSound = disabled(new BlockhitSoundModule());

        xray = new XRayModule();
        xray.setStaffModuleEnabled(false);
    }

    private static <T extends AbstractModule> T disabled(T module) {
        module.setDefaultState(false);
        module.setDefaultRenderHud(false);
        return module;
    }

}
