/* SPDX-License-Identifier: LGPL-3.0-only
 * Hook locations adapted from Sk1erLLC/OldAnimations mixins at
 * ab10fa4879bf54a899bba777d7a0bb70c946e4fb. Modifications: ASM/Forge bridge, no Essential dependency.
 * See META-INF/licenses/OldAnimations-NOTICE.md.
 */
package cn.kazer.client189.animation;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

public final class BlockHitTransformer implements IClassTransformer {
    private static final String BRIDGE="cn/kazer/client189/animation/OldAnimationsAdapter";
    private static final String STACK="Lnet/minecraft/item/ItemStack;";
    private static final String TRANSFORM="Lnet/minecraft/client/renderer/block/model/ItemCameraTransforms$TransformType;";
    @Override public byte[] transform(String name,String target,byte[] bytes) {
        if(bytes==null || !(target.equals("net.minecraft.client.renderer.ItemRenderer")
                ||target.equals("net.minecraft.client.Minecraft")||target.equals("net.minecraft.client.model.ModelBiped")
                ||target.equals("net.minecraft.client.renderer.entity.RenderItem")
                ||target.equals("net.minecraft.client.renderer.entity.layers.LayerArmorBase")
                ||target.equals("net.minecraft.client.renderer.EntityRenderer")
                ||target.equals("net.minecraftforge.client.GuiIngameForge"))) return bytes;
        ClassNode type=new ClassNode();new ClassReader(bytes).accept(type,0);int hooks=0;
        if(target.endsWith("RenderItem")) type.fields.add(new FieldNode(Opcodes.ACC_PRIVATE,"kazer$lastEntity","Lnet/minecraft/entity/EntityLivingBase;",null,null));
        for(MethodNode m:type.methods) {
            if(target.endsWith("Minecraft") && is(m,"rightClickMouse","func_147121_ag")) { m.instructions.insert(call("beforeRightClick","()V"));hooks++; }
            if(target.endsWith("Minecraft") && (is(m,"clickMouse","func_147116_af")||is(m,"sendClickBlockToController","func_147115_a"))) {
                guard(m,"cancelBlockClick",Opcodes.RETURN);hooks++;
            }
            if(target.endsWith("LayerArmorBase") && is(m,"shouldCombineTextures","func_177142_b")) { guard(m,"redArmor",Opcodes.IRETURN);hooks++; }
            if(target.endsWith("RenderItem") && is(m,"renderItemModelForEntity","func_175049_a")) {
                InsnList c=new InsnList();c.add(new VarInsnNode(Opcodes.ALOAD,0));c.add(new VarInsnNode(Opcodes.ALOAD,2));
                c.add(new FieldInsnNode(Opcodes.PUTFIELD,type.name,"kazer$lastEntity","Lnet/minecraft/entity/EntityLivingBase;"));m.instructions.insert(c);hooks++;
            }
            if(target.endsWith("EntityRenderer")&&is(m,"orientCamera","func_78467_g")) {
                InsnList c=new InsnList();c.add(new VarInsnNode(Opcodes.FLOAD,1));c.add(call("cameraPartial","(F)V"));m.instructions.insert(c);hooks++;
            }
            int transforms=0,healthLoads=0;
            for(AbstractInsnNode node:m.instructions.toArray()) {
                if(target.endsWith("ModelBiped") && is(m,"setRotationAngles","func_78087_a") && node instanceof LdcInsnNode
                        && Float.valueOf(-.5235988F).equals(((LdcInsnNode)node).cst)) {
                    m.instructions.insert(node,call("blockingArmYaw","(F)F"));hooks++;
                }
                if(target.endsWith("GuiIngameForge") && m.name.equals("renderHealth") && node instanceof VarInsnNode
                        && node.getOpcode()==Opcodes.ILOAD && ((VarInsnNode)node).var==5 && ++healthLoads==2) {
                    // Match upstream ModifyVariable at LOAD ordinal 1: preserve the first background read.
                    InsnList c=new InsnList();c.add(new VarInsnNode(Opcodes.ILOAD,5));c.add(call("healthFlash","(Z)Z"));
                    c.add(new VarInsnNode(Opcodes.ISTORE,5));m.instructions.insertBefore(node,c);hooks++;
                }
                if(!(node instanceof MethodInsnNode))continue;
                MethodInsnNode inv=(MethodInsnNode)node;
                if(target.endsWith("ItemRenderer")) {
                    if(is(m,"renderItemInFirstPerson","func_78440_a")) {
                        if(is(inv,"transformFirstPersonItem","func_178096_b")) {
                            ++transforms;
                            if(transforms==1) {
                                AbstractInsnNode start=previous(previous(previous(inv)));LabelNode end=new LabelNode();
                                InsnList c=new InsnList();c.add(call("skipNoneTransform","()Z"));c.add(new JumpInsnNode(Opcodes.IFNE,end));
                                m.instructions.insertBefore(start,c);m.instructions.insert(inv,end);hooks++;
                            } else if(transforms<=4) {
                                InsnList c=new InsnList();c.add(new VarInsnNode(Opcodes.FLOAD,1));c.add(call("useSwing","(FF)F"));
                                m.instructions.insertBefore(inv,c);hooks++;
                            }
                        } else if(is(inv,"getItemInUseCount","func_71052_bv")) {
                            InsnList c=new InsnList();c.add(new VarInsnNode(Opcodes.ALOAD,0));c.add(field(type,"itemToRender","field_78453_b",STACK));
                            m.instructions.insertBefore(inv,c);m.instructions.set(inv,call("renderedUseCount","(Ljava/lang/Object;"+STACK+")I"));hooks++;
                        } else if(is(inv,"renderItem","func_178099_a")) {
                            m.instructions.set(inv,call("renderFirstPerson","(Ljava/lang/Object;Lnet/minecraft/entity/EntityLivingBase;"+STACK+TRANSFORM+")V"));hooks++;
                        }
                    } else if(is(m,"doBowTransformations","func_178098_a")&&is(inv,"scale","func_179152_a")&&inv.desc.equals("(FFF)V")) {
                        m.instructions.set(inv,call("bowScale","(FFF)V"));hooks++;
                    } else if(is(m,"updateEquippedItem","func_78441_a")&&is(inv,"clamp_float","func_76131_a")) {
                        InsnList c=new InsnList();c.add(new VarInsnNode(Opcodes.ALOAD,0));c.add(field(type,"itemToRender","field_78453_b",STACK));
                        c.add(new VarInsnNode(Opcodes.ALOAD,0));c.add(field(type,"equippedItemSlot","field_78450_g","I"));
                        c.add(new VarInsnNode(Opcodes.ALOAD,0));c.add(field(type,"equippedProgress","field_78454_c","F"));
                        m.instructions.insertBefore(inv,c);m.instructions.set(inv,call("equipClamp","(FFF"+STACK+"IF)F"));hooks++;
                    }
                }
                if(target.endsWith("RenderItem")&&is(m,"renderItemModelTransform","func_175040_a")
                        &&is(inv,"renderItem","func_180454_a")&&inv.desc.equals("("+STACK+"Lnet/minecraft/client/resources/model/IBakedModel;)V")) {
                    InsnList c=new InsnList();c.add(new VarInsnNode(Opcodes.ALOAD,0));c.add(new FieldInsnNode(Opcodes.GETFIELD,type.name,"kazer$lastEntity","Lnet/minecraft/entity/EntityLivingBase;"));
                    c.add(new VarInsnNode(Opcodes.ALOAD,3));c.add(call("thirdPersonPose","(Ljava/lang/Object;"+TRANSFORM+")V"));m.instructions.insertBefore(inv,c);hooks++;
                }
                if(target.endsWith("EntityRenderer")&&is(inv,"getEyeHeight","func_70047_e")) {
                    if(is(m,"orientCamera","func_78467_g")) { m.instructions.insertBefore(inv,new VarInsnNode(Opcodes.FLOAD,1));m.instructions.set(inv,call("cameraEye","(Ljava/lang/Object;F)F"));hooks++; }
                    else if(is(m,"renderWorldDirections","func_175067_i")) { m.instructions.set(inv,call("directionEye","(Ljava/lang/Object;)F"));hooks++; }
                }
            }
        }
        if(hooks==0)return bytes;
        ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_MAXS);type.accept(writer);
        System.out.println("[KC 1.8.9] Sk1er OldAnimations hooks: "+target+" ("+hooks+")");return writer.toByteArray();
    }
    private static MethodInsnNode call(String name,String desc) { return new MethodInsnNode(Opcodes.INVOKESTATIC,BRIDGE,name,desc,false); }
    private static boolean is(MethodNode m,String mcp,String srg) { return m.name.equals(mcp)||m.name.equals(srg); }
    private static boolean is(MethodInsnNode m,String mcp,String srg) { return m.name.equals(mcp)||m.name.equals(srg); }
    private static AbstractInsnNode previous(AbstractInsnNode node) { do {node=node.getPrevious();}while(node!=null&&node.getOpcode()<0);return node; }
    private static FieldInsnNode field(ClassNode type,String mcp,String srg,String desc) {
        for(FieldNode f:type.fields)if(f.name.equals(mcp)||f.name.equals(srg))return new FieldInsnNode(Opcodes.GETFIELD,type.name,f.name,desc);
        throw new IllegalStateException("Missing OldAnimations field: "+type.name+"."+mcp);
    }
    private static void guard(MethodNode m,String hook,int opcode) {
        InsnList c=new InsnList();LabelNode nativeCode=new LabelNode();c.add(call(hook,"()Z"));c.add(new JumpInsnNode(Opcodes.IFEQ,nativeCode));
        if(opcode==Opcodes.IRETURN)c.add(new InsnNode(Opcodes.ICONST_1));c.add(new InsnNode(opcode));c.add(nativeCode);m.instructions.insert(c);
    }
}
