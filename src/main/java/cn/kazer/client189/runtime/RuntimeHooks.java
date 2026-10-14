package cn.kazer.client189.runtime;

import com.cheatbreaker.client.CheatBreaker;
import com.cheatbreaker.client.event.type.PluginMessageEvent;
import com.cheatbreaker.client.module.type.ChatModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.network.play.server.S3FPacketCustomPayload;

public final class RuntimeHooks {
    public static boolean chunkVisible(int x,int z,int cameraX,int cameraZ,int distance) {
        return Math.abs((long)x-cameraX)<=distance && Math.abs((long)z-cameraZ)<=distance;
    }
    private static final java.nio.FloatBuffer SKY_CLIP=clipMatrix();
    private static java.nio.FloatBuffer clipMatrix() {
        java.nio.FloatBuffer matrix=org.lwjgl.BufferUtils.createFloatBuffer(16);
        matrix.put(new float[]{1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,4});matrix.flip();return matrix;
    }
    public static void customSkyBegin() {
        GlStateManager.matrixMode(5889);GlStateManager.pushMatrix();
        if(Minecraft.getMinecraft().gameSettings.renderDistanceChunks<=4)GlStateManager.multMatrix(SKY_CLIP);
        GlStateManager.matrixMode(5888);
    }
    public static void customSkyEnd() {
        GlStateManager.matrixMode(5889);GlStateManager.popMatrix();GlStateManager.matrixMode(5888);
    }
    public static net.minecraft.client.renderer.chunk.RenderChunk visibleChunk(net.minecraft.client.renderer.chunk.RenderChunk chunk,net.minecraft.util.BlockPos camera) {
        if(chunk==null)return null;
        net.minecraft.util.BlockPos position=chunk.getPosition();
        return chunkVisible(position.getX()>>4,position.getZ()>>4,camera.getX()>>4,camera.getZ()>>4,Minecraft.getMinecraft().gameSettings.renderDistanceChunks)?chunk:null;
    }
    public static void clearDistanceFog(boolean normalAir) {
        Minecraft mc=Minecraft.getMinecraft();net.minecraft.entity.Entity camera=mc.getRenderViewEntity();
        if(normalAir && mc.theWorld!=null && camera!=null && mc.theWorld.provider.getDimensionId()==0
            && !mc.theWorld.provider.doesXZShowFog((int)camera.posX,(int)camera.posZ)) {
            GlStateManager.setFogStart(4096F);GlStateManager.setFogEnd(5120F);
        }
    }
    /** Only suppress the obsolete one-block void skirt, not terrain or sky planes. */
    public static double horizonBoxHeight(double height) {
        return net.minecraft.client.Minecraft.getMinecraft().gameSettings.renderDistanceChunks<=4?0:height;
    }
    public static void resumeAfterEscape(Minecraft mc) {
        if(mc.currentScreen==null)mc.setIngameFocus();
    }
    public static void resourcePackScan(Object repository) {
        if(Minecraft.getMinecraft().currentScreen instanceof cn.kazer.client189.ui.KazerResourcePacks)return;
        ((net.minecraft.client.resources.ResourcePackRepository)repository).updateRepositoryEntriesAll();
    }
    public static void splash(Object textureManager) {
        Minecraft mc=Minecraft.getMinecraft();
        net.minecraft.client.gui.ScaledResolution res=new net.minecraft.client.gui.ScaledResolution(mc);
        net.minecraft.client.renderer.GlStateManager.matrixMode(5889);net.minecraft.client.renderer.GlStateManager.loadIdentity();
        net.minecraft.client.renderer.GlStateManager.ortho(0,res.getScaledWidth(),res.getScaledHeight(),0,1000,3000);
        net.minecraft.client.renderer.GlStateManager.matrixMode(5888);net.minecraft.client.renderer.GlStateManager.loadIdentity();
        net.minecraft.client.renderer.GlStateManager.translate(0,0,-2000);net.minecraft.client.renderer.GlStateManager.disableDepth();
        net.minecraft.client.gui.Gui.drawRect(0,0,res.getScaledWidth(),res.getScaledHeight(),0xFF000000);
        try(java.io.InputStream input=RuntimeHooks.class.getResourceAsStream("/assets/minecraft/client/kazer_logo.png")){
            net.minecraft.client.renderer.texture.TextureManager textures=(net.minecraft.client.renderer.texture.TextureManager)textureManager;
            net.minecraft.util.ResourceLocation logo=textures.getDynamicTextureLocation("kazer-startup",new net.minecraft.client.renderer.texture.DynamicTexture(javax.imageio.ImageIO.read(input)));
            textures.bindTexture(logo);net.minecraft.client.renderer.GlStateManager.enableBlend();
            net.minecraft.client.renderer.GlStateManager.tryBlendFuncSeparate(770,771,1,0);net.minecraft.client.renderer.GlStateManager.color(1,1,1,1);
            int size=Math.max(32,Math.min(44,res.getScaledHeight()/6));
            net.minecraft.client.gui.Gui.drawModalRectWithCustomSizedTexture(res.getScaledWidth()/2-size/2,res.getScaledHeight()/2-size-10,0,0,size,size,size,size);
        }catch(java.io.IOException error){org.apache.logging.log4j.LogManager.getLogger().warn("Could not draw Kazer startup logo",error);}
        int x=res.getScaledWidth()/2-66,y=res.getScaledHeight()/2+31;
        net.minecraft.client.gui.Gui.drawRect(x,y,x+132,y+2,0xFF36413C);
        net.minecraft.client.gui.Gui.drawRect(x,y,x+17,y+2,0xFF55D5C8);
        net.minecraft.client.renderer.GlStateManager.enableAlpha();net.minecraft.client.renderer.GlStateManager.alphaFunc(516,.1F);
        org.lwjgl.opengl.Display.update();
    }
    public static void optionsLoad() { com.cheatbreaker.client.config.SharedGameOptions.beforeLoad(Minecraft.getMinecraft().mcDataDir,"1.8.9"); }
    public static void optionsSave() {
        com.cheatbreaker.client.config.SharedGameOptions.afterSave(Minecraft.getMinecraft().mcDataDir,"1.8.9");
        if(CheatBreaker.getInstance()!=null&&CheatBreaker.getInstance().configManager!=null)CheatBreaker.getInstance().configManager.writeGlobalConfig();
    }
    private RuntimeHooks() { }
    private static final java.util.Map<String,Integer> TAB_COLORS=new java.util.LinkedHashMap<>();
    private static final java.util.Set<String> TAB_OWN_NAMES=new java.util.HashSet<>();
    private static final String BADGE_PREFIX="\u00a7r   ";
    public static void joinedWorld(boolean firstJoin) {
        if (Minecraft.getMinecraft().isCallingFromMinecraftThread() && CheatBreaker.getInstance()!=null)
            CheatBreaker.getInstance().netHandler.onJoinGame(firstJoin);
    }
    public static void registerApollo() {
        Minecraft mc=Minecraft.getMinecraft();if(!mc.isCallingFromMinecraftThread()||mc.getNetHandler()==null)return;
        byte[] channels="lunar:apollo\0apollo:json".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        mc.getNetHandler().addToSendQueue(new net.minecraft.network.play.client.C17PacketCustomPayload("REGISTER",
                new net.minecraft.network.PacketBuffer(io.netty.buffer.Unpooled.wrappedBuffer(channels))));
        mc.getNetHandler().addToSendQueue(new net.minecraft.network.play.client.C17PacketCustomPayload(
                com.cheatbreaker.client.nethandler.apollo.ApolloProtocol.CHANNEL,
                new net.minecraft.network.PacketBuffer(io.netty.buffer.Unpooled.wrappedBuffer(
                        com.cheatbreaker.client.nethandler.apollo.ApolloProtocol.handshake()))));
    }
    public static void keepAlive() {
        if(CheatBreaker.getInstance()!=null)CheatBreaker.getInstance().eventBus.callEvent(new com.cheatbreaker.client.event.type.KeepAliveEvent());
    }
    public static void customCrosshair(ScaledResolution resolution) {
        com.cheatbreaker.client.config.GlobalSettings settings=CheatBreaker.getInstance().globalSettings;
        float factor=1F/CheatBreaker.getInstance().getScaleFactor();
        float x=(int)(resolution.getScaledWidth()/factor)/2F, y=(int)(resolution.getScaledHeight()/factor)/2F;
        float size=((Number)settings.crosshairSize.getValue()).floatValue(), gap=((Number)settings.crosshairGap.getValue()).floatValue();
        float half=((Number)settings.crosshairThickness.getValue()).floatValue()/2F;
        float[][] arms={{x-gap-size,y-half,x-gap,y+half},{x+gap,y-half,x+gap+size,y+half},
                {x-half,y-gap-size,x+half,y-gap},{x-half,y+gap,x+half,y+gap+size}};
        GlStateManager.pushMatrix();GlStateManager.scale(factor,factor,factor);
        for(float[] arm:arms) {
            if((Boolean)settings.crosshairOutline.getValue()) cn.kazer.client189.compat.LegacyGui.drawRectWithOutline(
                    arm[0],arm[1],arm[2],arm[3],.5F,-1358954496,settings.crosshairColor.getColorValue());
            else cn.kazer.client189.compat.LegacyGui.drawRect(arm[0],arm[1],arm[2],arm[3],settings.crosshairColor.getColorValue());
        }
        GlStateManager.popMatrix();GlStateManager.color(1,1,1,1);
    }
    public static String tabName(String name,Object object){
        net.minecraft.client.network.NetworkPlayerInfo tabPlayer=(net.minecraft.client.network.NetworkPlayerInfo)object;
        CheatBreaker core=CheatBreaker.getInstance();Minecraft mc=Minecraft.getMinecraft();
        java.util.UUID uuid=tabPlayer.getGameProfile().getId();
        boolean local=uuid!=null && uuid.equals(mc.getSession().getProfile().getId());
        if(local && com.cheatbreaker.client.kazerui.module.modules.mechanic.ModuleTab.highlighting()){if(TAB_OWN_NAMES.size()>256)TAB_OWN_NAMES.clear();TAB_OWN_NAMES.add(name);}
        if(core==null||!com.cheatbreaker.client.kazerui.module.modules.mechanic.ModuleTab.logoEnabled(core.moduleManager.nametagModule.isEnabled() && (Boolean)core.moduleManager.nametagModule.showBadgeInTab.getValue()))return name;
        boolean show=(uuid!=null && (uuid.equals(mc.getSession().getProfile().getId())||core.getPresenceClient().isKazerClient(uuid)))
                && com.cheatbreaker.client.module.type.nametag.NametagLayout.isPlayerNameLine(name,tabPlayer.getGameProfile().getName())
                ;
        if(!show)return name;
        String result=BADGE_PREFIX+name;
        if(local && com.cheatbreaker.client.kazerui.module.modules.mechanic.ModuleTab.highlighting())TAB_OWN_NAMES.add(result);
        if(TAB_COLORS.size()>256)TAB_COLORS.clear();
        TAB_COLORS.put(result,core.getPresenceClient().badgeColor(uuid));
        return result;
    }
    public static int tabText(Object renderer,String text,float x,float y,int color){
        net.minecraft.client.gui.FontRenderer font=(net.minecraft.client.gui.FontRenderer)renderer;
        String styled=text.startsWith("\u00a7o")?text.substring(2):text;
        color=com.cheatbreaker.client.kazerui.module.modules.mechanic.ModuleTab.nameColor(color,TAB_OWN_NAMES.contains(styled));
        boolean shadow=com.cheatbreaker.client.kazerui.module.modules.mechanic.ModuleTab.nameShadow(false);
        if(styled.startsWith(BADGE_PREFIX)&&TAB_COLORS.containsKey(styled)){
            cn.kazer.client189.compat.LegacyGL.glPushAttrib(org.lwjgl.opengl.GL11.GL_ALL_ATTRIB_BITS);
            try{
                Minecraft mc=Minecraft.getMinecraft();mc.getTextureManager().bindTexture(new net.minecraft.util.ResourceLocation("client/kazer_badge.png"));
                    int badgeColor=TAB_COLORS.get(styled);
                GlStateManager.color((badgeColor>>16&255)/255F,(badgeColor>>8&255)/255F,(badgeColor&255)/255F,1F);
                net.minecraft.client.gui.Gui.drawModalRectWithCustomSizedTexture((int)x,(int)y,0,0,7,7,7,7);
            }finally{cn.kazer.client189.compat.LegacyGL.glPopAttrib();}
            return font.drawString((text.startsWith("\u00a7o")?"\u00a7o":"")+styled.substring(BADGE_PREFIX.length()),x+9,y,color,shadow);
        }
        return font.drawString(text,x,y,color,shadow);
    }
    public static boolean tabPing(Object object) {
        net.minecraft.client.network.NetworkPlayerInfo info=(net.minecraft.client.network.NetworkPlayerInfo)object;
        if(info==null||info.getResponseTime()<0)return false;
        // Display names may be nicknames, ranks or plugin labels. Native ping
        // belongs to the packet row, not to a text/skin identity heuristic.
        return true;
    }
    public static boolean tabPing(int width,int x,int y,Object object){net.minecraft.client.network.NetworkPlayerInfo info=(net.minecraft.client.network.NetworkPlayerInfo)object;return tabPing(object) && !com.cheatbreaker.client.kazerui.module.modules.mechanic.ModuleTab.drawPing(x,y,width,info.getResponseTime());}
    public static boolean listKey(net.minecraft.client.settings.KeyBinding key){boolean down=key.isKeyDown();return key==Minecraft.getMinecraft().gameSettings.keyBindPlayerList?com.cheatbreaker.client.kazerui.module.modules.mechanic.ModuleTab.listKey(down):down;}
    public static boolean clearGlass(Object state){
        CheatBreaker core=CheatBreaker.getInstance();if(core==null)return false;
        net.minecraft.block.Block block=((net.minecraft.block.state.IBlockState)state).getBlock();
        if(com.cheatbreaker.client.kazerui.module.modules.visual.ModuleOverlay.hideBlock(block))return true;
        String mode=com.cheatbreaker.client.kazerui.module.modules.visual.ModuleOverlay.glass(String.valueOf(core.globalSettings.clearGlass.getValue()));
        return block instanceof net.minecraft.block.BlockGlass&&(mode.equals("REGULAR")||mode.equals("ALL"))
                ||block instanceof net.minecraft.block.BlockStainedGlass&&mode.equals("ALL");
    }
    public static boolean drawGuiBackground() {
        Minecraft mc=Minecraft.getMinecraft();CheatBreaker core=CheatBreaker.getInstance();
        if(core==null||mc.theWorld==null||mc.currentScreen instanceof com.cheatbreaker.client.ui.module.CBModulesGui
                ||mc.currentScreen instanceof com.cheatbreaker.client.ui.module.CBModulePlaceGui
                ||!(Boolean)core.globalSettings.containerVignette.getValue())return false;
        ScaledResolution resolution=new ScaledResolution(mc);
        cn.kazer.client189.compat.KazerCompat.drawVignette(resolution.getScaledWidth(),resolution.getScaledHeight(),
                cn.kazer.client189.compat.KazerCompat.vignette().worldGuiProgress(Minecraft.getSystemTime()));
        return true;
    }
    public static boolean hideAchievements(){return CheatBreaker.getInstance()!=null&&!(Boolean)CheatBreaker.getInstance().globalSettings.showAchievements.getValue();}
    public static boolean hideGlint(){return !com.cheatbreaker.client.kazerui.module.modules.visual.ModuleOverlay.glint(CheatBreaker.getInstance()==null||(Boolean)CheatBreaker.getInstance().globalSettings.enchantmentGlint.getValue(),false);}
    public static boolean itemGlint(Object object) {
        net.minecraft.item.ItemStack stack=(net.minecraft.item.ItemStack)object;
        return stack.hasEffect() && (!(stack.getItem() instanceof net.minecraft.item.ItemPotion)
                || CheatBreaker.getInstance()==null || (Boolean)CheatBreaker.getInstance().globalSettings.shinyPots.getValue());
    }
    public static int hotbarScroll(int delta){return CheatBreaker.getInstance()!=null&&(Boolean)CheatBreaker.getInstance().globalSettings.disableHotbarScrolling.getValue()?0:delta;}
    public static boolean zoomKeyboard(int key){return com.cheatbreaker.client.kazerui.module.modules.mechanic.ModuleZoom.ownsNative() && key==cn.kazer.client189.compat.KazerCompat.zoomKey().getKeyCode()?false:org.lwjgl.input.Keyboard.isKeyDown(key);}
    public static boolean zoomMouse(int button){return com.cheatbreaker.client.kazerui.module.modules.mechanic.ModuleZoom.ownsNative() && button==cn.kazer.client189.compat.KazerCompat.zoomKey().getKeyCode()+100?false:org.lwjgl.input.Mouse.isButtonDown(button);}
    public static boolean zoomKey(net.minecraft.client.settings.KeyBinding key){return com.cheatbreaker.client.kazerui.module.modules.mechanic.ModuleZoom.ownsNative() && key==cn.kazer.client189.compat.KazerCompat.zoomKey()?false:net.minecraft.client.settings.GameSettings.isKeyDown(key);}
    public static void blockOutline(net.minecraft.util.AxisAlignedBB box){if(!com.cheatbreaker.client.kazerui.module.modules.visual.ModuleBlockOutline.draw(box))net.minecraft.client.renderer.RenderGlobal.drawSelectionBoundingBox(box);}
    public static void centerInventory(Object gui){
        Minecraft mc=Minecraft.getMinecraft();
        if(CheatBreaker.getInstance()==null)return;
        boolean showPotions=(Boolean)CheatBreaker.getInstance().globalSettings.showPotionInfo.getValue();
        if(!showPotions) net.minecraftforge.fml.relauncher.ReflectionHelper.setPrivateValue(
                net.minecraft.client.renderer.InventoryEffectRenderer.class,(net.minecraft.client.renderer.InventoryEffectRenderer)gui,
                false,"hasActivePotionEffects","field_147045_u");
        if(showPotions&&!(Boolean)CheatBreaker.getInstance().globalSettings.keepInventoryCentered.getValue())return;
        net.minecraft.client.gui.inventory.GuiContainer container=(net.minecraft.client.gui.inventory.GuiContainer)gui;
        int size=net.minecraftforge.fml.relauncher.ReflectionHelper.getPrivateValue(net.minecraft.client.gui.inventory.GuiContainer.class,container,"xSize","field_146999_f");
        net.minecraftforge.fml.relauncher.ReflectionHelper.setPrivateValue(net.minecraft.client.gui.inventory.GuiContainer.class,container,(container.width-size)/2,"guiLeft","field_147003_i");
    }
    public static boolean hideEntity(Object object){
        Minecraft mc=Minecraft.getMinecraft();CheatBreaker core=CheatBreaker.getInstance();
        if(core==null||mc.thePlayer==null||object==mc.thePlayer)return false;
        int distance=((Number)core.globalSettings.entityDistance.getValue()).intValue();
        return ((net.minecraft.entity.Entity)object).getDistanceSqToEntity(mc.thePlayer)>com.cheatbreaker.client.config.RenderSettingsPolicy.distanceSquared(distance);
    }
    public static boolean hideTile(Object object){
        Minecraft mc=Minecraft.getMinecraft();CheatBreaker core=CheatBreaker.getInstance();
        if(core==null||mc.thePlayer==null)return false;
        int distance=((Number)core.globalSettings.tileEntityDistance.getValue()).intValue();
        return ((net.minecraft.tileentity.TileEntity)object).getDistanceSq(mc.thePlayer.posX,mc.thePlayer.posY,mc.thePlayer.posZ)>com.cheatbreaker.client.config.RenderSettingsPolicy.distanceSquared(distance);
    }
    public static boolean dropParticle() {
        if (com.cheatbreaker.client.kazerui.module.modules.visual.ModuleParticleChanger.active()) return false;
        CheatBreaker core = CheatBreaker.getInstance();
        if (core == null) return false;
        int percent = ((Number)core.globalSettings.particleAmount.getValue()).intValue();
        return percent < 100 && !com.cheatbreaker.client.config.RenderSettingsPolicy.keepParticle(percent,
                java.util.concurrent.ThreadLocalRandom.current().nextInt(100));
    }
    public static long chunkDeadline(long nativeDeadline) {
        CheatBreaker core=CheatBreaker.getInstance();if(core==null)return nativeDeadline;
        return ChunkUploadBudget.deadline(System.nanoTime(),nativeDeadline,
                (Boolean)core.globalSettings.enableFpsBoost.getValue(),
                ((Number)core.globalSettings.slowChunkLoading.getValue()).intValue());
    }
    public static int frameLimit(int normal) {
        CheatBreaker core = CheatBreaker.getInstance();
        if (core == null) return normal;
        if (Minecraft.getMinecraft().theWorld == null) {
            int menuLimit=((Number)core.globalSettings.maxMainMenuFps.getValue()).intValue();
            normal=com.cheatbreaker.client.kazerui.KazerUiRuntime.isOwnedScreen(Minecraft.getMinecraft().currentScreen)
                ? menuLimit : Math.min(normal,menuLimit);
        }
        return com.cheatbreaker.client.config.RenderSettingsPolicy.frameLimit(normal,
                org.lwjgl.opengl.Display.isActive(), (Boolean)core.globalSettings.limitUnfocusedFps.getValue(),
                ((Number)core.globalSettings.maxUnfocusedFps.getValue()).intValue());
    }
    public static int sprintTimer(int vanilla) {
        CheatBreaker core=CheatBreaker.getInstance();
        return core!=null && core.ownedUi!=null && !com.cheatbreaker.client.kazerui.module.modules.mechanic.ModuleToggleSneak.doubleTapEnabled() ? 0 : vanilla;
    }
    public static float gamma(float vanilla) {
        CheatBreaker core = CheatBreaker.getInstance();
        return core==null?vanilla:com.cheatbreaker.client.kazerui.module.modules.mechanic.ModuleLighting.gamma(vanilla);
    }
    public static void lightingTexture(Object texture) {
        if(com.cheatbreaker.client.kazerui.module.modules.mechanic.ModuleLighting.fullBright())
            java.util.Arrays.fill(((net.minecraft.client.renderer.texture.DynamicTexture)texture).getTextureData(),0xFFFFFFFF);
    }
    private static com.cheatbreaker.client.module.type.motionblur.MotionBlurModule blur() {
        return CheatBreaker.getInstance() == null || CheatBreaker.getInstance().ownedUi != null ? null : com.cheatbreaker.client.module.type.motionblur.MotionBlurModule.getActive();
    }
    public static void worldBegin(float partial) { if (blur() != null) blur().onWorldRenderBegin(partial); }
    public static void cameraReady(float partial) { if (blur() != null) blur().onCameraReady(partial); }
    public static void worldEnd(float partial) { if (blur() != null) blur().onWorldRenderEnd(partial); }
    public static void motionBlur() {
        CheatBreaker core=CheatBreaker.getInstance();
        if(core!=null && core.ownedUi!=null) com.cheatbreaker.client.kazerui.module.modules.visual.ModuleMotionBlur.postProcess();
        else if(blur()!=null)blur().renderMotionBlur();
        com.cheatbreaker.client.kazerui.module.modules.visual.ModuleColorSaturation.postProcess();
    }
    public static void pluginMessage(Object message) {
        Minecraft mc = Minecraft.getMinecraft();
        // The off-thread invocation will be retried on the client thread by PacketThreadUtil.
        if (!mc.isCallingFromMinecraftThread() || CheatBreaker.getInstance() == null) return;
        S3FPacketCustomPayload packet = (S3FPacketCustomPayload)message;
        byte[] bytes = new byte[packet.getBufferData().readableBytes()];
        packet.getBufferData().getBytes(packet.getBufferData().readerIndex(), bytes);
        CheatBreaker.getInstance().eventBus.callEvent(new PluginMessageEvent(packet.getChannelName(), bytes));
    }
    public static boolean hideVanillaScoreboard() {
        CheatBreaker core=CheatBreaker.getInstance();
        return core!=null && (core.ownedUi==null ? core.moduleManager.scoreboard.isEnabled() : core.ownedUi.features().isEnabled("scoreboard"));
    }
    public static boolean beginChat() {
        if (com.cheatbreaker.client.kazerui.module.modules.mechanic.ChatRuntime.active()) { GlStateManager.pushMatrix(); return true; }
        Minecraft mc = Minecraft.getMinecraft();
        CheatBreaker core = CheatBreaker.getInstance();
        if (core == null) { GlStateManager.pushMatrix(); return true; }
        ChatModule chat = core.moduleManager.chatModule;
        if (!chat.shouldRenderChat(mc.ingameGUI.getChatGUI().getChatOpen())) return false;
        GlStateManager.pushMatrix();
        if (chat.isEnabled()) {
            ScaledResolution resolution = new ScaledResolution(mc);
            GlStateManager.translate(0, -(resolution.getScaledHeight() - 48), 0);
            chat.updateChatDimensions(); chat.scaleAndTranslate(resolution);
            GlStateManager.translate(0, chat.getHeight() - 20, 0);
        }
        return true;
    }
    public static String chatSend(String message) {
        String result = com.cheatbreaker.client.kazerui.module.ChatSendEvent.fire(message);
        return result == null || result.isEmpty() ? null : result;
    }
    public static void chatClose(Minecraft mc, net.minecraft.client.gui.GuiScreen screen, net.minecraft.client.gui.GuiTextField input, int key) {
        if (screen == null && (key == 28 || key == 156) && com.cheatbreaker.client.kazerui.module.modules.mechanic.ChatHooks.keepChatOpen()) input.setText("");
        else mc.displayGuiScreen(screen);
    }
    public static void chatInputRect(int left, int top, int right, int bottom, int color) {
        com.cheatbreaker.client.kazerui.module.modules.mechanic.ChatHooks.inputBackground(left,top,right,bottom,color);
    }
    public static void endChat() { GlStateManager.popMatrix(); }
    /** Inverse of beginChat + vanilla's (2,20) and chatScale transforms. */
    public static net.minecraft.util.IChatComponent chatComponent(Object gui,int displayX,int displayY) {
        if (com.cheatbreaker.client.kazerui.module.modules.mechanic.ChatRuntime.active())
            return com.cheatbreaker.client.kazerui.module.modules.mechanic.ChatRuntime.component(gui,displayX,displayY);
        Minecraft mc=Minecraft.getMinecraft();
        net.minecraft.client.gui.GuiNewChat chatGui=(net.minecraft.client.gui.GuiNewChat)gui;
        ChatModule module=CheatBreaker.getInstance().moduleManager.chatModule;
        if(!chatGui.getChatOpen()||!module.shouldRenderChat(true)||!module.isRenderHud())return null;
        module.updateChatDimensions();
        ScaledResolution resolution=new ScaledResolution(mc);
        float scale=((Number)module.scale.getValue()).floatValue();
        float[] points=module.getScaledPoints(resolution,true);
        float combined=scale*chatGui.getChatScale();
        float originX=(points[0]+2)*scale;
        float bottom=resolution.getScaledHeight()-(points[1]+module.getHeight())*scale;
        int x=net.minecraft.util.MathHelper.floor_float((displayX/(float)resolution.getScaleFactor()-originX)/combined);
        int y=net.minecraft.util.MathHelper.floor_float((displayY/(float)resolution.getScaleFactor()-bottom)/combined);
        com.cheatbreaker.client.kazerui.module.render.KazerChatAccess access=
                (com.cheatbreaker.client.kazerui.module.render.KazerChatAccess)chatGui;
        java.util.List<net.minecraft.client.gui.ChatLine> lines=access.kazerDrawnLines();
        if(x<0||y<0||x>chatGui.getChatWidth()/chatGui.getChatScale()
                ||y>=Math.min(chatGui.getLineCount(),lines.size())*9)return null;
        int scroll=access.kazerChatScroll();
        int row=y/9+scroll;if(row<0||row>=lines.size())return null;
        int width=0;
        for(net.minecraft.util.IChatComponent part:lines.get(row).getChatComponent())
            if(part instanceof net.minecraft.util.ChatComponentText) {
                width+=mc.fontRendererObj.getStringWidth(net.minecraft.client.gui.GuiUtilRenderComponents.func_178909_a(
                        ((net.minecraft.util.ChatComponentText)part).getChatComponentText_TextValue(),false));
                if(width>x)return part;
            }
        return null;
    }
    public static boolean customChatHit() {
        return com.cheatbreaker.client.kazerui.module.modules.mechanic.ChatRuntime.active()
                || CheatBreaker.getInstance()!=null&&CheatBreaker.getInstance().moduleManager.chatModule.isEnabled();
    }
    public static void chatRect(int left, int top, int right, int bottom, int color) {
        CheatBreaker core = CheatBreaker.getInstance();
        if (core == null || !core.moduleManager.chatModule.isEnabled() || (Boolean)core.moduleManager.chatModule.showBackground.getValue())
            net.minecraft.client.gui.Gui.drawRect(left, top, right, bottom, color);
    }
}
