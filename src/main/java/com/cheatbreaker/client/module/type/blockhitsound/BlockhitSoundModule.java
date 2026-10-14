package com.cheatbreaker.client.module.type.blockhitsound;

import com.cheatbreaker.client.config.Setting;
import com.cheatbreaker.client.event.type.TickEvent;
import com.cheatbreaker.client.module.AbstractModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

/**
 * Plays the vanilla anvil land cue when the local player is hurt while blocking
 * and a nearby player just started swinging while facing the local player.
 *
 * Purely observational: reads entity state each client tick, never touches
 * damage, knockback, blocking, or any PvP verdict logic. The heuristic is
 * conservative - it may miss real blockhits rather than risk false cues.
 */
public final class BlockhitSoundModule extends AbstractModule
{
    private static final ResourceLocation ANVIL_LAND_SOUND = new ResourceLocation("random.anvil_land");
    /** Debug timbre: the arrow-hit confirmation sound, so it can never be confused with a server's own anvil cue. */
    private static final ResourceLocation DEBUG_HIT_SOUND = new ResourceLocation("random.successful_hit");

    private final BlockhitCueTracker tracker = new BlockhitCueTracker();
    private final Setting volume;
    private final Setting maxDistance;
    private final Setting debugSound;
    private int lastHurtTime = 0;

    public BlockhitSoundModule()
    {
        super("Blockhit Sound");
        this.setDefaultState(true);
        this.setDefaultRenderHud(false);
        this.isEditable = false;
        this.setPreviewLabel("Blockhit Sound", 1.0f);

        this.volume = new Setting(this, "Volume").setValue(100).setMinMax(0, 100);
        this.maxDistance = new Setting(this, "Max Distance")
            .setValue(5.0f)
            .setMinMax((float) BlockhitCueTracker.MIN_DISTANCE, (float) BlockhitCueTracker.MAX_DISTANCE);
        this.debugSound = new Setting(this, "Debug Sound").setValue(false);
        new Setting(this, "label").setValue("Heuristic detection, may be inaccurate");

        this.addEvent(TickEvent.class, this::onTick);
    }

    @Override
    public void setState(boolean state)
    {
        if (!state)
        {
            this.tracker.clear();
            this.lastHurtTime = 0;
        }

        super.setState(state);
    }

    private void onTick(TickEvent event)
    {
        Minecraft mc = this.minecraft;

        if (mc.thePlayer == null || mc.theWorld == null)
        {
            this.tracker.clear();
            this.lastHurtTime = 0;
            return;
        }

        long nowMs = System.currentTimeMillis();
        double localX = mc.thePlayer.posX;
        double localZ = mc.thePlayer.posZ;
        double maxDist = BlockhitCueTracker.quantizeMaxDistance((Float) this.maxDistance.getValue());

        for (EntityPlayer other : mc.theWorld.playerEntities)
        {
            if (other == mc.thePlayer)
            {
                continue;
            }

            // swingProgressInt <= 0 catches each swing start (-1, 0) exactly once per tick polling.
            if (other.isSwingInProgress && other.swingProgressInt <= 0)
            {
                this.tracker.recordSwing(other.posX, other.posZ, other.rotationYaw, localX, localZ, nowMs, maxDist);
            }
        }

        int hurtTime = mc.thePlayer.hurtTime;
        boolean hurtEdge = hurtTime > this.lastHurtTime;
        this.lastHurtTime = hurtTime;

        if (!hurtEdge)
        {
            return;
        }

        if (!this.tracker.onLocalHurt(mc.thePlayer.isBlocking(), localX, localZ, nowMs))
        {
            return;
        }

        float gain = BlockhitCueTracker.volumeGain((Integer) this.volume.getValue());
        if (gain > 0.0f)
        {
            ResourceLocation cue = (Boolean) this.debugSound.getValue() ? DEBUG_HIT_SOUND : ANVIL_LAND_SOUND;
            mc.getSoundHandler().playSound(PositionedSoundRecord.create(cue, gain));
        }
    }
}
