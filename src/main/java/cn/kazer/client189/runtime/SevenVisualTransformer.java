package cn.kazer.client189.runtime;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Version-specific rendering call sites only. Original bytes are kept for unknown shapes. */
public final class SevenVisualTransformer implements IClassTransformer {
    private static final String VISUAL = "com/cheatbreaker/client/kazerui/module/modules/visual/";
    private static final String WEATHER = "com/cheatbreaker/client/kazerui/module/modules/mechanic/ModuleWeatherChanger";
    private static final String HOOK = "cn/kazer/client189/runtime/SevenVisualHooks";
    public byte[] transform(String name, String target, byte[] bytes) {
        if (bytes == null || target == null) return bytes;
        boolean particle = target.startsWith("net.minecraft.client.particle.");
        if (!particle && !(target.equals("net.minecraft.client.renderer.EntityRenderer")
            || target.equals("net.minecraft.client.renderer.RenderGlobal")
            || target.equals("net.minecraft.client.renderer.entity.RenderItem")
            || target.equals("net.minecraft.client.renderer.entity.RenderEntityItem")
            || target.equals("net.minecraft.client.renderer.entity.layers.LayerArmorBase")
            || target.equals("net.minecraft.client.gui.GuiScreen") || target.equals("net.minecraft.client.audio.SoundHandler"))) return bytes;
        ClassNode type = new ClassNode(); new ClassReader(bytes).accept(type, 0);
        for (MethodNode method : type.methods) for (AbstractInsnNode insn : method.instructions.toArray())
            if (insn instanceof MethodInsnNode) {
                String owner = ((MethodInsnNode)insn).owner;
                if (owner.equals(HOOK) || owner.equals(WEATHER) || owner.equals(VISUAL + "ModuleParticleChanger")
                    || owner.equals(VISUAL + "ModuleGlintColorizer")) return bytes;
            }
        int count = 0;
        for (MethodNode method : type.methods) {
            if (target.endsWith("SoundHandler") && is(method.name,"playSound","func_147682_a")
                && method.desc.equals("(Lnet/minecraft/client/audio/ISound;)V")) {
                InsnList code = new InsnList(); LabelNode run = new LabelNode();
                code.add(new VarInsnNode(Opcodes.ALOAD,1));
                code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"muteSound","(Lnet/minecraft/client/audio/ISound;)Z",false));
                code.add(new JumpInsnNode(Opcodes.IFEQ,run)); code.add(new InsnNode(Opcodes.RETURN)); code.add(run);
                if(type.version>=Opcodes.V1_7)code.add(new FrameNode(Opcodes.F_SAME,0,null,0,null));
                method.instructions.insert(code); count++;
            }
            if (target.endsWith("EffectRenderer") && is(method.name, "addEffect", "func_78873_a")
                && method.desc.equals("(Lnet/minecraft/client/particle/EntityFX;)V")) {
                InsnList code = new InsnList(); LabelNode run = new LabelNode();
                code.add(new VarInsnNode(Opcodes.ALOAD, 1));
                code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, VISUAL + "ModuleParticleChanger", "keep", "(Ljava/lang/Object;)Z", false));
                code.add(new JumpInsnNode(Opcodes.IFNE, run)); code.add(new InsnNode(Opcodes.RETURN)); code.add(run);
                if(type.version>=Opcodes.V1_7)code.add(new FrameNode(Opcodes.F_SAME,0,null,0,null));
                method.instructions.insert(code); count++;
            }
            boolean particleRender = particle && is(method.name, "renderParticle", "func_180434_a")
                && method.desc.equals("(Lnet/minecraft/client/renderer/WorldRenderer;Lnet/minecraft/entity/Entity;FFFFFF)V");
            boolean itemTransform = target.endsWith("RenderEntityItem") && is(method.name, "func_177077_a", "func_177077_a")
                && method.desc.equals("(Lnet/minecraft/entity/item/EntityItem;DDDFLnet/minecraft/client/resources/model/IBakedModel;)I");
            int translations = 0;
            for (AbstractInsnNode insn : method.instructions.toArray()) {
                if (particleRender && insn instanceof FieldInsnNode && insn.getOpcode() == Opcodes.GETFIELD) {
                    FieldInsnNode field = (FieldInsnNode)insn;
                    int shift = is(field.name, "particleRed", "field_70552_h") ? 16
                        : is(field.name, "particleGreen", "field_70553_i") ? 8 : is(field.name, "particleBlue", "field_70551_j") ? 0 : -1;
                    boolean size = is(field.name, "particleScale", "field_70544_f");
                    if (field.desc.equals("F") && (size || shift >= 0)) {
                        InsnList code = new InsnList(); code.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        if (!size) code.add(new IntInsnNode(Opcodes.BIPUSH, shift));
                        code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, VISUAL + "ModuleParticleChanger",
                            size ? "scale" : "channel", size ? "(FLjava/lang/Object;)F" : "(FLjava/lang/Object;I)F", false));
                        method.instructions.insert(insn, code); count++;
                    }
                }
                if (target.endsWith("RenderItem") && is(method.name, "renderEffect", "func_180451_a")
                    && insn instanceof LdcInsnNode && Integer.valueOf(-8372020).equals(((LdcInsnNode)insn).cst)) {
                    method.instructions.insert(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, VISUAL + "ModuleGlintColorizer", "itemColor", "(I)I", false)); count++;
                }
                if (!(insn instanceof MethodInsnNode)) continue;
                MethodInsnNode call = (MethodInsnNode)insn;
                if (target.endsWith("GuiScreen") && is(method.name, "renderToolTip", "func_146285_a")
                    && is(call.name, "drawHoveringText", "func_146283_a")
                    && call.desc.equals("(Ljava/util/List;IILnet/minecraft/client/gui/FontRenderer;)V")) {
                    method.instructions.insertBefore(call, new VarInsnNode(Opcodes.ALOAD, 1));
                    method.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "tooltip",
                        "(Lnet/minecraft/client/gui/GuiScreen;Ljava/util/List;IILnet/minecraft/client/gui/FontRenderer;Lnet/minecraft/item/ItemStack;)V", false)); count++;
                }
                if (target.endsWith("LayerArmorBase") && is(method.name, "renderGlint", "func_177183_a")
                    && call.owner.equals("net/minecraft/client/renderer/GlStateManager")
                    && is(call.name, "color", "func_179131_c") && call.desc.equals("(FFFF)V")) {
                    method.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "armorColor", call.desc, false)); count++;
                }
                if (itemTransform && call.owner.equals("net/minecraft/client/renderer/GlStateManager")) {
                    if (is(call.name, "translate", "func_179109_b") && call.desc.equals("(FFF)V") && translations++ == 0) {
                        InsnList code = new InsnList(); code.add(new VarInsnNode(Opcodes.ALOAD, 1));
                        code.add(new VarInsnNode(Opcodes.ALOAD, 9)); code.add(new VarInsnNode(Opcodes.DLOAD, 4));
                        method.instructions.insertBefore(call, code);
                        method.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "itemTranslate",
                            "(FFFLnet/minecraft/entity/item/EntityItem;Lnet/minecraft/client/resources/model/IBakedModel;D)V", false)); count++;
                    }
                    if (is(call.name, "rotate", "func_179114_b") && call.desc.equals("(FFFF)V")) {
                        InsnList code = new InsnList(); code.add(new VarInsnNode(Opcodes.ALOAD, 1));
                        code.add(new VarInsnNode(Opcodes.FLOAD, 8)); code.add(new VarInsnNode(Opcodes.ALOAD, 9));
                        method.instructions.insertBefore(call, code);
                        method.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "itemRotate",
                            "(FFFFLnet/minecraft/entity/item/EntityItem;FLnet/minecraft/client/resources/model/IBakedModel;)V", false)); count++;
                    }
                }
                if (target.endsWith("EntityRenderer") || target.endsWith("RenderGlobal")) {
                    String owner = null, hook = null, desc = null;
                    if (is(call.name,"getThunderStrength","func_72819_i") && call.desc.equals("(F)F")) {
                        owner = WEATHER; hook = "thunder"; desc = "(F)F";
                    }
                    if (is(call.name, "getRainStrength", "func_72867_j") && call.desc.equals("(F)F")) {
                        owner = is(method.name, "addRainParticles", "func_78484_h") ? HOOK : WEATHER;
                        hook = owner.equals(HOOK) ? "rainSoundStrength" : "strength"; desc = "(F)F";
                    }
                    if (is(method.name, "renderRainSnow", "func_78474_d") || is(method.name, "addRainParticles", "func_78484_h")) {
                        if ((is(call.name, "canRain", "func_76738_d") || is(call.name, "getEnableSnow", "func_76746_c")) && call.desc.equals("()Z")) {
                            owner = WEATHER; hook = "precipitation"; desc = "(Z)Z";
                        }
                        if (is(call.name, "getFloatTemperature", "func_180626_a") || is(call.name, "getTemperatureAtHeight", "func_76939_a")) {
                            if (Type.getReturnType(call.desc).equals(Type.FLOAT_TYPE)) { owner = WEATHER; hook = "temperature"; desc = "(F)F"; }
                        }
                    }
                    if (hook != null) { method.instructions.insert(call, new MethodInsnNode(Opcodes.INVOKESTATIC, owner, hook, desc, false)); count++; }
                }
            }
        }
        if (count == 0) return bytes;
        // Call replacements preserve every existing stack/local frame. Do not load
        // Minecraft classes through ASM's parent loader while LaunchWrapper defines them.
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        type.accept(writer); System.out.println("[Kazer Visual Modules] Adapted " + target + " (" + count + ")");
        return writer.toByteArray();
    }
    private static boolean is(String name, String mcp, String srg) { return name.equals(mcp) || name.equals(srg); }
}
