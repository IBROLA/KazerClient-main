package com.cheatbreaker.client.kazerui.cosmetics.render;

import com.cheatbreaker.client.kazerui.cosmetics.Cosmetic;
import com.cheatbreaker.client.kazerui.cosmetics.CosmeticTextures;
import com.cheatbreaker.client.kazerui.cosmetics.CosmeticType;
import com.cheatbreaker.client.kazerui.cosmetics.KazerCdn;
import com.cheatbreaker.client.kazerui.cosmetics.gecko.GeoAnimation;
import com.cheatbreaker.client.kazerui.cosmetics.gecko.GeoModel;
import com.cheatbreaker.client.kazerui.cosmetics.gecko.Molang;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;

public final class GeckoCosmetics {
    private static final ThreadPoolExecutor LOAD = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
        new ArrayBlockingQueue<Runnable>(8), r -> { Thread t = new Thread(r, "Kazer gecko load"); t.setDaemon(true); return t; },
        new ThreadPoolExecutor.AbortPolicy());
    private static final Map<String, Future<Definition>> DEFINITIONS = new LinkedHashMap<String, Future<Definition>>(32,.75f,true);
    private static final Map<String, Long> RETRY = new LinkedHashMap<String, Long>(32,.75f,true);
    private static final Map<EntityPlayer, Map<Integer, State>> STATES = new WeakHashMap<EntityPlayer, Map<Integer, State>>();
    private static final Map<EntityPlayer, Map<Integer, State>> GUI_STATES = new WeakHashMap<EntityPlayer, Map<Integer, State>>();
    private static final long PREVIEW_START = System.nanoTime();
    private static Molang.Expr constants;
    private static Molang.Expr petConstants;
    private static boolean molangLoaded;

    private GeckoCosmetics() {}

    static final class Definition {
        final List<GeoModel> models = new ArrayList<GeoModel>();
        final List<Molang.Expr> modelConditions = new ArrayList<Molang.Expr>();
        Map<String, GeoAnimation> animations;
        String texture, type, attached = "NONE";
        final List<ObjCosmetics.Op> ops = new ArrayList<ObjCosmetics.Op>();

        boolean coversHead, coversBody, coversRightArm, coversLeftArm, coversRightLeg, coversLeftLeg;
        boolean offsetWithChestplate, hideHead, hideBody, hideRightArm, hideLeftArm, hideRightLeg, hideLeftLeg;

        final List<List<String[]>> controllers = new ArrayList<List<String[]>>();
        final List<List<Molang.Expr>> controllerConditions = new ArrayList<List<Molang.Expr>>();
    }

    static final class State {
        final Molang.Scope scope = new Molang.Scope();
        final String[] playing; final double[] started;
        final java.lang.ref.WeakReference<EntityPlayer> wearer;
        float partialTicks;
        final PetMotionSample motion;
        final CosmeticAnimationClock clock;
        final boolean gui;
        State(int controllers, EntityPlayer player,boolean smoothPet,boolean gui) {
            this.gui=gui;clock=smoothPet?new CosmeticAnimationClock():null;
            motion=smoothPet?new PetMotionSample():null;
            playing = new String[controllers]; started = new double[controllers];
            wearer = new java.lang.ref.WeakReference<EntityPlayer>(player);
            scope.queries = name -> { EntityPlayer p = wearer.get(); return p == null ? 0 : "life_time".equals(name)&&clock!=null?clock.time():query(name,p,partialTicks,motion,gui); };
        }
    }

    public static boolean has(Cosmetic c) { return c.geckolib && KazerCdn.has(c.resource); }

    private static String strip(String path) { return path.startsWith("lunar:") ? path.substring(6) : path; }

    static Definition definition(final Cosmetic c) {
        Future<Definition> f;
        synchronized (DEFINITIONS) {
            Long retry=RETRY.get(c.resource);
            if(retry!=null) {if(System.nanoTime()<retry)return null;RETRY.remove(c.resource);}
            f = DEFINITIONS.get(c.resource);
            if (f == null) {
                if (DEFINITIONS.size() >= 32) {
                    Iterator<Future<Definition>> entries = DEFINITIONS.values().iterator();
                    while (entries.hasNext()) {
                        Future<Definition> old = entries.next();
                        if (!old.isDone()) continue;
                        try { Definition d = old.get(); if (d != null) for (GeoModel model : d.models) model.delete(); }
                        catch (Exception ignored) { }
                        entries.remove(); break;
                    }
                    if (DEFINITIONS.size() >= 32) return null;
                }
                try { f = LOAD.submit(() -> load(c.resource)); }
                catch (RejectedExecutionException busy) { return null; }
                DEFINITIONS.put(c.resource, f);
            }
        }
        if (!f.isDone()) return null;
        try { Definition definition=f.get();if(definition!=null)return definition; } catch (Exception ignored) { }
        synchronized(DEFINITIONS) {
            if(DEFINITIONS.get(c.resource)==f)DEFINITIONS.remove(c.resource);
            if(RETRY.size()>=32)RETRY.remove(RETRY.keySet().iterator().next());
            RETRY.put(c.resource,System.nanoTime()+30_000_000_000L);
        }
        return null;
    }

    private static Definition load(String path) {
        try {
            JsonObject gek = json(KazerCdn.fetch(path).get());
            if (gek == null) return null;
            Definition d = new Definition();
            d.type = gek.has("type") ? gek.get("type").getAsString() : "";
            d.texture = strip(gek.get("texture").getAsString());
            if (gek.has("attached_bone")) d.attached = gek.get("attached_bone").getAsString();
            Future<byte[]> anim = gek.has("animation") ? KazerCdn.fetch(strip(gek.get("animation").getAsString())) : null;
            JsonElement model = gek.get("model");
            if (model.isJsonObject()) {
                for (Map.Entry<String, JsonElement> e : model.getAsJsonObject().entrySet()) {
                    d.models.add(GeoModel.parse(json(KazerCdn.fetch(strip(e.getKey())).get())));
                    d.modelConditions.add(Molang.compile(e.getValue().getAsString()));
                }
            } else {
                d.models.add(GeoModel.parse(json(KazerCdn.fetch(strip(model.getAsString())).get())));
                d.modelConditions.add(Molang.constant(1));
            }
            JsonObject anims = anim == null ? null : json(anim.get());
            d.animations = anims == null ? Collections.<String, GeoAnimation>emptyMap() : GeoAnimation.parse(anims);
            if (gek.has("transformations") && gek.get("transformations").isJsonArray())
                for (JsonElement t : gek.getAsJsonArray("transformations"))
                    if (t.isJsonObject() && t.getAsJsonObject().has("transformType")) ObjCosmetics.parse(d.ops, t.getAsJsonObject());
            d.offsetWithChestplate = flag(gek, "offset_with_chestplate");
            d.hideHead = flag(gek, "hide_head"); d.hideBody = flag(gek, "hide_body");
            d.hideRightArm = flag(gek, "hide_right_arm"); d.hideLeftArm = flag(gek, "hide_left_arm");
            d.hideRightLeg = flag(gek, "hide_right_leg"); d.hideLeftLeg = flag(gek, "hide_left_leg");
            stateMachine(d, gek);
            for (List<String[]> controller : d.controllers) {
                List<Molang.Expr> conditions = new ArrayList<Molang.Expr>(controller.size());
                for (String[] state : controller) conditions.add(Molang.compile(state[1]));
                d.controllerConditions.add(conditions);
            }
            if ("NONE".equals(d.attached)) for (GeoModel m : d.models) {
                d.coversHead |= m.hasCubes("armorHead"); d.coversBody |= m.hasCubes("armorBody");
                d.coversRightArm |= m.hasCubes("armorRightArm"); d.coversLeftArm |= m.hasCubes("armorLeftArm");
                d.coversRightLeg |= m.hasCubes("armorRightLeg") || m.hasCubes("armorRightBoot");
                d.coversLeftLeg |= m.hasCubes("armorLeftLeg") || m.hasCubes("armorLeftBoot");
            }
            return d;
        } catch (Exception e) {
            LogManager.getLogger("Kazer/Cosmetics").warn("Could not load {}: {}", path, e.toString());
            return null;
        }
    }

    private static boolean flag(JsonObject o, String key) { return o.has(key) && o.get(key).isJsonPrimitive() && o.get(key).getAsBoolean(); }

    private static JsonObject json(byte[] data) {
        if (data == null) return null;
        String s = new String(data, StandardCharsets.UTF_8);
        if (!s.isEmpty() && s.charAt(0) == 0xFEFF) s = s.substring(1);
        return new JsonParser().parse(s).getAsJsonObject();
    }

    private static void stateMachine(Definition d, JsonObject gek) {
        JsonObject sm = gek.has("state_machine") && gek.get("state_machine").isJsonObject() ? gek.getAsJsonObject("state_machine") : null;
        if (sm != null && sm.has("controllers")) {
            for (JsonElement c : sm.getAsJsonArray("controllers")) {
                List<String[]> states = new ArrayList<String[]>();
                JsonArray arr = c.getAsJsonObject().getAsJsonArray("states");
                if (arr != null) for (JsonElement s : arr) {
                    JsonObject o = s.getAsJsonObject();
                    if (!o.has("anim")) continue;
                    states.add(new String[]{o.get("anim").getAsString(), o.has("plays_when") ? o.get("plays_when").getAsString() : "1"});
                }
                d.controllers.add(states);
            }
        } else if (sm != null && sm.has("anim")) {
            d.controllers.add(Collections.singletonList(new String[]{sm.get("anim").getAsString(), "1"}));
        } else if (gek.has("default_anim") && gek.get("default_anim").isJsonObject()) {
            List<String[]> states = new ArrayList<String[]>();
            for (Map.Entry<String, JsonElement> e : gek.getAsJsonObject("default_anim").entrySet())
                states.add(new String[]{e.getKey(), e.getValue().getAsString()});
            d.controllers.add(states);
        }
    }

    private static synchronized void loadMolang() {
        if (molangLoaded) return;
        molangLoaded = true;
        Molang.loadFunctions(resource("functions.molang"));
        String source=resource("constants.molang");
        constants = Molang.compile(source);
        petConstants = Molang.compile(petConstantSource(source));
    }

    /** Pet turning follows the short arc; the authored non-pet constants are untouched. */
    static String petConstantSource(String source) {
        return source.replace("math.lerp(c.entity_y_rotation_smoothed, q.entity_y_rotation, 0.05)",
                "math.lerprotate(c.entity_y_rotation_smoothed, q.entity_y_rotation, 0.05)")
            .replace("c.entity_y_rotation_smoothed - q.entity_y_rotation;",
                "math.min_angle(c.entity_y_rotation_smoothed - q.entity_y_rotation);")
            .replace("c.last_head_y_rotation - q.head_y_rotation;",
                "math.min_angle(c.last_head_y_rotation - q.head_y_rotation);");
    }

    private static String resource(String name) {
        try (InputStream in = GeckoCosmetics.class.getResourceAsStream("/assets/kazerui/cosmetics/" + name)) {
            if (in == null) return "";
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            for (int n; (n = in.read(buf)) > 0; ) out.write(buf, 0, n);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception e) { return ""; }
    }

    private static double lifeTime(EntityPlayer p, float partialTicks) {
        return p instanceof Mannequin ? (System.nanoTime()-PREVIEW_START)/1_000_000_000.0 : ((double)p.ticksExisted + partialTicks) / 20.0;
    }

    private static double query(String name, EntityPlayer p, float pt,PetMotionSample motion,boolean gui) {
            boolean preview = gui || p instanceof Mannequin;
            double dx = (p.posX - p.prevPosX) * 20, dy = (p.posY - p.prevPosY) * 20, dz = (p.posZ - p.prevPosZ) * 20;
            if (preview) dx = dy = dz = 0;
            else if(motion!=null){dx=motion.x(pt);dy=motion.y(pt);dz=motion.z(pt);}
            float bodyYaw=motion==null?p.renderYawOffset:PetMotionSample.angle(p.prevRenderYawOffset,p.renderYawOffset,pt);
            float yaw = bodyYaw * (float)Math.PI / 180f;
            switch (name) {
                case "life_time": return lifeTime(p, pt);
                case "is_in_gui": case "is_in_preview_model": return preview ? 1 : 0;
                case "is_crouching": case "is_sneaking": return p.isSneaking() ? 1 : 0;
                case "is_sprinting": case "is_running": return p.isSprinting() ? 1 : 0;
                case "is_on_ground": return p.onGround || preview ? 1 : 0;
                case "is_in_water": return p.isInWater() ? 1 : 0;
                case "is_flying": return p.capabilities.isFlying ? 1 : 0;
                case "ground_speed": return Math.sqrt(dx * dx + dz * dz);
                case "is_moving": return dx * dx + dz * dz > 1e-4 ? 1 : 0;
                case "x_velocity": return dx;
                case "y_velocity": case "local_y_velocity": return dy;
                case "z_velocity": return dz;
                case "local_z_velocity": return -dx * MathHelper.sin(yaw) + dz * MathHelper.cos(yaw);
                case "local_x_velocity": return dx * MathHelper.cos(yaw) + dz * MathHelper.sin(yaw);
                case "head_y_rotation": return preview ? 0 : MathHelper.wrapAngleTo180_float((motion==null?p.rotationYawHead:PetMotionSample.angle(p.prevRotationYawHead,p.rotationYawHead,pt)) - bodyYaw);
                case "head_x_rotation": return preview ? 0 : motion==null?p.rotationPitch:p.prevRotationPitch+(p.rotationPitch-p.prevRotationPitch)*pt;
                case "body_y_rotation": return preview ? 0 : bodyYaw;
                case "entity_y_rotation": return preview ? 0 : motion==null?p.rotationYaw:PetMotionSample.angle(p.prevRotationYaw,p.rotationYaw,pt);
                case "is_first_person": return 0;
                case "is_slim_model": return "slim".equals(((net.minecraft.client.entity.AbstractClientPlayer)p).getSkinType()) ? 1 : 0;
                default: return 0;
            }
    }

    private static GeoModel pose(EntityPlayer p, Definition d, Cosmetic c, float partialTicks) {
        loadMolang();
        boolean gui=p instanceof Mannequin || !Minecraft.getMinecraft().getRenderManager().isRenderShadow();
        Map<EntityPlayer, Map<Integer, State>> owner=gui?GUI_STATES:STATES;
        Map<Integer, State> states = owner.get(p);
        if (states == null) { states = new LinkedHashMap<Integer, State>(8,.75f,true); owner.put(p, states); }
        State st = states.get(c.id);
        if (st == null) {
            if (states.size() >= 8) states.remove(states.keySet().iterator().next());
            st = new State(d.controllers.size(),p,c.type==CosmeticType.PET || c.type==CosmeticType.COMPANION,gui); states.put(c.id, st);
        }
        st.partialTicks = Math.max(0,Math.min(1,partialTicks));
        if(st.motion!=null)st.motion.update(p.ticksExisted,(p.posX-p.prevPosX)*20,(p.posY-p.prevPosY)*20,(p.posZ-p.prevPosZ)*20);
        double now = lifeTime(p, st.partialTicks);
        if(st.clock!=null)now=st.clock.sample(now);
        if(st.clock==null || st.clock.advanced()) {
            st.scope.interpolationSeconds=st.clock==null?-1:st.clock.delta();
            try { (st.clock==null?constants:petConstants).eval(st.scope); }
            catch (RuntimeException ignored) { }
            finally { st.scope.interpolationSeconds=-1; }
        }

        GeoModel geo = d.models.get(0);
        for (int i = 0; i < d.models.size(); i++)
            if (d.modelConditions.get(i).eval(st.scope) != 0) { geo = d.models.get(i); break; }
        geo.resetPose();
        for (int i = 0; i < d.controllers.size(); i++) {
            String pick = null;
            for (int transition=0; transition<d.controllers.get(i).size(); transition++) {
                String[] s = d.controllers.get(i).get(transition);
                double v;
                try { v = d.controllerConditions.get(i).get(transition).eval(st.scope); } catch (RuntimeException e) { v = 0; }
                if (v != 0) { pick = s[0]; break; }
            }
            if (pick == null) continue;
            if (!pick.equals(st.playing[i])) { st.playing[i] = pick; st.started[i] = now; }
            GeoAnimation anim = d.animations.get(pick);
            if (anim == null) continue;
            try { anim.apply(geo, st.scope, now - st.started[i]); } catch (RuntimeException ignored) { }
        }
        return geo;
    }

    private static void draw(GeoModel geo, ResourceLocation texture) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.alphaFunc(516, .003f);
        GlStateManager.enableRescaleNormal();
        geo.render();
        GlStateManager.alphaFunc(516, .1f);
        GlStateManager.disableBlend();
    }

    public static boolean preview(EntityPlayer viewer, Cosmetic c, float x, float y, float w, float h) {
        Definition d = definition(c);
        if (d == null || d.models.isEmpty()) return false;
        ResourceLocation texture = CosmeticTextures.get(d.texture, false);
        if (texture == null) return false;
        GeoModel geo = pose(viewer, d, c, 0);
        float[] b = geo.bounds();
        float size = Math.max(b[3] - b[0], Math.max(b[4] - b[1], b[5] - b[2]));
        if (size <= 0) return false;
        float s = Math.min(w, h) * .62f / size;
        GlStateManager.pushMatrix();
        GlStateManager.enableDepth();
        GlStateManager.enableColorMaterial();
        GlStateManager.translate(x + w / 2, y + h / 2, 150);

        GlStateManager.scale(s, -s, s);
        GlStateManager.rotate(-15, 1, 0, 0);
        GlStateManager.rotate(215, 0, 1, 0);
        GlStateManager.translate(-(b[0] + b[3]) / 2, -(b[1] + b[4]) / 2, -(b[2] + b[5]) / 2);
        net.minecraft.client.renderer.RenderHelper.enableStandardItemLighting();
        draw(geo, texture);
        net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.popMatrix();
        return true;
    }

    static void render(EntityPlayer p, ModelBiped model, Cosmetic c, float partialTicks) {
        boolean preview = p instanceof Mannequin;
        Definition d = definition(c);
        if (d == null || d.models.isEmpty()) return;
        ResourceLocation texture = CosmeticTextures.get(d.texture, false);
        if (texture == null) return;
        GeoModel geo = pose(p, d, c, partialTicks);

        GlStateManager.pushMatrix();
        if (c.type == CosmeticType.COMPANION || (preview && c.type == CosmeticType.PET))
            GlStateManager.translate(preview ? .45f : .8f, preview && c.type == CosmeticType.PET ? -.2f : 0f, 0f);
        if ("NONE".equals(d.attached)) syncArmorBones(geo, model);
        if (p.isSneaking()) GlStateManager.translate(0, .2f, 0);
        ModelRenderer part = part(model, d.attached);
        if (part != null) part.postRender(.0625f);
        GlStateManager.rotate(180, 0, 0, 1);
        GlStateManager.translate(0, -1.51f, 0);
        if (d.offsetWithChestplate && p.getCurrentArmor(2) != null) GlStateManager.translate(0, .065f, 0);
        ObjCosmetics.apply(d.ops, p);

        draw(geo, texture);
        GlStateManager.popMatrix();
    }

    private static ModelRenderer part(ModelBiped m, String attached) {
        switch (attached) {
            case "HEAD": return m.bipedHead;
            case "SHOULDER": return m.bipedBody;
            case "LEFT_ARM": return m.bipedLeftArm;
            case "RIGHT_ARM": return m.bipedRightArm;
            case "LEFT_LEG": return m.bipedLeftLeg;
            case "RIGHT_LEG": return m.bipedRightLeg;
            default: return null;
        }
    }

    private static void syncArmorBones(GeoModel geo, ModelBiped m) {
        follow(geo.bones.get("armorHead"), m.bipedHead, 0, 0);
        follow(geo.bones.get("armorBody"), m.bipedBody, 0, 0);
        follow(geo.bones.get("armorRightArm"), m.bipedRightArm, 5, 2);
        follow(geo.bones.get("armorLeftArm"), m.bipedLeftArm, -5, 2);
        follow(geo.bones.get("armorRightLeg"), m.bipedRightLeg, 2, 12);
        follow(geo.bones.get("armorRightBoot"), m.bipedRightLeg, 2, 12);
        follow(geo.bones.get("armorLeftLeg"), m.bipedLeftLeg, -2, 12);
        follow(geo.bones.get("armorLeftBoot"), m.bipedLeftLeg, -2, 12);
    }

    private static void follow(GeoModel.Bone bone, ModelRenderer part, float dx, float baseY) {
        if (bone == null) return;
        bone.rotX = -part.rotateAngleX;
        bone.rotY = -part.rotateAngleY;
        bone.rotZ = part.rotateAngleZ;
        bone.posX = part.rotationPointX + dx;
        bone.posY = baseY - part.rotationPointY;
        bone.posZ = part.rotationPointZ;
    }

    public static void hideParts(EntityPlayer p, ModelPlayer m, List<Cosmetic> worn) {
        boolean head = false, body = false, ra = false, la = false, rl = false, ll = false;
        for (Cosmetic c : worn) {
            if (!c.geckolib) continue;
            Definition d = definition(c);
            if (d == null) continue;
            head |= d.hideHead; body |= d.hideBody; ra |= d.hideRightArm; la |= d.hideLeftArm; rl |= d.hideRightLeg; ll |= d.hideLeftLeg;

            if (d.coversHead) m.bipedHeadwear.showModel = false;
            if (d.coversBody) m.bipedBodyWear.showModel = false;
            if (d.coversRightArm) m.bipedRightArmwear.showModel = false;
            if (d.coversLeftArm) m.bipedLeftArmwear.showModel = false;
            if (d.coversRightLeg) m.bipedRightLegwear.showModel = false;
            if (d.coversLeftLeg) m.bipedLeftLegwear.showModel = false;
        }
        if (head) { m.bipedHead.showModel = false; m.bipedHeadwear.showModel = false; }
        if (body) { m.bipedBody.showModel = false; m.bipedBodyWear.showModel = false; }
        if (ra) { m.bipedRightArm.showModel = false; m.bipedRightArmwear.showModel = false; }
        if (la) { m.bipedLeftArm.showModel = false; m.bipedLeftArmwear.showModel = false; }
        if (rl) { m.bipedRightLeg.showModel = false; m.bipedRightLegwear.showModel = false; }
        if (ll) { m.bipedLeftLeg.showModel = false; m.bipedLeftLegwear.showModel = false; }
    }

    /** Save flags as bits, not a new array/list for each player render. */
    public static int visibility(ModelBiped m) {
        int bits=(m.bipedHead.showModel?1:0)|(m.bipedBody.showModel?2:0)|(m.bipedRightArm.showModel?4:0)
            |(m.bipedLeftArm.showModel?8:0)|(m.bipedRightLeg.showModel?16:0)|(m.bipedLeftLeg.showModel?32:0)|(m.bipedHeadwear.showModel?64:0);
        if(m instanceof ModelPlayer){ModelPlayer p=(ModelPlayer)m;
            bits|=(p.bipedBodyWear.showModel?128:0)|(p.bipedRightArmwear.showModel?256:0)|(p.bipedLeftArmwear.showModel?512:0)
                |(p.bipedRightLegwear.showModel?1024:0)|(p.bipedLeftLegwear.showModel?2048:0);
        }
        return bits;
    }
    public static void visibility(ModelBiped m,int bits) {
        m.bipedHead.showModel=(bits&1)!=0;m.bipedBody.showModel=(bits&2)!=0;m.bipedRightArm.showModel=(bits&4)!=0;
        m.bipedLeftArm.showModel=(bits&8)!=0;m.bipedRightLeg.showModel=(bits&16)!=0;m.bipedLeftLeg.showModel=(bits&32)!=0;m.bipedHeadwear.showModel=(bits&64)!=0;
        if(m instanceof ModelPlayer){ModelPlayer p=(ModelPlayer)m;
            p.bipedBodyWear.showModel=(bits&128)!=0;p.bipedRightArmwear.showModel=(bits&256)!=0;p.bipedLeftArmwear.showModel=(bits&512)!=0;
            p.bipedRightLegwear.showModel=(bits&1024)!=0;p.bipedLeftLegwear.showModel=(bits&2048)!=0;
        }
    }
}
