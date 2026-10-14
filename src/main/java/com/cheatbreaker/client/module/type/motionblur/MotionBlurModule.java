package com.cheatbreaker.client.module.type.motionblur;

import com.cheatbreaker.client.CheatBreaker;
import com.cheatbreaker.client.config.Setting;
import com.cheatbreaker.client.event.type.LoadWorldEvent;
import com.cheatbreaker.client.module.AbstractModule;

/**
 * Classic motion blur (phosphor accumulation trail) over the 3D world only;
 * HUD and GUI always stay crisp. Rendering lives in {@link MotionBlurPipeline},
 * which is deliberately independent of OptiFine's Fast Render: it never goes
 * through the vanilla {@code FrameBuffer}/{@code ShaderGroup} path that Fast
 * Render disables (see ADR-0004).
 */
public class MotionBlurModule extends AbstractModule {

    private final Setting mode;
    private final Setting amount;
    private final MotionBlurAccumulation accumulation = new MotionBlurAccumulation();
    private final MotionBlurPipeline pipeline = new MotionBlurPipeline();
    private final MonkeyBlurRenderer monkeyRenderer = new MonkeyBlurRenderer();
    private MotionBlurMode lastRenderedMode;

    public MotionBlurModule() {
        super("Motion Blur");
        this.setDefaultState(false);
        this.setDefaultRenderHud(false);
        this.isEditable = false;
        this.setPreviewLabel("Motion Blur", 5.0f * 0.22f);
        this.mode = new Setting(this, "Mode")
                .setValue(MotionBlurMode.DEFAULT.label())
                .acceptedValues(MotionBlurMode.labelsInOrder())
                .setDisplayLabel("Type");
        this.amount = new Setting(this, "Amount")
                .setValue(60)
                .setMinMax(MotionBlurStrength.MIN_PERCENT, MotionBlurStrength.MAX_PERCENT)
                .setDisplayLabel("Value").onChange(value -> this.accumulation.onModuleEnabled());
        this.addEvent(LoadWorldEvent.class, event -> this.accumulation.onWorldLoad());
    }

    @Override
    public void setState(boolean state) {
        if (state != this.isEnabled()) {
            this.accumulation.onModuleEnabled();
        }
        super.setState(state);
    }

    /** @return the module while it is actually blurring, or null (EntityRenderer hook). */
    public static MotionBlurModule getActive() {
        MotionBlurModule module = CheatBreaker.getInstance().getModuleManager().motionBlurModule;
        if (module == null || !module.isEnabled() || !MotionBlurStrength.isActive(module.getAmountPercent())) {
            return null;
        }
        return module;
    }

    public int getAmountPercent() {
        Object value = this.amount.getValue();
        return MotionBlurStrength.clampPercent(value instanceof Number ? ((Number) value).intValue() : MotionBlurStrength.DEFAULT_PERCENT);
    }

    public MotionBlurMode currentMode() {
        Object value = this.mode.getValue();
        return MotionBlurMode.fromLabel(value instanceof String ? (String) value : null);
    }

    /** Reads the mode and flags an accumulation reset on every mode switch. */
    private MotionBlurMode noteMode() {
        MotionBlurMode mode = this.currentMode();
        if (mode != this.lastRenderedMode) {
            if (this.lastRenderedMode != null) {
                this.accumulation.onModeChanged();
            }
            this.lastRenderedMode = mode;
        }
        return mode;
    }

    /** Runs between world render and GUI draw; blurs the world pass (plus hand for all but Monkey). */
    public void renderMotionBlur() {
        if (!isEnabled() || !MotionBlurStrength.isActive(getAmountPercent())) return;
        MotionBlurMode mode = this.noteMode();
        if (this.minecraft.gameSettings.anaglyph || mode == MotionBlurMode.MONKEY) {
            return; // Monkey renders inside renderWorld; anaglyph skips everything.
        }
        this.pipeline.render(this.minecraft, this.accumulation, mode, this.getAmountPercent());
    }

    private boolean monkeyActive() {
        return isEnabled() && MotionBlurStrength.isActive(getAmountPercent()) && this.noteMode() == MotionBlurMode.MONKEY && !this.minecraft.gameSettings.anaglyph;
    }

    /** renderWorld HEAD: redirect the world into the Monkey framebuffer. */
    public void onWorldRenderBegin(float partialTicks) {
        if (this.monkeyActive()) {
            this.monkeyRenderer.beginWorldRender(this.minecraft);
        }
    }

    /** Right after the camera transform is final. */
    public void onCameraReady(float partialTicks) {
        if (this.monkeyActive()) {
            this.monkeyRenderer.captureCamera(this.minecraft, partialTicks);
        }
    }

    /** Just before the first-person hand pass. */
    public void onWorldRenderEnd(float partialTicks) {
        if (this.monkeyActive()) {
            this.monkeyRenderer.finishWorldRender(this.minecraft, partialTicks, this.getAmountPercent(), this.accumulation);
        }
    }
}
