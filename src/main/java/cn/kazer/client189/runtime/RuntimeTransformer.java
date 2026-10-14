package cn.kazer.client189.runtime;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Small version adapters; Kazer's UI/config/module sources remain independent of OptiFine. */
public final class RuntimeTransformer implements IClassTransformer {
    private static final String HOOK = "cn/kazer/client189/runtime/RuntimeHooks";
    private static final String PINNED = "cn/kazer/client189/runtime/KazerPinnedServers";
    @Override public byte[] transform(String name, String target, byte[] bytes) {
        if (bytes == null || !(target.equals("net.minecraft.client.renderer.ItemRenderer")
                || target.equals("net.minecraft.client.renderer.entity.layers.LayerHeldItem")
                || target.equals("net.minecraft.client.renderer.entity.layers.LayerCape")
                || target.equals("net.minecraft.client.renderer.entity.layers.LayerArmorBase")
                || target.equals("net.minecraft.client.model.ModelBiped")
                || target.equals("net.minecraft.client.gui.GuiIngame")
                || target.equals("net.minecraft.client.gui.GuiNewChat")
                || target.equals("net.minecraft.client.gui.GuiChat")
                || target.equals("net.minecraft.client.gui.ChatLine")
                || target.equals("net.minecraft.client.network.NetHandlerPlayClient")
                || target.equals("net.minecraft.client.renderer.EntityRenderer")
                || target.equals("net.minecraft.client.renderer.RenderGlobal")
                || target.equals("net.minecraft.client.particle.EffectRenderer")
                || target.equals("net.minecraft.client.Minecraft")
                || target.equals("net.minecraft.client.entity.EntityPlayerSP")
                || target.equals("net.minecraft.client.settings.GameSettings")
                || target.equals("net.minecraft.client.settings.KeyBinding")
                || target.equals("net.minecraft.client.renderer.entity.RendererLivingEntity")
                || target.equals("net.minecraftforge.client.GuiIngameForge")
                || target.equals("net.minecraft.client.gui.GuiScreen")
                || target.equals("net.minecraft.client.gui.GuiKeyBindingList")
                || target.equals("net.minecraftforge.fml.client.config.GuiUtils")
                || target.equals("net.minecraft.client.gui.GuiScreenResourcePacks")
                || target.equals("net.minecraft.client.gui.inventory.GuiContainer")
                || target.equals("net.minecraft.client.gui.GuiTextField")
                || target.equals("net.minecraft.client.renderer.InventoryEffectRenderer")
                || target.equals("net.minecraft.client.gui.achievement.GuiAchievement")
                || target.equals("net.minecraft.entity.player.InventoryPlayer")
                || target.equals("net.minecraft.client.renderer.entity.RenderManager")
                || target.equals("net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher")
                || target.equals("net.minecraft.client.renderer.entity.RenderItem")
                || target.equals("net.minecraft.client.renderer.entity.RenderEntityItem")
                || target.equals("net.minecraft.client.gui.GuiPlayerTabOverlay")
                || target.equals("net.minecraft.client.renderer.BlockRendererDispatcher")
                || target.equals("net.minecraft.client.multiplayer.ServerList")
                || target.equals("net.minecraft.client.gui.ServerSelectionList")
                || target.equals("net.minecraft.client.gui.GuiMultiplayer")
                || target.equals("net.optifine.CustomSky") || target.equals("optifine.CustomSky"))) return bytes;
        ClassNode type = new ClassNode(); new ClassReader(bytes).accept(type, 0);
        int count = 0;
        if(target.endsWith("GuiKeyBindingList")) for(MethodNode method:type.methods)
            if(method.name.equals("<init>")) for(AbstractInsnNode insn:method.instructions.toArray()) if(insn instanceof MethodInsnNode) {
                MethodInsnNode call=(MethodInsnNode)insn;
                if(call.owner.equals("net/minecraft/client/settings/KeyBinding") && call.desc.equals("()Ljava/util/Set;")) {
                    method.instructions.set(call,new MethodInsnNode(Opcodes.INVOKESTATIC,
                        "com/cheatbreaker/client/kazerui/gui/NativeControlBindings","categories","()Ljava/util/Set;",false));count++;
                }
            }
        if(target.endsWith("GuiKeyBindingList")) for(MethodNode method:type.methods)
            if(method.name.equals("<init>")) for(AbstractInsnNode insn:method.instructions.toArray()) if(insn instanceof FieldInsnNode) {
                FieldInsnNode access=(FieldInsnNode)insn;
                if(access.getOpcode()==Opcodes.GETFIELD && is(access.name,"keyBindings","field_74324_K")
                        && access.desc.equals("[Lnet/minecraft/client/settings/KeyBinding;")) {
                    method.instructions.insert(access,new MethodInsnNode(Opcodes.INVOKESTATIC,
                        "com/cheatbreaker/client/kazerui/gui/NativeControlBindings","visible",
                        "([Lnet/minecraft/client/settings/KeyBinding;)[Lnet/minecraft/client/settings/KeyBinding;",false));count++;
                }
            }
        if(target.endsWith("GuiScreen")) for(MethodNode method:type.methods)
            if(is(method.name,"keyTyped","func_73869_a")) for(AbstractInsnNode insn:method.instructions.toArray()) if(insn instanceof MethodInsnNode) {
                MethodInsnNode call=(MethodInsnNode)insn;
                if(call.owner.equals("net/minecraft/client/Minecraft") && is(call.name,"setIngameFocus","func_71381_h")) {
                    method.instructions.set(call,new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"resumeAfterEscape","(Lnet/minecraft/client/Minecraft;)V",false));count++;
                }
            }
        if(target.endsWith("GuiScreenResourcePacks")) for(MethodNode method:type.methods)
            if(is(method.name,"initGui","func_73866_w_")) for(AbstractInsnNode insn:method.instructions.toArray()) if(insn instanceof MethodInsnNode) {
                MethodInsnNode call=(MethodInsnNode)insn;
                if(is(call.name,"updateRepositoryEntriesAll","func_110611_a")) {
                    method.instructions.set(call,new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"resourcePackScan","(Ljava/lang/Object;)V",false));count++;
                }
            }
        if(target.endsWith("GuiScreen") || target.endsWith("GuiUtils")) for(MethodNode method:type.methods)
            if(is(method.name,"drawHoveringText","func_146283_a")) for(AbstractInsnNode insn:method.instructions.toArray()) if(insn instanceof MethodInsnNode) {
                MethodInsnNode call=(MethodInsnNode)insn;
                if(is(call.name,"drawStringWithShadow","func_175063_a") && call.desc.equals("(Ljava/lang/String;FFI)I")) {
                    method.instructions.set(call,new MethodInsnNode(Opcodes.INVOKESTATIC,"com/cheatbreaker/client/kazerui/module/render/TooltipHooks","drawLine","(Ljava/lang/Object;Ljava/lang/String;FFI)I",false));count++;
                }
            }
        if(target.equals("net.minecraft.client.gui.GuiIngame")) {
            type.interfaces.add("com/cheatbreaker/client/kazerui/module/render/TitleAccess");
            addChatGetter(type,"kazerTitleTime","titlesTimer","field_175195_w","I",Opcodes.IRETURN);
            addChatGetter(type,"kazerTitle","displayedTitle","field_175201_x","Ljava/lang/String;",Opcodes.ARETURN);
            addChatGetter(type,"kazerSubtitle","displayedSubTitle","field_175200_y","Ljava/lang/String;",Opcodes.ARETURN);
            addChatGetter(type,"kazerFadeIn","titleFadeIn","field_175199_z","I",Opcodes.IRETURN);
            addChatGetter(type,"kazerStay","titleDisplayTime","field_175192_A","I",Opcodes.IRETURN);
            addChatGetter(type,"kazerFadeOut","titleFadeOut","field_175193_B","I",Opcodes.IRETURN);count++;
        }
        if(target.equals("net.minecraft.client.gui.inventory.GuiContainer")) {
            type.interfaces.add("com/cheatbreaker/client/kazerui/gui/ContainerBounds");
            addChatGetter(type,"kazerLeft","guiLeft","field_147003_i","I",Opcodes.IRETURN);
            addChatGetter(type,"kazerTop","guiTop","field_147009_r","I",Opcodes.IRETURN);
            addChatGetter(type,"kazerWidth","xSize","field_146999_f","I",Opcodes.IRETURN);count++;
        }
        if (target.equals("net.minecraft.client.gui.GuiNewChat")) {
            String access="com/cheatbreaker/client/kazerui/module/render/KazerChatAccess";
            if (!type.interfaces.contains(access)) {
                type.interfaces.add(access);
                addChatGetter(type,"kazerDrawnLines","drawnChatLines","field_146253_i","Ljava/util/List;",Opcodes.ARETURN);
                addChatGetter(type,"kazerChatLines","chatLines","field_146252_h","Ljava/util/List;",Opcodes.ARETURN);
                addChatGetter(type,"kazerChatScroll","scrollPos","field_146250_j","I",Opcodes.IRETURN);
                addChatGetter(type,"kazerChatScrolled","isScrolled","field_146251_k","Z",Opcodes.IRETURN);
                FieldNode scrolled=null;
                for(FieldNode field:type.fields)if(is(field.name,"isScrolled","field_146251_k")&&field.desc.equals("Z"))scrolled=field;
                if(scrolled==null)throw new IllegalStateException("Native chat scrolled flag was not found");
                MethodNode setter=new MethodNode(Opcodes.ACC_PUBLIC,"kazerChatScrolled","(Z)V",null,null);
                setter.instructions.add(new VarInsnNode(Opcodes.ALOAD,0));setter.instructions.add(new VarInsnNode(Opcodes.ILOAD,1));
                setter.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD,type.name,scrolled.name,"Z"));
                setter.instructions.add(new InsnNode(Opcodes.RETURN));type.methods.add(setter);
                count++;
            }
        }
        if (target.equals("net.minecraft.client.gui.ChatLine")) {
            String tag="com/cheatbreaker/client/kazerui/module/render/ChatLineTag";
            if (!type.interfaces.contains(tag)) {
                type.interfaces.add(tag);
                type.fields.add(new FieldNode(Opcodes.ACC_PRIVATE,"kazerMessageId","I",null,null));
                addChatGetter(type,"kazerui$id","kazerMessageId","kazerMessageId","I",Opcodes.IRETURN);
                MethodNode setter=new MethodNode(Opcodes.ACC_PUBLIC,"kazerui$setId","(I)V",null,null);
                setter.instructions.add(new VarInsnNode(Opcodes.ALOAD,0));
                setter.instructions.add(new VarInsnNode(Opcodes.ILOAD,1));
                setter.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD,type.name,"kazerMessageId","I"));
                setter.instructions.add(new InsnNode(Opcodes.RETURN));type.methods.add(setter);count++;
            }
        }
        if(target.equals("net.minecraft.client.settings.KeyBinding")) {
            String state="com/cheatbreaker/client/kazerui/module/modules/mechanic/KazerKeyState";
            String pressed=null;
            for(FieldNode field:type.fields)if(field.desc.equals("Z") && is(field.name,"pressed","field_74513_e"))pressed=field.name;
            if(pressed==null)throw new IllegalStateException("Native key-state field was not found");
            if(!type.interfaces.contains(state)) {
                type.interfaces.add(state);
                MethodNode setter=new MethodNode(Opcodes.ACC_PUBLIC,"kazerSetPressed","(Z)V",null,null);
                setter.instructions.add(new VarInsnNode(Opcodes.ALOAD,0));setter.instructions.add(new VarInsnNode(Opcodes.ILOAD,1));
                setter.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD,type.name,pressed,"Z"));setter.instructions.add(new InsnNode(Opcodes.RETURN));type.methods.add(setter);count++;
            }
        }
        java.util.List<MethodNode> skyWrappers=new java.util.ArrayList<MethodNode>();
        for (MethodNode method : type.methods) {
            if(target.endsWith("EntityRenderer") && is(method.name,"setupFog","func_78468_a") && method.desc.equals("(IF)V")) {
                boolean adapted=false;
                for(AbstractInsnNode insn:method.instructions.toArray())if(insn instanceof MethodInsnNode && ((MethodInsnNode)insn).name.equals("clearDistanceFog"))adapted=true;
                if(!adapted) {
                    type.fields.add(new FieldNode(Opcodes.ACC_PRIVATE,"kazerDistanceFog","Z",null,null));
                    InsnList reset=new InsnList();reset.add(new VarInsnNode(Opcodes.ALOAD,0));reset.add(new InsnNode(Opcodes.ICONST_0));reset.add(new FieldInsnNode(Opcodes.PUTFIELD,type.name,"kazerDistanceFog","Z"));method.instructions.insert(reset);
                    for(AbstractInsnNode insn:method.instructions.toArray()) {
                        if(insn instanceof MethodInsnNode && is(((MethodInsnNode)insn).name,"doesXZShowFog","func_76568_b") && ((MethodInsnNode)insn).desc.equals("(II)Z")) {
                            InsnList mark=new InsnList();mark.add(new VarInsnNode(Opcodes.ALOAD,0));mark.add(new InsnNode(Opcodes.ICONST_1));mark.add(new FieldInsnNode(Opcodes.PUTFIELD,type.name,"kazerDistanceFog","Z"));method.instructions.insertBefore(insn,mark);
                        }
                        if(insn.getOpcode()==Opcodes.RETURN) {
                            InsnList hook=new InsnList();hook.add(new VarInsnNode(Opcodes.ALOAD,0));hook.add(new FieldInsnNode(Opcodes.GETFIELD,type.name,"kazerDistanceFog","Z"));
                            hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"clearDistanceFog","(Z)V",false));method.instructions.insertBefore(insn,hook);count++;
                        }
                    }
                }
            }
            if(target.endsWith("RenderGlobal") && method.name.equals("getRenderChunkOffset")
                && method.desc.equals("(Lnet/minecraft/util/BlockPos;Lnet/minecraft/client/renderer/chunk/RenderChunk;Lnet/minecraft/util/EnumFacing;ZI)Lnet/minecraft/client/renderer/chunk/RenderChunk;")) {
                boolean adapted=false,radial=false;
                for(AbstractInsnNode insn:method.instructions.toArray()) {
                    if(insn instanceof MethodInsnNode && ((MethodInsnNode)insn).name.equals("visibleChunk"))adapted=true;
                    if(insn instanceof FieldInsnNode && ((FieldInsnNode)insn).name.equals("renderDistanceSq"))radial=true;
                }
                if(!adapted&&radial) {
                    for(AbstractInsnNode insn:method.instructions.toArray())if(insn instanceof VarInsnNode && insn.getOpcode()==Opcodes.ILOAD && ((VarInsnNode)insn).var==4
                        && nextOpcode(insn) instanceof JumpInsnNode && nextOpcode(insn).getOpcode()==Opcodes.IFEQ)method.instructions.set(insn,new InsnNode(Opcodes.ICONST_0));
                    for(AbstractInsnNode insn:method.instructions.toArray())if(insn.getOpcode()==Opcodes.ARETURN) {
                        InsnList hook=new InsnList();hook.add(new VarInsnNode(Opcodes.ALOAD,1));
                        hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"visibleChunk","(Lnet/minecraft/client/renderer/chunk/RenderChunk;Lnet/minecraft/util/BlockPos;)Lnet/minecraft/client/renderer/chunk/RenderChunk;",false));
                        method.instructions.insertBefore(insn,hook);count++;
                    }
                }
            }
            if(target.endsWith("EntityRenderer")) for(AbstractInsnNode insn:method.instructions.toArray())if(insn instanceof MethodInsnNode) {
                MethodInsnNode call=(MethodInsnNode)insn;
                // OptiFine's exclusive-mode repair must not recreate our windowed
                // fullscreen HWND/context. Leave its normal exclusive path intact.
                if((call.owner.equals("Config")||call.owner.equals("net/optifine/Config"))
                    &&call.name.equals("checkDisplayMode")&&call.desc.equals("()V")&&call.getOpcode()==Opcodes.INVOKESTATIC) {
                    LabelNode done=new LabelNode();InsnList guard=new InsnList();
                    guard.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"com/cheatbreaker/client/kazerui/input/WindowMode","active","()Z",false));
                    guard.add(new JumpInsnNode(Opcodes.IFNE,done));method.instructions.insertBefore(call,guard);
                    InsnList tail=new InsnList();tail.add(done);tail.add(new FrameNode(Opcodes.F_SAME,0,null,0,null));
                    method.instructions.insert(call,tail);count++;
                }
            }
            if(target.endsWith("RenderGlobal") && is(method.name,"renderSky","func_174976_a")) {
                for(AbstractInsnNode insn:method.instructions.toArray())if(insn instanceof MethodInsnNode) {
                    MethodInsnNode call=(MethodInsnNode)insn;
                    if(call.getOpcode()==Opcodes.INVOKESTATIC && call.owner.equals("net/optifine/CustomSky") && call.name.equals("renderSky")
                        && call.desc.equals("(Lnet/minecraft/world/World;Lnet/minecraft/client/renderer/texture/TextureManager;F)V")) {
                        skyWrappers.add(customSkyWrapper(call));call.owner=type.name;call.name="kazerCustomSky";count++;
                    }
                    if(is(call.name,"getHorizon","func_72919_O") && call.desc.equals("()D")) {
                        AbstractInsnNode sub=nextOpcode(insn),store=nextOpcode(sub),load=nextOpcode(store),zero=nextOpcode(load),compare=nextOpcode(zero);
                        if(sub!=null && sub.getOpcode()==Opcodes.DSUB && store instanceof VarInsnNode && store.getOpcode()==Opcodes.DSTORE
                            && load instanceof VarInsnNode && load.getOpcode()==Opcodes.DLOAD && ((VarInsnNode)load).var==((VarInsnNode)store).var
                            && zero!=null && zero.getOpcode()==Opcodes.DCONST_0 && compare!=null && compare.getOpcode()==Opcodes.DCMPG) {
                            method.instructions.insert(load,new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"horizonBoxHeight","(D)D",false));count++;
                        }
                    }
                }
            }
            if((target.endsWith("GuiIngame") || target.endsWith("GuiIngameForge")) && (method.name.startsWith("render") || method.name.equals("func_175180_a"))) {
                for(AbstractInsnNode insn:method.instructions.toArray()) {
                    if(insn instanceof FieldInsnNode && insn.getOpcode()==Opcodes.GETFIELD) {
                        FieldInsnNode field=(FieldInsnNode)insn;
                        if(is(field.name,"titlesTimer","field_175195_w") && field.desc.equals("I")){method.instructions.insert(insn,new MethodInsnNode(Opcodes.INVOKESTATIC,"com/cheatbreaker/client/kazerui/module/modules/hud/ModuleTitles","vanillaTimer","(I)I",false));count++;}
                    }
                    if(insn instanceof MethodInsnNode) {
                        MethodInsnNode call=(MethodInsnNode)insn;
                        if(call.owner.equals("net/minecraft/client/settings/KeyBinding") && is(call.name,"isKeyDown","func_151470_d") && call.desc.equals("()Z")){call.setOpcode(Opcodes.INVOKESTATIC);call.owner=HOOK;call.name="listKey";call.desc="(Lnet/minecraft/client/settings/KeyBinding;)Z";call.itf=false;count++;}
                    }
                }
            }
            if(target.endsWith("RenderManager")) {
                for(AbstractInsnNode insn:method.instructions.toArray())if(is(method.name,"doRenderEntity","func_147939_a") && insn instanceof FieldInsnNode && insn.getOpcode()==Opcodes.GETFIELD) {
                    FieldInsnNode field=(FieldInsnNode)insn;
                    if(is(field.name,"debugBoundingBox","field_85095_o") && field.desc.equals("Z")) {
                        method.instructions.insert(insn,new MethodInsnNode(Opcodes.INVOKESTATIC,"com/cheatbreaker/client/kazerui/module/modules/visual/ModuleHitbox","enabled","(Z)Z",false));count++;
                    }
                }
                if(is(method.name,"renderDebugBoundingBox","func_85094_b") && method.desc.equals("(Lnet/minecraft/entity/Entity;DDDFF)V")) {
                    LabelNode fallback=new LabelNode();InsnList head=new InsnList();
                    head.add(new VarInsnNode(Opcodes.ALOAD,1));head.add(new VarInsnNode(Opcodes.DLOAD,2));head.add(new VarInsnNode(Opcodes.DLOAD,4));head.add(new VarInsnNode(Opcodes.DLOAD,6));head.add(new VarInsnNode(Opcodes.FLOAD,8));head.add(new VarInsnNode(Opcodes.FLOAD,9));
                    head.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"com/cheatbreaker/client/kazerui/module/modules/visual/ModuleHitbox","render","(Lnet/minecraft/entity/Entity;DDDFF)Z",false));head.add(new JumpInsnNode(Opcodes.IFEQ,fallback));head.add(new InsnNode(Opcodes.RETURN));head.add(fallback);head.add(new FrameNode(Opcodes.F_SAME,0,null,0,null));method.instructions.insert(head);count++;
                }
            }
            if(target.endsWith("RenderGlobal") && is(method.name,"drawSelectionBox","func_72731_b")) {
                for(AbstractInsnNode insn:method.instructions.toArray())if(insn instanceof MethodInsnNode) {
                    MethodInsnNode call=(MethodInsnNode)insn;
                    if(call.owner.equals("net/minecraft/client/renderer/RenderGlobal") && is(call.name,"drawSelectionBoundingBox","func_181561_a") && call.desc.equals("(Lnet/minecraft/util/AxisAlignedBB;)V")) {call.owner=HOOK;call.name="blockOutline";count++;}
                }
            }
            if(target.endsWith("RendererLivingEntity") && is(method.name,"setBrightness","func_177092_a") && method.desc.equals("(Lnet/minecraft/entity/EntityLivingBase;FZ)Z")) {
                String[] components={"red","green","blue","alpha"};int component=0;
                for(AbstractInsnNode insn:method.instructions.toArray())if(insn instanceof MethodInsnNode) {
                    MethodInsnNode call=(MethodInsnNode)insn;
                    if(component<4 && call.owner.equals("java/nio/FloatBuffer") && call.name.equals("put") && call.desc.equals("(F)Ljava/nio/FloatBuffer;")) {
                        method.instructions.insertBefore(insn,new MethodInsnNode(Opcodes.INVOKESTATIC,"com/cheatbreaker/client/kazerui/module/modules/visual/ModuleHitColor",components[component++],"(F)F",false));count++;
                    }
                }
            }
            if(target.endsWith("LayerArmorBase") && is(method.name,"shouldCombineTextures","func_177142_b") && method.desc.equals("()Z")) {
                for(AbstractInsnNode insn:method.instructions.toArray())if(insn.getOpcode()==Opcodes.IRETURN) {
                    method.instructions.insertBefore(insn,new MethodInsnNode(Opcodes.INVOKESTATIC,"com/cheatbreaker/client/kazerui/module/modules/visual/ModuleHitColor","combineArmor","(Z)Z",false));count++;
                }
            }
            if(target.endsWith("RenderEntityItem") && is(method.name,"doRender","func_76986_a") && method.desc.equals("(Lnet/minecraft/entity/item/EntityItem;DDDFF)V")) {
                LabelNode fallback=new LabelNode();InsnList head=new InsnList();
                head.add(new VarInsnNode(Opcodes.ALOAD,1));head.add(new VarInsnNode(Opcodes.DLOAD,2));head.add(new VarInsnNode(Opcodes.DLOAD,4));head.add(new VarInsnNode(Opcodes.DLOAD,6));head.add(new VarInsnNode(Opcodes.FLOAD,9));
                head.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"com/cheatbreaker/client/kazerui/module/modules/visual/ModuleItems2d","render","(Lnet/minecraft/entity/item/EntityItem;DDDF)Z",false));
                head.add(new JumpInsnNode(Opcodes.IFEQ,fallback));head.add(new InsnNode(Opcodes.RETURN));head.add(fallback);head.add(new FrameNode(Opcodes.F_SAME,0,null,0,null));method.instructions.insert(head);count++;
            }
            if(target.endsWith("EntityRenderer")) {
                final String camera="com/cheatbreaker/client/kazerui/module/modules/mechanic/CameraHooks";
                for(AbstractInsnNode insn:method.instructions.toArray()) {
                    if(insn instanceof MethodInsnNode) {
                        MethodInsnNode call=(MethodInsnNode)insn;
                        if(call.owner.equals("org/lwjgl/input/Keyboard") && call.name.equals("isKeyDown") && call.desc.equals("(I)Z")){call.owner=HOOK;call.name="zoomKeyboard";count++;}
                        else if(call.owner.equals("org/lwjgl/input/Mouse") && call.name.equals("isButtonDown") && call.desc.equals("(I)Z")){call.owner=HOOK;call.name="zoomMouse";count++;}
                        else if(call.owner.equals("net/minecraft/client/settings/GameSettings") && is(call.name,"isKeyDown","func_100015_a") && call.desc.equals("(Lnet/minecraft/client/settings/KeyBinding;)Z")){call.owner=HOOK;call.name="zoomKey";count++;}
                        if(is(call.name,"setAngles","func_70082_c") && call.desc.equals("(FF)V")) {
                            call.setOpcode(Opcodes.INVOKESTATIC);call.owner=camera;call.name="turn";
                            call.desc="(Lnet/minecraft/entity/Entity;FF)V";call.itf=false;count++;
                        }
                    }
                    if(insn instanceof FieldInsnNode && insn.getOpcode()==Opcodes.GETFIELD) {
                        FieldInsnNode field=(FieldInsnNode)insn;
                        if(field.owner.equals("net/minecraft/client/settings/GameSettings") && is(field.name,"mouseSensitivity","field_74341_c") && field.desc.equals("F")) {
                            method.instructions.insert(insn,new MethodInsnNode(Opcodes.INVOKESTATIC,"com/cheatbreaker/client/kazerui/module/modules/mechanic/ModuleZoom","sensitivity","(F)F",false));count++;
                        }
                    }
                    if(is(method.name,"orientCamera","func_78467_g") && insn instanceof FieldInsnNode && insn.getOpcode()==Opcodes.GETFIELD) {
                        FieldInsnNode field=(FieldInsnNode)insn;String hook=null;
                        if(is(field.name,"rotationYaw","field_70177_z"))hook="yaw";
                        else if(is(field.name,"rotationPitch","field_70125_A"))hook="pitch";
                        else if(is(field.name,"prevRotationYaw","field_70126_B"))hook="previousYaw";
                        else if(is(field.name,"prevRotationPitch","field_70127_C"))hook="previousPitch";
                        if(hook!=null && field.desc.equals("F")) {
                            method.instructions.set(insn,new MethodInsnNode(Opcodes.INVOKESTATIC,camera,hook,"(Lnet/minecraft/entity/Entity;)F",false));count++;
                        }
                    }
                }
            }
            if(target.endsWith("NetHandlerPlayClient") && is(method.name,"handleEntityStatus","func_147236_a")) {
                for(AbstractInsnNode insn:method.instructions.toArray())if(insn instanceof MethodInsnNode) {
                    MethodInsnNode call=(MethodInsnNode)insn;
                    if(call.desc.equals("(B)V") && is(call.name,"handleStatusUpdate","func_70103_a")) {
                        InsnList observe=new InsnList();observe.add(new InsnNode(Opcodes.DUP2));
                        observe.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"com/cheatbreaker/client/kazerui/module/modules/hud/CombatHud","status","(Lnet/minecraft/entity/Entity;B)V",false));
                        method.instructions.insertBefore(insn,observe);count++;
                    }
                }
            }
            if(target.endsWith("LayerCape") && is(method.name,"doRenderLayer","func_177141_a")
                    && method.desc.startsWith("(Lnet/minecraft/client/entity/AbstractClientPlayer;")) {
                LabelNode vanilla=new LabelNode();InsnList guard=new InsnList();guard.add(new VarInsnNode(Opcodes.ALOAD,1));
                guard.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"cn/kazer/client189/runtime/KazerCosmeticLayer","customCape","(Lnet/minecraft/client/entity/AbstractClientPlayer;)Z",false));
                guard.add(new JumpInsnNode(Opcodes.IFEQ,vanilla));guard.add(new InsnNode(Opcodes.RETURN));guard.add(vanilla);method.instructions.insert(guard);count++;
            }
            if (target.endsWith("GuiChat") && is(method.name,"keyTyped","func_73869_a")) {
                String input = null;
                for (FieldNode field : type.fields) if (is(field.name,"inputField","field_146415_a")) input = field.name;
                if (input == null) throw new IllegalStateException("Native chat input field was not found");
                for (AbstractInsnNode instruction : method.instructions.toArray()) if (instruction instanceof MethodInsnNode) {
                    MethodInsnNode call = (MethodInsnNode)instruction;
                    if (call.owner.equals("net/minecraft/client/Minecraft") && is(call.name,"displayGuiScreen","func_147108_a")) {
                        InsnList args = new InsnList(); args.add(new VarInsnNode(Opcodes.ALOAD,0));
                        args.add(new FieldInsnNode(Opcodes.GETFIELD,type.name,input,"Lnet/minecraft/client/gui/GuiTextField;"));
                        args.add(new VarInsnNode(Opcodes.ILOAD,2));method.instructions.insertBefore(call,args);
                        method.instructions.set(call,new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"chatClose","(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/gui/GuiScreen;Lnet/minecraft/client/gui/GuiTextField;I)V",false));
                    }
                }
                count++;
            }
            if (target.endsWith("GuiChat") && is(method.name,"drawScreen","func_73863_a")) {
                for (AbstractInsnNode instruction : method.instructions.toArray()) if (instruction instanceof MethodInsnNode) {
                    MethodInsnNode call = (MethodInsnNode)instruction;
                    if (is(call.name,"drawRect","func_73734_a") && call.desc.equals("(IIIII)V"))
                        method.instructions.set(call,new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"chatInputRect","(IIIII)V",false));
                }
                count++;
            }
            if (target.endsWith("GuiScreen") && is(method.name,"sendChatMessage","func_175281_b") && method.desc.equals("(Ljava/lang/String;Z)V")) {
                InsnList code = new InsnList(); LabelNode send = new LabelNode();
                code.add(new VarInsnNode(Opcodes.ALOAD,1));
                code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"chatSend","(Ljava/lang/String;)Ljava/lang/String;",false));
                code.add(new VarInsnNode(Opcodes.ASTORE,1));code.add(new VarInsnNode(Opcodes.ALOAD,1));
                code.add(new JumpInsnNode(Opcodes.IFNONNULL,send));code.add(new InsnNode(Opcodes.RETURN));code.add(send);
                method.instructions.insert(code);count++;
            }
            if(target.endsWith("EntityPlayerSP") && is(method.name,"onLivingUpdate","func_70636_d")) {
                for(AbstractInsnNode insn:method.instructions.toArray())if(insn instanceof FieldInsnNode && insn.getOpcode()==Opcodes.PUTFIELD
                        && is(((FieldInsnNode)insn).name,"sprintToggleTimer","field_71156_d")) {
                    method.instructions.insertBefore(insn,new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"sprintTimer","(I)I",false));count++;
                }
            }
            if(target.endsWith("Minecraft")&&is(method.name,"runTick","func_71407_l")) {
                for(AbstractInsnNode insn:method.instructions.toArray()) if(insn instanceof MethodInsnNode
                        &&is(((MethodInsnNode)insn).name,"handleKeyboardInput","func_146282_l")) {
                    for(AbstractInsnNode previous=insn.getPrevious();previous!=null;previous=previous.getPrevious())
                        if(previous instanceof MethodInsnNode&&((MethodInsnNode)previous).owner.equals("org/lwjgl/input/Keyboard")
                                &&((MethodInsnNode)previous).name.equals("getEventKeyState")) {
                            method.instructions.set(previous,new MethodInsnNode(Opcodes.INVOKESTATIC,"cn/kazer/client189/input/ImeHooks","shouldDispatchInGui","()Z",false));count++;break;
                        }
                }
            }
            if(target.endsWith("GuiScreen")&&is(method.name,"handleKeyboardInput","func_146282_l")) {
                for(AbstractInsnNode insn:method.instructions.toArray()) if(insn instanceof MethodInsnNode) {
                    MethodInsnNode call=(MethodInsnNode)insn;
                    if(call.owner.equals("org/lwjgl/input/Keyboard")&&call.name.equals("getEventKeyState")) {
                        method.instructions.set(call,new MethodInsnNode(Opcodes.INVOKESTATIC,"cn/kazer/client189/input/ImeHooks","shouldDispatch","()Z",false));count++;
                    }
                }
            }
            if(target.endsWith("GuiTextField")&&is(method.name,"drawTextBox","func_146194_f")) {
                for(AbstractInsnNode insn:method.instructions.toArray()) if(insn.getOpcode()==Opcodes.RETURN) {
                    InsnList code=new InsnList();code.add(new VarInsnNode(Opcodes.ALOAD,0));
                    code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"cn/kazer/client189/input/ImeHooks","textField","(Ljava/lang/Object;)V",false));
                    method.instructions.insertBefore(insn,code);count++;
                }
            }
            if(target.endsWith("Minecraft")&&is(method.name,"toggleFullscreen","func_71352_k")) {
                int switches=0;
                for(AbstractInsnNode insn:method.instructions.toArray())if(insn instanceof MethodInsnNode) {
                    MethodInsnNode call=(MethodInsnNode)insn;
                    if(call.owner.equals("org/lwjgl/opengl/Display")&&call.name.equals("setFullscreen")&&call.desc.equals("(Z)V"))switches++;
                }
                if(switches==1&&method.desc.equals("()V")) {
                    LabelNode original=new LabelNode();InsnList code=new InsnList();
                    code.add(new VarInsnNode(Opcodes.ALOAD,0));
                    code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"cn/kazer/client189/input/FullscreenTransition","toggle","(Lnet/minecraft/client/Minecraft;)Z",false));
                    code.add(new JumpInsnNode(Opcodes.IFEQ,original));code.add(new InsnNode(Opcodes.RETURN));code.add(original);
                    method.instructions.insert(code);count++;
                }
                for(AbstractInsnNode insn:method.instructions.toArray()) if(insn.getOpcode()==Opcodes.RETURN) {
                    method.instructions.insertBefore(insn,new MethodInsnNode(Opcodes.INVOKESTATIC,"cn/kazer/client189/input/WindowsFullscreenImeSupport","updateWindowLayer","()V",false));count++;
                }
            }
            if(target.endsWith("RenderGlobal")&&is(method.name,"updateChunks","func_174967_a")&&method.desc.equals("(J)V")) {
                InsnList code=new InsnList();code.add(new VarInsnNode(Opcodes.LLOAD,1));
                code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"chunkDeadline","(J)J",false));
                code.add(new VarInsnNode(Opcodes.LSTORE,1));method.instructions.insert(code);count++;
            }
            if(target.endsWith("ServerList")&&is(method.name,"loadServerList","func_78853_a")) {
                for(AbstractInsnNode insn:method.instructions.toArray())if(insn.getOpcode()==Opcodes.RETURN) {
                    InsnList code=new InsnList();code.add(new VarInsnNode(Opcodes.ALOAD,0));
                    code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,PINNED,"loaded","(Ljava/lang/Object;)V",false));
                    method.instructions.insertBefore(insn,code);count++;
                }
            }
            if(target.endsWith("ServerList")&&is(method.name,"saveServerList","func_78855_b")) {
                for(AbstractInsnNode insn:method.instructions.toArray())if(insn instanceof MethodInsnNode) {
                    MethodInsnNode call=(MethodInsnNode)insn;
                    if(is(call.name,"getNBTCompound","func_78836_a")) {
                        method.instructions.insertBefore(call,new MethodInsnNode(Opcodes.INVOKESTATIC,PINNED,"serverNbt",
                                "(Ljava/lang/Object;)Ljava/lang/Object;",false));
                        method.instructions.set(call,new TypeInsnNode(Opcodes.CHECKCAST,Type.getReturnType(call.desc).getInternalName()));count++;
                    } else if(is(call.name,"appendTag","func_74742_a")) {
                        method.instructions.set(call,new MethodInsnNode(Opcodes.INVOKESTATIC,PINNED,"appendNbt",
                                "(Ljava/lang/Object;Ljava/lang/Object;)V",false));count++;
                    }
                }
            }
            if(target.endsWith("ServerList")&&is(method.name,"swapServers","func_78857_a")) {
                InsnList code=new InsnList();LabelNode run=new LabelNode();
                code.add(new VarInsnNode(Opcodes.ALOAD,0));code.add(new VarInsnNode(Opcodes.ILOAD,1));code.add(new VarInsnNode(Opcodes.ILOAD,2));
                code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,PINNED,"blockSwap","(Ljava/lang/Object;II)Z",false));
                code.add(new JumpInsnNode(Opcodes.IFEQ,run));code.add(new InsnNode(Opcodes.RETURN));code.add(run);
                method.instructions.insert(code);count++;
            }
            if(target.endsWith("ServerSelectionList")&&method.name.equals("func_148195_a")) {
                TypeInsnNode creation=null;
                for(AbstractInsnNode insn:method.instructions.toArray()) {
                    if(insn instanceof TypeInsnNode&&insn.getOpcode()==Opcodes.NEW
                            &&((TypeInsnNode)insn).desc.endsWith("ServerListEntryNormal"))creation=(TypeInsnNode)insn;
                    if(creation!=null&&insn instanceof MethodInsnNode&&((MethodInsnNode)insn).name.equals("<init>")
                            &&((MethodInsnNode)insn).owner.endsWith("ServerListEntryNormal")) {
                        AbstractInsnNode duplicate=creation.getNext();while(duplicate!=null&&duplicate.getOpcode()<0)duplicate=duplicate.getNext();
                        if(duplicate==null||duplicate.getOpcode()!=Opcodes.DUP)throw new IllegalStateException("Unexpected server row constructor");
                        String owner=((MethodInsnNode)insn).owner;
                        method.instructions.remove(duplicate);method.instructions.remove(creation);
                        method.instructions.insertBefore(insn,new MethodInsnNode(Opcodes.INVOKESTATIC,PINNED,"entry",
                                "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",false));
                        method.instructions.set(insn,new TypeInsnNode(Opcodes.CHECKCAST,owner));creation=null;count++;
                    }
                }
            }
            if(target.endsWith("GuiMultiplayer")&&is(method.name,"selectServer","func_146790_a")) {
                for(AbstractInsnNode insn:method.instructions.toArray())if(insn.getOpcode()==Opcodes.RETURN) {
                    InsnList code=new InsnList();code.add(new VarInsnNode(Opcodes.ALOAD,0));code.add(new VarInsnNode(Opcodes.ILOAD,1));
                    code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,PINNED,"selected","(Ljava/lang/Object;I)V",false));
                    method.instructions.insertBefore(insn,code);count++;
                }
            }
            if(target.endsWith("GuiMultiplayer")&&(method.name.equals("func_175392_a")||method.name.equals("func_175394_b"))) {
                InsnList code=new InsnList();code.add(new VarInsnNode(Opcodes.ALOAD,0));code.add(new VarInsnNode(Opcodes.ALOAD,1));
                code.add(new VarInsnNode(Opcodes.ILOAD,2));code.add(new InsnNode(method.name.equals("func_175392_a")?Opcodes.ICONST_1:Opcodes.ICONST_0));
                code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,PINNED,"canMove","(Ljava/lang/Object;Ljava/lang/Object;IZ)Z",false));
                code.add(new InsnNode(Opcodes.IRETURN));method.instructions.insert(code);count++;
            }
            if(target.equals("net.minecraft.client.Minecraft")) {
                for(AbstractInsnNode insn:method.instructions.toArray()) if(insn instanceof MethodInsnNode) {
                    MethodInsnNode call=(MethodInsnNode)insn;
                    if(call.owner.equals("org/lwjgl/opengl/Display") && call.name.equals("setTitle")
                            && call.desc.equals("(Ljava/lang/String;)V")) {
                        InsnList title=new InsnList();title.add(new InsnNode(Opcodes.POP));
                        title.add(new LdcInsnNode("1.8.9"));
                        title.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"com/cheatbreaker/client/util/KazerBuildInfo","windowTitle","(Ljava/lang/String;)Ljava/lang/String;",false));
                        method.instructions.insertBefore(call,title);count++;
                    }
                }
            }
            if(target.endsWith("Minecraft")&&is(method.name,"drawSplashScreen","func_180510_a")){
                InsnList code=new InsnList();code.add(new VarInsnNode(Opcodes.ALOAD,1));
                code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"splash","(Ljava/lang/Object;)V",false));
                code.add(new InsnNode(Opcodes.RETURN));method.instructions.insert(code);count++;
            }
            if (target.endsWith("GameSettings") && is(method.name,"loadOptions","func_74300_a")) {
                method.instructions.insert(new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"optionsLoad","()V",false));count++;
            }
            if (target.endsWith("GameSettings") && is(method.name,"saveOptions","func_74303_b")) {
                for(AbstractInsnNode insn:method.instructions.toArray())if(insn.getOpcode()==Opcodes.RETURN){
                    method.instructions.insertBefore(insn,new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"optionsSave","()V",false));count++;
                }
            }
            if (target.endsWith("BlockRendererDispatcher") && is(method.name,"renderBlock","func_175018_a")) {
                InsnList code=new InsnList();LabelNode render=new LabelNode();code.add(new VarInsnNode(Opcodes.ALOAD,1));
                code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"clearGlass","(Ljava/lang/Object;)Z",false));
                code.add(new JumpInsnNode(Opcodes.IFEQ,render));code.add(new InsnNode(Opcodes.ICONST_1));code.add(new InsnNode(Opcodes.IRETURN));code.add(render);
                method.instructions.insert(code);count++;
            }
            if(target.endsWith("GuiPlayerTabOverlay")) {
                if(is(method.name,"drawPing","func_175245_a")) {
                    InsnList code=new InsnList();LabelNode draw=new LabelNode();
                    code.add(new VarInsnNode(Opcodes.ILOAD,1));code.add(new VarInsnNode(Opcodes.ILOAD,2));code.add(new VarInsnNode(Opcodes.ILOAD,3));
                    code.add(new VarInsnNode(Opcodes.ALOAD,4));
                    code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"tabPing","(IIILjava/lang/Object;)Z",false));
                    code.add(new JumpInsnNode(Opcodes.IFNE,draw));code.add(new InsnNode(Opcodes.RETURN));code.add(draw);
                    method.instructions.insert(code);count++;
                }
                if(is(method.name,"getPlayerName","func_175243_a")) {
                    for(AbstractInsnNode insn:method.instructions.toArray())if(insn.getOpcode()==Opcodes.ARETURN){
                        InsnList code=new InsnList();code.add(new VarInsnNode(Opcodes.ALOAD,1));
                        code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"tabName","(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/String;",false));
                        method.instructions.insertBefore(insn,code);count++;
                    }
                }
                if(is(method.name,"renderPlayerlist","func_175249_a")
                        || is(method.name,"drawScoreboardValues","func_175247_a")) {
                    for(AbstractInsnNode insn:method.instructions.toArray())if(insn instanceof FieldInsnNode && insn.getOpcode()==Opcodes.GETFIELD) {
                        FieldInsnNode field=(FieldInsnNode)insn;String hook=is(field.name,"header","field_175256_i")?"header":is(field.name,"footer","field_175255_h")?"footer":null;
                        if(hook!=null && field.desc.equals("Lnet/minecraft/util/IChatComponent;")){method.instructions.insert(insn,new MethodInsnNode(Opcodes.INVOKESTATIC,"com/cheatbreaker/client/kazerui/module/modules/mechanic/ModuleTab",hook,"(Lnet/minecraft/util/IChatComponent;)Lnet/minecraft/util/IChatComponent;",false));count++;}
                    }
                    for(AbstractInsnNode insn:method.instructions.toArray())if(insn instanceof MethodInsnNode){
                        MethodInsnNode call=(MethodInsnNode)insn;
                        if(is(call.name,"drawRect","func_73734_a") && call.desc.equals("(IIIII)V")){method.instructions.insertBefore(insn,new MethodInsnNode(Opcodes.INVOKESTATIC,"com/cheatbreaker/client/kazerui/module/modules/mechanic/ModuleTab","boxColor","(I)I",false));count++;}
                        if(is(call.name,"drawStringWithShadow","func_175063_a")&&call.desc.equals("(Ljava/lang/String;FFI)I")) {
                            method.instructions.set(call,new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"tabText","(Ljava/lang/Object;Ljava/lang/String;FFI)I",false));count++;
                        }
                    }
                }
            }
            if (target.endsWith("GuiScreen") && is(method.name,"drawWorldBackground","func_146270_b")) {
                guard(method,"drawGuiBackground");count++;
            }
            if (target.endsWith("GuiAchievement") && is(method.name,"updateAchievementWindow","func_146254_a")) {
                guard(method,"hideAchievements");count++;
            }
            if (target.endsWith("InventoryPlayer") && is(method.name,"changeCurrentItem","func_70453_c")) {
                InsnList code=new InsnList();code.add(new VarInsnNode(Opcodes.ILOAD,1));
                code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"hotbarScroll","(I)I",false));
                code.add(new VarInsnNode(Opcodes.ISTORE,1));method.instructions.insert(code);count++;
            }
            if (target.endsWith("InventoryEffectRenderer") && is(method.name,"updateActivePotionEffects","func_175378_g")) {
                for (AbstractInsnNode insn:method.instructions.toArray()) if(insn.getOpcode()==Opcodes.RETURN) {
                    InsnList code=new InsnList();code.add(new VarInsnNode(Opcodes.ALOAD,0));
                    code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"centerInventory","(Ljava/lang/Object;)V",false));
                    method.instructions.insertBefore(insn,code);count++;
                }
            }
            if ((target.endsWith("RenderManager") && is(method.name,"shouldRender","func_178635_a"))
                    || (target.endsWith("TileEntityRendererDispatcher") && is(method.name,"renderTileEntity","func_180546_a"))) {
                boolean entity=target.endsWith("RenderManager");
                InsnList code=new InsnList();LabelNode run=new LabelNode();code.add(new VarInsnNode(Opcodes.ALOAD,1));
                code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,entity?"hideEntity":"hideTile","(Ljava/lang/Object;)Z",false));
                code.add(new JumpInsnNode(Opcodes.IFEQ,run));if(entity)code.add(new InsnNode(Opcodes.ICONST_0));
                code.add(new InsnNode(entity?Opcodes.IRETURN:Opcodes.RETURN));code.add(run);
                method.instructions.insert(code);count++;
            }
            if (target.endsWith("RenderItem") && is(method.name,"renderEffect","func_180451_a")) {
                guard(method,"hideGlint");count++;
            }
            if (target.endsWith("RenderItem")) for(AbstractInsnNode insn:method.instructions.toArray())
                if(insn instanceof MethodInsnNode) {
                    MethodInsnNode call=(MethodInsnNode)insn;
                    if(is(call.name,"hasEffect","func_77962_s")&&call.desc.equals("()Z")) {
                        method.instructions.set(call,new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"itemGlint","(Ljava/lang/Object;)Z",false));count++;
                    }
                }
            if (target.endsWith("CustomSky") && method.name.equals("renderSky")) {
                for (AbstractInsnNode insn : method.instructions.toArray()) if (insn instanceof IntInsnNode
                        && ((IntInsnNode)insn).operand == 8 && insn.getNext() instanceof JumpInsnNode
                        && insn.getNext().getOpcode() == Opcodes.IF_ICMPLT) {
                    ((IntInsnNode)insn).operand = 0; count++;
                }
            }
            if (target.endsWith("EffectRenderer") && (is(method.name, "addEffect", "func_78873_a")
                    || method.name.equals("a") && Type.getArgumentTypes(method.desc).length == 1 && method.desc.endsWith(";)V"))) {
                guard(method, "dropParticle"); count++;
            }
            if (target.equals("net.minecraft.client.Minecraft") && (is(method.name, "getLimitFramerate", "func_90020_K")
                    || method.name.equals("j") && method.desc.equals("()I"))) {
                for (AbstractInsnNode insn : method.instructions.toArray()) if (insn.getOpcode() == Opcodes.IRETURN)
                    method.instructions.insertBefore(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "frameLimit", "(I)I", false));
                count++;
            }
            if (target.endsWith("EntityRenderer")) {
                if (is(method.name, "renderWorldPass", "func_175068_a") || method.name.equals("a") && method.desc.equals("(IFJ)V")) {
                    InsnList start = new InsnList(); start.add(new VarInsnNode(Opcodes.FLOAD, 2));
                    start.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "worldBegin", "(F)V", false));
                    method.instructions.insert(start); count++;
                    for (AbstractInsnNode insn : method.instructions.toArray()) if (insn instanceof MethodInsnNode) {
                        MethodInsnNode call = (MethodInsnNode)insn;
                        if (is(call.name, "setupCameraTransform", "func_78479_a") || call.name.equals("a") && call.desc.equals("(FI)V")) {
                            InsnList code = new InsnList(); code.add(new VarInsnNode(Opcodes.FLOAD, 2));
                            code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "cameraReady", "(F)V", false));
                            method.instructions.insert(call, code); count++;
                        }
                        if (is(call.name, "renderHand", "func_78476_b") || call.name.equals("b") && call.desc.equals("(FI)V")) {
                            InsnList code = new InsnList(); code.add(new VarInsnNode(Opcodes.FLOAD, 2));
                            code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "worldEnd", "(F)V", false));
                            method.instructions.insertBefore(call, code); count++;
                        }
                    }
                }
                if (is(method.name, "updateCameraAndRender", "func_181560_a") || method.name.equals("a") && method.desc.equals("(FJ)V")) {
                    for (AbstractInsnNode insn : method.instructions.toArray()) if (insn instanceof MethodInsnNode
                            && (is(((MethodInsnNode)insn).name, "renderWorld", "func_78471_a")
                            || ((MethodInsnNode)insn).name.equals("b") && ((MethodInsnNode)insn).desc.equals("(FJ)V"))) {
                        method.instructions.insert(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "motionBlur", "()V", false)); count++;
                    }
                }
                if (is(method.name, "updateLightmap", "func_78472_g") || method.name.equals("g") && method.desc.equals("(F)V")) {
                    // OptiFine custom lightmaps can bypass the gamma field. Apply fullbright
                    // to the owned upload buffer, covering both native and custom branches.
                    for(AbstractInsnNode insn:method.instructions.toArray()) if(insn instanceof MethodInsnNode) {
                        MethodInsnNode call=(MethodInsnNode)insn;
                        if(is(call.name,"updateDynamicTexture","func_110564_a") && call.desc.equals("()V")) {
                            InsnList hook=new InsnList();hook.add(new InsnNode(Opcodes.DUP));
                            hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"lightingTexture","(Ljava/lang/Object;)V",false));
                            method.instructions.insertBefore(call,hook);count++;
                        }
                    }
                    for (AbstractInsnNode insn : method.instructions.toArray()) if (insn instanceof FieldInsnNode
                            && (is(((FieldInsnNode)insn).name, "gammaSetting", "field_74333_Y") || ((FieldInsnNode)insn).name.equals("aJ"))) {
                        method.instructions.insert(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "gamma", "(F)F", false)); count++;
                    }
                }
            }
            if (target.endsWith("NetHandlerPlayClient") && is(method.name, "handleCustomPayload", "func_147240_a")) {
                // Native packet thread checks occur inside the original method. The bridge schedules its own safe copy.
                InsnList code = new InsnList(); code.add(new VarInsnNode(Opcodes.ALOAD, 1));
                code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "pluginMessage", "(Ljava/lang/Object;)V", false));
                method.instructions.insert(code); count++;
            }
            if (target.endsWith("NetHandlerPlayClient") && (is(method.name,"handleJoinGame","func_147282_a")
                    || is(method.name,"handleRespawn","func_147280_a"))) {
                InsnList code=new InsnList();code.add(new InsnNode(is(method.name,"handleJoinGame","func_147282_a")?Opcodes.ICONST_1:Opcodes.ICONST_0));
                code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"joinedWorld","(Z)V",false));
                method.instructions.insert(code);count++;
                if(is(method.name,"handleJoinGame","func_147282_a")) for(AbstractInsnNode insn:method.instructions.toArray())
                    if(insn.getOpcode()==Opcodes.RETURN)method.instructions.insertBefore(insn,
                            new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"registerApollo","()V",false));
            }
            if(target.endsWith("NetHandlerPlayClient")&&is(method.name,"handleKeepAlive","func_147272_a")) {
                for(AbstractInsnNode insn:method.instructions.toArray())if(insn.getOpcode()==Opcodes.RETURN)
                    method.instructions.insertBefore(insn,new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"keepAlive","()V",false));count++;
            }
            if (target.endsWith("GuiIngame") && is(method.name, "renderScoreboard", "func_180475_a")) {
                guard(method, "hideVanillaScoreboard"); count++;
            }
            if(target.endsWith("GuiNewChat")&&is(method.name,"getChatComponent","func_146236_a")) {
                InsnList code=new InsnList();LabelNode vanilla=new LabelNode();
                code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"customChatHit","()Z",false));
                code.add(new JumpInsnNode(Opcodes.IFEQ,vanilla));
                code.add(new VarInsnNode(Opcodes.ALOAD,0));code.add(new VarInsnNode(Opcodes.ILOAD,1));code.add(new VarInsnNode(Opcodes.ILOAD,2));
                code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"chatComponent","(Ljava/lang/Object;II)Lnet/minecraft/util/IChatComponent;",false));
                code.add(new InsnNode(Opcodes.ARETURN));code.add(vanilla);method.instructions.insert(code);count++;
            }
            if (target.endsWith("GuiNewChat") && is(method.name,"setChatLine","func_146237_a")) {
                InsnList code = new InsnList(); LabelNode nativePath = new LabelNode();
                code.add(new VarInsnNode(Opcodes.ALOAD,0));code.add(new VarInsnNode(Opcodes.ALOAD,1));
                code.add(new VarInsnNode(Opcodes.ILOAD,2));code.add(new VarInsnNode(Opcodes.ILOAD,3));code.add(new VarInsnNode(Opcodes.ILOAD,4));
                code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"com/cheatbreaker/client/kazerui/module/modules/mechanic/ChatRuntime","insert","(Ljava/lang/Object;Lnet/minecraft/util/IChatComponent;IIZ)Z",false));
                code.add(new JumpInsnNode(Opcodes.IFEQ,nativePath));code.add(new InsnNode(Opcodes.RETURN));code.add(nativePath);
                method.instructions.insert(code);count++;
            }
            if (target.endsWith("GuiNewChat") && is(method.name,"refreshChat","func_146245_b")) {
                InsnList code = new InsnList(); LabelNode nativePath = new LabelNode();code.add(new VarInsnNode(Opcodes.ALOAD,0));
                code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"com/cheatbreaker/client/kazerui/module/modules/mechanic/ChatRuntime","refresh","(Ljava/lang/Object;)Z",false));
                code.add(new JumpInsnNode(Opcodes.IFEQ,nativePath));code.add(new InsnNode(Opcodes.RETURN));code.add(nativePath);
                method.instructions.insert(code);count++;
            }
            if (target.endsWith("GuiNewChat") && is(method.name, "drawChat", "func_146230_a")) {
                InsnList owned = new InsnList(); LabelNode nativePath = new LabelNode();
                owned.add(new VarInsnNode(Opcodes.ALOAD,0));owned.add(new VarInsnNode(Opcodes.ILOAD,1));
                owned.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"com/cheatbreaker/client/kazerui/module/modules/mechanic/ChatRuntime","draw","(Ljava/lang/Object;I)Z",false));
                owned.add(new JumpInsnNode(Opcodes.IFEQ,nativePath));owned.add(new InsnNode(Opcodes.RETURN));owned.add(nativePath);
                method.instructions.insert(owned);
                InsnList code = new InsnList(); LabelNode render = new LabelNode();
                code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "beginChat", "()Z", false));
                code.add(new JumpInsnNode(Opcodes.IFNE, render)); code.add(new InsnNode(Opcodes.RETURN)); code.add(render);
                method.instructions.insert(code);
                for (AbstractInsnNode insn : method.instructions.toArray()) if (insn.getOpcode() == Opcodes.RETURN && insn != code.getFirst()) {
                    // The guard return precedes 'render'; only wrap actual original exits.
                    boolean after = false;
                    for (AbstractInsnNode p = render.getNext(); p != null; p = p.getNext()) if (p == insn) { after = true; break; }
                    if (after) method.instructions.insertBefore(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "endChat", "()V", false));
                }
                for (AbstractInsnNode insn : method.instructions.toArray()) if (insn instanceof MethodInsnNode) {
                    MethodInsnNode call = (MethodInsnNode)insn;
                    if (is(call.name, "drawRect", "func_73734_a") && call.desc.equals("(IIIII)V"))
                        method.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "chatRect", "(IIIII)V", false));
                }
                count++;
            }
        }
        type.methods.addAll(skyWrappers);
        if (count == 0) return bytes;
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS); type.accept(writer);
        System.out.println("[KC 1.8.9] Runtime adapters: " + target + " (" + count + ")");
        return writer.toByteArray();
    }
    private static void addChatGetter(ClassNode type,String methodName,String mcp,String srg,String descriptor,int returnOpcode) {
        FieldNode nativeField=null;
        for (FieldNode field:type.fields) if(is(field.name,mcp,srg) && field.desc.equals(descriptor)) nativeField=field;
        if(nativeField==null)throw new IllegalStateException("Native chat field was not found: "+type.name+"."+srg);
        MethodNode getter=new MethodNode(Opcodes.ACC_PUBLIC,methodName,"()"+descriptor,null,null);
        getter.instructions.add(new VarInsnNode(Opcodes.ALOAD,0));
        getter.instructions.add(new FieldInsnNode(Opcodes.GETFIELD,type.name,nativeField.name,descriptor));
        getter.instructions.add(new InsnNode(returnOpcode));type.methods.add(getter);
    }
    private static boolean is(String value, String mcp, String srg) { return mcp.equals(value) || srg.equals(value); }
    private static MethodNode customSkyWrapper(MethodInsnNode original) {
        MethodNode method=new MethodNode(Opcodes.ACC_PRIVATE|Opcodes.ACC_STATIC,"kazerCustomSky",original.desc,null,null);
        LabelNode begin=new LabelNode(),end=new LabelNode(),handler=new LabelNode();
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"customSkyBegin","()V",false));
        method.instructions.add(begin);method.instructions.add(new VarInsnNode(Opcodes.ALOAD,0));method.instructions.add(new VarInsnNode(Opcodes.ALOAD,1));method.instructions.add(new VarInsnNode(Opcodes.FLOAD,2));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,original.owner,original.name,original.desc,false));
        method.instructions.add(end);method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"customSkyEnd","()V",false));method.instructions.add(new InsnNode(Opcodes.RETURN));
        method.instructions.add(handler);
        method.instructions.add(new FrameNode(Opcodes.F_FULL,3,new Object[]{"net/minecraft/world/World","net/minecraft/client/renderer/texture/TextureManager",Opcodes.FLOAT},1,new Object[]{"java/lang/Throwable"}));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"customSkyEnd","()V",false));method.instructions.add(new InsnNode(Opcodes.ATHROW));
        method.tryCatchBlocks.add(new TryCatchBlockNode(begin,end,handler,null));method.maxStack=3;method.maxLocals=3;
        return method;
    }
    private static AbstractInsnNode nextOpcode(AbstractInsnNode node) {
        if(node==null)return null;
        do {node=node.getNext();}while(node!=null && node.getOpcode()<0);
        return node;
    }
    private static void guard(MethodNode method, String hook) {
        guardOwner(method,HOOK,hook);
    }
    private static void guardOwner(MethodNode method,String owner,String hook) {
        InsnList code = new InsnList(); LabelNode run = new LabelNode();
        code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, owner, hook, "()Z", false));
        code.add(new JumpInsnNode(Opcodes.IFEQ, run)); code.add(new InsnNode(Opcodes.RETURN)); code.add(run);
        method.instructions.insert(code);
    }
}
