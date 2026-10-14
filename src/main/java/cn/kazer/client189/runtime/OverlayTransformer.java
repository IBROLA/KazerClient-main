package cn.kazer.client189.runtime;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Render adapters only; validated target signatures and no game-state writes. */
public final class OverlayTransformer implements IClassTransformer {
    private static final String MODULE="com/cheatbreaker/client/kazerui/module/modules/visual/ModuleOverlay",HOOK="cn/kazer/client189/runtime/OverlayHooks";
    public byte[] transform(String name,String target,byte[] bytes) {
        if(bytes==null||target==null)return bytes;
        if(!(target.equals("net.minecraft.client.renderer.entity.Render")||target.equals("net.minecraft.client.renderer.entity.RenderArrow")
            ||target.equals("net.minecraft.entity.projectile.EntityArrow")||target.equals("net.minecraft.client.renderer.entity.layers.LayerArrow")
            ||target.equals("net.minecraft.client.renderer.entity.layers.LayerArmorBase")||target.equals("net.minecraft.client.renderer.entity.layers.LayerCustomHead")||target.equals("net.minecraft.client.renderer.entity.RendererLivingEntity")
            ||target.equals("net.minecraft.client.renderer.tileentity.TileEntitySkullRenderer")||target.equals("net.minecraft.client.renderer.tileentity.TileEntityEndPortalRenderer")
            ||target.equals("net.minecraft.client.gui.GuiIngame")||target.equals("net.minecraftforge.client.GuiIngameForge")
            ||target.equals("net.minecraft.client.renderer.EntityRenderer")||target.equals("net.minecraft.client.renderer.ItemRenderer")
            ||target.equals("net.minecraft.client.model.ModelHorse")||target.equals("net.minecraft.client.resources.ResourcePackListEntry")
            ||target.equals("net.minecraft.client.renderer.BlockRendererDispatcher")
            ||target.equals("net.minecraft.client.renderer.texture.SimpleTexture")||target.equals("net.minecraft.client.renderer.entity.RenderFish")
            ||target.equals("net.minecraft.block.BlockBarrier")))return bytes;
        ClassNode type=new ClassNode();new ClassReader(bytes).accept(type,0);int count=0;
        for(MethodNode method:type.methods)for(AbstractInsnNode insn:method.instructions.toArray())if(insn instanceof MethodInsnNode) {
            String owner=((MethodInsnNode)insn).owner;
            if(owner.equals(MODULE)||owner.equals(HOOK)||owner.equals("cn/kazer/client189/runtime/OverlayModelCache")
                ||owner.equals("com/cheatbreaker/client/kazerui/module/modules/visual/OverlayTexturePalette"))return bytes;
        }
        if(target.endsWith("EntityArrow")) {
            FieldNode ground=null;for(FieldNode field:type.fields)if(is(field.name,"inGround","field_70254_i")&&field.desc.equals("Z"))ground=field;
            if(ground!=null&&!type.interfaces.contains("cn/kazer/client189/runtime/ArrowGroundState")) {
                type.interfaces.add("cn/kazer/client189/runtime/ArrowGroundState");
                MethodNode getter=new MethodNode(Opcodes.ACC_PUBLIC,"kazerInGround","()Z",null,null);
                getter.instructions.add(new VarInsnNode(Opcodes.ALOAD,0));getter.instructions.add(new FieldInsnNode(Opcodes.GETFIELD,type.name,ground.name,"Z"));
                getter.instructions.add(new InsnNode(Opcodes.IRETURN));type.methods.add(getter);count++;
            }
        }
        for(MethodNode method:type.methods) {
            if(target.endsWith("LayerCustomHead")&&is(method.name,"doRenderLayer","func_177141_a")&&method.desc.startsWith("(Lnet/minecraft/entity/EntityLivingBase;"))
                for(AbstractInsnNode insn:method.instructions.toArray())if(insn instanceof MethodInsnNode) {
                    MethodInsnNode call=(MethodInsnNode)insn;
                    if(is(call.name,"getCurrentArmor","func_82169_q")&&call.desc.equals("(I)Lnet/minecraft/item/ItemStack;")) {
                        InsnList code=new InsnList();code.add(new VarInsnNode(Opcodes.ALOAD,1));
                        code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"headArmor","(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/EntityLivingBase;)Lnet/minecraft/item/ItemStack;",false));method.instructions.insert(call,code);count++;
                    }
                }
            if(target.endsWith("BlockBarrier")&&is(method.name,"getRenderType","func_149645_b")&&method.desc.equals("()I"))
                for(AbstractInsnNode insn:method.instructions.toArray())if(insn.getOpcode()==Opcodes.IRETURN){method.instructions.insertBefore(insn,new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"barrierType","(I)I",false));count++;}
            if(target.endsWith("BlockRendererDispatcher")&&is(method.name,"renderBlock","func_175018_a")&&method.desc.equals("(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/BlockPos;Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/client/renderer/WorldRenderer;)Z")) {
                InsnList code=new InsnList();LabelNode run=new LabelNode();for(int i=1;i<=4;i++)code.add(new VarInsnNode(Opcodes.ALOAD,i));
                code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"cn/kazer/client189/runtime/OverlayModelCache","renderBarrier",method.desc,false));
                code.add(new JumpInsnNode(Opcodes.IFEQ,run));code.add(new InsnNode(Opcodes.ICONST_1));code.add(new InsnNode(Opcodes.IRETURN));code.add(run);method.instructions.insert(code);count++;
            }
            if(target.endsWith("BlockRendererDispatcher")&&method.desc.startsWith("(Lnet/minecraft/block/state/IBlockState;")&&method.desc.endsWith(")Lnet/minecraft/client/resources/model/IBakedModel;"))
                for(AbstractInsnNode insn:method.instructions.toArray())if(insn.getOpcode()==Opcodes.ARETURN) {
                    InsnList code=new InsnList();code.add(new VarInsnNode(Opcodes.ALOAD,1));
                    code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"cn/kazer/client189/runtime/OverlayModelCache","adjust","(Lnet/minecraft/client/resources/model/IBakedModel;Lnet/minecraft/block/state/IBlockState;)Lnet/minecraft/client/resources/model/IBakedModel;",false));method.instructions.insertBefore(insn,code);count++;
                }
            if(target.endsWith(".Render")&&is(method.name,"renderShadow","func_76975_c")&&method.desc.endsWith("FF)V")){guard(method,HOOK,"skipShadow");count++;}
            if(target.endsWith(".Render")&&is(method.name,"renderEntityOnFire","func_76977_a")&&method.desc.endsWith("F)V")){guard(method,MODULE,"fire");count++;}
            if(target.endsWith("LayerArrow")&&is(method.name,"doRenderLayer","func_177141_a")&&method.desc.startsWith("(Lnet/minecraft/entity/EntityLivingBase;")){guard(method,HOOK,"skipArrows");count++;}
            if(target.endsWith("TileEntitySkullRenderer")&&is(method.name,"renderTileEntityAt","func_180535_a")&&method.desc.equals("(Lnet/minecraft/tileentity/TileEntitySkull;DDDFI)V")){guard(method,MODULE,"hideSkulls");count++;}
            if(target.endsWith("TileEntityEndPortalRenderer")&&is(method.name,"renderTileEntityAt","func_180535_a")&&method.desc.equals("(Lnet/minecraft/tileentity/TileEntityEndPortal;DDDFI)V")){guard(method,MODULE,"hidePortals");count++;}
            if(target.endsWith("LayerArmorBase")&&is(method.name,"renderGlint","func_177183_a")&&method.desc.endsWith("FFFFFFF)V")){guard(method,HOOK,"skipArmorGlint");count++;}
            if(target.endsWith("RenderArrow")&&is(method.name,"doRender","func_76986_a")&&method.desc.startsWith("(Lnet/minecraft/entity/projectile/EntityArrow;")) {
                InsnList code=new InsnList();LabelNode run=new LabelNode();code.add(new VarInsnNode(Opcodes.ALOAD,1));
                code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"skipGroundArrow","(Lnet/minecraft/entity/Entity;)Z",false));
                code.add(new JumpInsnNode(Opcodes.IFEQ,run));code.add(new InsnNode(Opcodes.RETURN));code.add(run);method.instructions.insert(code);count++;
            }
            if(target.endsWith("LayerArmorBase")&&is(method.name,"getCurrentArmor","func_177176_a")&&method.desc.equals("(Lnet/minecraft/entity/EntityLivingBase;I)Lnet/minecraft/item/ItemStack;"))
                for(AbstractInsnNode insn:method.instructions.toArray())if(insn.getOpcode()==Opcodes.ARETURN) {
                    InsnList code=new InsnList();code.add(new VarInsnNode(Opcodes.ALOAD,1));code.add(new VarInsnNode(Opcodes.ILOAD,2));
                    code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"armor","(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/EntityLivingBase;I)Lnet/minecraft/item/ItemStack;",false));method.instructions.insertBefore(insn,code);count++;
                }
            boolean pumpkin=is(method.name,"renderPumpkinOverlay","func_180476_e");
            if(pumpkin&&method.desc.equals("(Lnet/minecraft/client/gui/ScaledResolution;)V")){guard(method,HOOK,"skipPumpkin");count++;}
            for(AbstractInsnNode insn:method.instructions.toArray()) {
                if((target.endsWith("GuiIngame")||target.endsWith("GuiIngameForge"))&&insn instanceof LdcInsnNode&&Integer.valueOf(8453920).equals(((LdcInsnNode)insn).cst)) {
                    method.instructions.insert(insn,new MethodInsnNode(Opcodes.INVOKESTATIC,MODULE,"xpColor","(I)I",false));count++;
                }
                if(insn instanceof FieldInsnNode&&insn.getOpcode()==Opcodes.GETFIELD&&target.endsWith("RendererLivingEntity")) {
                    FieldInsnNode field=(FieldInsnNode)insn;String hook=is(field.name,"deathTime","field_70725_aQ")?
                        (is(method.name,"rotateCorpse","func_77043_a")?"renderDeathTime":"renderDamageDeathTime"):is(field.name,"hurtTime","field_70737_aN")?"renderHurtTime":null;
                    if(hook!=null&&field.desc.equals("I")&&field.owner.equals("net/minecraft/entity/EntityLivingBase")){method.instructions.set(field,new MethodInsnNode(Opcodes.INVOKESTATIC,MODULE,hook,"(Lnet/minecraft/entity/EntityLivingBase;)I",false));count++;}
                }
                if(!(insn instanceof MethodInsnNode))continue;MethodInsnNode call=(MethodInsnNode)insn;
                if(target.endsWith("RenderFish")&&is(method.name,"doRender","func_76986_a")&&call.owner.equals("net/minecraft/client/renderer/WorldRenderer")&&is(call.name,"color","func_181669_b")&&call.desc.equals("(IIII)Lnet/minecraft/client/renderer/WorldRenderer;")) {
                    method.instructions.set(call,new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"lineColor","(Lnet/minecraft/client/renderer/WorldRenderer;IIII)Lnet/minecraft/client/renderer/WorldRenderer;",false));count++;
                }
                if(target.endsWith("SimpleTexture")&&is(method.name,"loadTexture","func_110551_a")&&call.desc.equals("(Ljava/io/InputStream;)Ljava/awt/image/BufferedImage;")) {
                    FieldNode location=null;for(FieldNode field:type.fields)if(is(field.name,"textureLocation","field_110568_b")&&field.desc.equals("Lnet/minecraft/util/ResourceLocation;"))location=field;
                    if(location!=null) {
                        InsnList code=new InsnList();code.add(new VarInsnNode(Opcodes.ALOAD,0));code.add(new FieldInsnNode(Opcodes.GETFIELD,type.name,location.name,location.desc));
                        code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"com/cheatbreaker/client/kazerui/module/modules/visual/OverlayTexturePalette","tint","(Ljava/awt/image/BufferedImage;Lnet/minecraft/util/ResourceLocation;)Ljava/awt/image/BufferedImage;",false));
                        method.instructions.insert(call,code);count++;
                    }
                }
                if(target.endsWith("ResourcePackListEntry")&&is(method.name,"drawEntry","func_148279_a")&&call.owner.equals("net/minecraft/client/gui/Gui")&&is(call.name,"drawModalRectWithCustomSizedTexture","func_146110_a")&&call.desc.equals("(IIFFIIFF)V")) {
                    method.instructions.set(call,new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"packIcon",call.desc,false));count++;
                }
                if(target.endsWith("ModelHorse")&&is(method.name,"render","func_78088_a")&&call.owner.equals("net/minecraft/client/model/ModelRenderer")&&is(call.name,"render","func_78785_a")&&call.desc.equals("(F)V")) {
                    AbstractInsnNode field=call.getPrevious();while(field!=null&&!(field instanceof FieldInsnNode))field=field.getPrevious();
                    if(field instanceof FieldInsnNode&&horseHead(((FieldInsnNode)field).name)) {
                        method.instructions.set(call,new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"horseHead","(Lnet/minecraft/client/model/ModelRenderer;F)V",false));count++;
                    }
                }
                if(target.endsWith("EntityRenderer")&&is(method.name,"setupViewBobbing","func_78475_f")&&call.owner.equals("net/minecraft/client/renderer/GlStateManager")) {
                    String hook=call.desc.equals("(FFF)V")&&is(call.name,"translate","func_179109_b")?"bobTranslate":call.desc.equals("(FFFF)V")&&is(call.name,"rotate","func_179114_b")?"bobRotate":null;
                    if(hook!=null){method.instructions.set(call,new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,hook,call.desc,false));count++;}
                }
                if(target.endsWith("ItemRenderer")&&is(method.name,"renderFireInFirstPerson","func_78442_d")&&call.owner.equals("net/minecraft/client/renderer/GlStateManager")&&is(call.name,"translate","func_179109_b")&&call.desc.equals("(FFF)V")) {
                    method.instructions.set(call,new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"fireTranslate",call.desc,false));count++;
                }
                if(pumpkin&&call.owner.equals("net/minecraft/client/renderer/GlStateManager")&&is(call.name,"color","func_179131_c")&&call.desc.equals("(FFFF)V")) {
                    AbstractInsnNode next=call.getNext();while(next!=null&&next.getOpcode()<0)next=next.getNext();
                    if(next!=null&&next.getOpcode()!=Opcodes.RETURN){method.instructions.set(call,new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"pumpkinColor",call.desc,false));count++;}
                }
            }
        }
        if(count==0)return bytes;
        ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_MAXS|ClassWriter.COMPUTE_FRAMES);type.accept(writer);
        System.out.println("[Kazer Overlay] Adapted "+target+" ("+count+")");return writer.toByteArray();
    }
    private static boolean is(String value,String mcp,String srg){return value.equals(mcp)||value.equals(srg);}
    private static boolean horseHead(String field) {
        switch(field) {
            case "head":case "neck":case "mane":case "horseFaceRopes":case "horseLeftFaceMetal":case "horseRightFaceMetal":
            case "horseLeftRein":case "horseRightRein":case "muleLeftEar":case "muleRightEar":case "horseLeftEar":case "horseRightEar":
            case "field_110709_a":case "field_110716_h":case "field_110714_j":case "field_110717_i":case "field_110700_P":case "field_110699_Q":
            case "field_110702_R":case "field_110701_S":case "field_110703_f":case "field_110704_g":case "field_110705_d":case "field_110706_e":return true;
            default:return false;
        }
    }
    private static void guard(MethodNode method,String owner,String name) {
        InsnList code=new InsnList();LabelNode run=new LabelNode();code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,owner,name,"()Z",false));
        code.add(new JumpInsnNode(Opcodes.IFEQ,run));code.add(new InsnNode(Opcodes.RETURN));code.add(run);method.instructions.insert(code);
    }
}
