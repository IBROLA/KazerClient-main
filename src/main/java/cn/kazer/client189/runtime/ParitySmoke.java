package cn.kazer.client189.runtime;

import com.cheatbreaker.client.CheatBreaker;
import com.cheatbreaker.client.ui.module.CBModulesGui;
import com.cheatbreaker.client.ui.module.KazerModulesGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.util.ScreenShotHelper;
import java.util.Arrays;

/** Opt-in isolated integration test; never runs in the user's normal game. */
public final class ParitySmoke {
    private int frame;
    private boolean worldRequested;
    public static void startup(com.cheatbreaker.client.ui.mainmenu.LoadingScreen loading) {
        if(!Boolean.getBoolean("kazer.test.parity"))return;
        try {
            Minecraft mc=Minecraft.getMinecraft();
            mc.getFramebuffer().unbindFramebuffer();
            org.lwjgl.opengl.GL11.glPushAttrib(org.lwjgl.opengl.GL11.GL_PIXEL_MODE_BIT);
            try {
                org.lwjgl.opengl.GL11.glReadBuffer(org.lwjgl.opengl.GL11.GL_FRONT);
                int w=mc.displayWidth,h=mc.displayHeight;
                java.nio.ByteBuffer pixels=org.lwjgl.BufferUtils.createByteBuffer(w*h*4);
                org.lwjgl.opengl.GL11.glReadPixels(0,0,w,h,org.lwjgl.opengl.GL11.GL_RGBA,org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE,pixels);
                java.awt.image.BufferedImage image=new java.awt.image.BufferedImage(w,h,java.awt.image.BufferedImage.TYPE_INT_ARGB);
                for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
                    int n=(y*w+x)*4;
                    image.setRGB(x,h-1-y,0xff000000|(pixels.get(n)&255)<<16|(pixels.get(n+1)&255)<<8|(pixels.get(n+2)&255));
                }
                java.io.File dir=new java.io.File(mc.mcDataDir,"screenshots");dir.mkdirs();
                javax.imageio.ImageIO.write(image,"png",new java.io.File(dir,"00-startup.png"));
            }finally{org.lwjgl.opengl.GL11.glPopAttrib();mc.getFramebuffer().bindFramebuffer(true);}
            System.out.println("[KC PARITY] Shared Kazer startup screen captured");
        } catch(java.io.IOException error){throw new IllegalStateException(error);}
    }
    public void frame() {
        if (!Boolean.getBoolean("kazer.test.parity")) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (worldRequested && frame < 140 && mc.thePlayer == null) return;
        frame++;
        if (frame == 40) {
            if(!com.cheatbreaker.client.util.KazerBuildInfo.windowTitle("1.8.9").equals(org.lwjgl.opengl.Display.getTitle()))
                throw new IllegalStateException("Incorrect startup window title");
            capture("01-main-menu.png");
            com.cheatbreaker.client.ui.mainmenu.MainMenu menu=(com.cheatbreaker.client.ui.mainmenu.MainMenu)mc.currentScreen;
            mc.displayGuiScreen(new com.cheatbreaker.client.ui.mainmenu.KazerAccountsGui(menu));
        }
        if (frame == 50) capture("01b-main-menu-accounts.png");
        if (frame == 55) {
            mc.displayGuiScreen(new net.minecraft.client.gui.GuiMultiplayer(new com.cheatbreaker.client.ui.mainmenu.MainMenu()));
            net.minecraft.client.gui.GuiMultiplayer menu=(net.minecraft.client.gui.GuiMultiplayer)mc.currentScreen;
            if(!KazerPinnedServers.isPinned(menu.getServerList().getServerData(0)))
                throw new IllegalStateException("Pinned Kazer server missing");
            menu.selectServer(0);
            if(KazerPinnedServers.canMove(menu,null,0,false))throw new IllegalStateException("Pinned server can move");
            menu.getServerList().saveServerList();
            try {
                net.minecraft.nbt.NBTTagCompound disk=net.minecraft.nbt.CompressedStreamTools.read(new java.io.File(mc.mcDataDir,"servers.dat"));
                net.minecraft.nbt.NBTTagList servers=disk.getTagList("servers",10);
                if(servers.tagCount()!=menu.getServerList().countServers()-1)throw new IllegalStateException("Virtual pinned server was persisted");
            }catch(java.io.IOException e){throw new IllegalStateException(e);}
            System.out.println("[KC PARITY] Pinned server is first, starred, non-movable and not persisted");
        }
        if (frame == 57) capture("01c-pinned-server.png");
        if (frame == 60) {
            worldRequested = true;
            mc.launchIntegratedServer("New World", "New World", null);
        }
        if (!worldRequested || mc.thePlayer == null || mc.theWorld == null) return;
        // The isolated fixture owns this input; focus changes must not release it between frames.
        if(frame>=380&&frame<=440)
            net.minecraft.client.settings.KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(),true);
        if(frame>=442&&frame<=455) {
            net.minecraft.client.settings.KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(),true);
            net.minecraft.client.settings.KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(),true);
        }
        if (frame == 140) {
            CheatBreaker core = CheatBreaker.getInstance();
            java.util.UUID id=mc.thePlayer.getUniqueID();
            net.minecraft.scoreboard.Scoreboard board=mc.theWorld.getScoreboard();
            net.minecraft.scoreboard.ScoreObjective objective=board.getObjective("kazer_v21_hp");
            if(objective==null)objective=board.addScoreObjective("kazer_v21_hp",net.minecraft.scoreboard.IScoreObjectiveCriteria.DUMMY);
            objective.setDisplayName("Health");board.setObjectiveInDisplaySlot(2,objective);
            board.getValueFromObjective(mc.thePlayer.getName(),objective).setScorePoints(20);
            com.google.gson.JsonObject packet=new com.google.gson.JsonObject();
            packet.addProperty("@type","type.googleapis.com/lunarclient.apollo.nametag.v1.OverrideNametagMessage");
            com.google.gson.JsonObject uuid=new com.google.gson.JsonObject();uuid.addProperty("high64",Long.toUnsignedString(id.getMostSignificantBits()));
            uuid.addProperty("low64",Long.toUnsignedString(id.getLeastSignificantBits()));packet.add("player_uuid",uuid);
            com.google.gson.JsonArray lines=new com.google.gson.JsonArray();
            lines.add(new com.google.gson.JsonPrimitive("{\"text\":\"[Mod Mode]\",\"color\":\"gray\"}"));
            lines.add(new com.google.gson.JsonPrimitive("{\"text\":\""+mc.thePlayer.getName()+"\",\"color\":\"red\"}"));
            packet.add("adventure_json_lines",lines);apolloPacket(packet);
            if(core.netHandler.nametagOverride(id)==null||core.netHandler.nametagOverride(id).size()!=2)
                throw new IllegalStateException("Official Apollo custom payload did not reach nametag state");
            mc.displayGuiScreen(new CBModulesGui());
        }
        if (frame == 160) capture("02-hud-editor.png");
        if (frame == 180) mc.displayGuiScreen(new KazerModulesGui());
        if (frame == 200) capture("03-modules.png");
        if (frame == 220) {
            mc.playerController.setGameType(net.minecraft.world.WorldSettings.GameType.SURVIVAL);
            CheatBreaker.getInstance().moduleManager.nametagModule.thirdPersonNametag.setValue(false);
            mc.displayGuiScreen(new GuiInventory(mc.thePlayer));
        }
        if (frame == 240) capture("04-inventory-nametag.png");
        if (frame == 245) {
            mc.playerController.setGameType(net.minecraft.world.WorldSettings.GameType.CREATIVE);
            net.minecraft.client.gui.inventory.GuiContainerCreative gui=new net.minecraft.client.gui.inventory.GuiContainerCreative(mc.thePlayer);
            mc.displayGuiScreen(gui);
            try {
                net.minecraftforge.fml.relauncher.ReflectionHelper.findMethod(net.minecraft.client.gui.inventory.GuiContainerCreative.class,gui,
                    new String[]{"setCurrentCreativeTab","func_147050_b"},net.minecraft.creativetab.CreativeTabs.class)
                    .invoke(gui,net.minecraft.creativetab.CreativeTabs.tabInventory);
            }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
        }
        if (frame == 255) capture("04b-creative-inventory-nametag.png");
        if (frame == 260) {
            mc.displayGuiScreen(null); mc.gameSettings.thirdPersonView = 1;
            mc.playerController.setGameType(net.minecraft.world.WorldSettings.GameType.SURVIVAL);
            CheatBreaker.getInstance().moduleManager.nametagModule.thirdPersonNametag.setValue(true);
        }
        if (frame == 280) capture("05-third-person-nametag.png");
        if (frame == 300) {
            mc.thePlayer.rotationPitch = 70F;
        }
        if (frame == 320) capture("06-pitched-nametag.png");
        if (frame == 340) { mc.gameSettings.thirdPersonView = 2; mc.thePlayer.rotationPitch = 10F; }
        if (frame == 360) capture("07-front-nametag.png");
        if (frame == 380) {
            mc.thePlayer.inventory.setInventorySlotContents(mc.thePlayer.inventory.currentItem,
                    new net.minecraft.item.ItemStack(net.minecraft.init.Items.diamond_sword));
            final java.util.UUID playerId=mc.thePlayer.getUniqueID();
            final int slot=mc.thePlayer.inventory.currentItem;
            mc.getIntegratedServer().addScheduledTask(new Runnable(){@Override public void run(){
                net.minecraft.entity.player.EntityPlayerMP player=mc.getIntegratedServer().getConfigurationManager().getPlayerByUUID(playerId);
                player.inventory.setInventorySlotContents(slot,new net.minecraft.item.ItemStack(net.minecraft.init.Items.diamond_sword));
                player.inventoryContainer.detectAndSendChanges();
            }});
            net.minecraft.client.settings.KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), true);
            mc.thePlayer.swingItem();
        }
        if (frame == 400) {
            int realUseCount=mc.thePlayer.getItemInUseCount();
            if (!mc.thePlayer.isBlocking()
                    || cn.kazer.client189.animation.OldAnimationsAdapter.renderedUseCount(mc.thePlayer,mc.thePlayer.getHeldItem())!=1
                    || mc.thePlayer.getItemInUseCount() != realUseCount)
                throw new IllegalStateException("Block-hit must preserve native item use; held="
                    +mc.thePlayer.getHeldItem()+", key="+mc.gameSettings.keyBindUseItem.isKeyDown()+", screen="+mc.currentScreen);
            net.minecraft.client.entity.EntityOtherPlayerMP other = new net.minecraft.client.entity.EntityOtherPlayerMP(
                    mc.theWorld, new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "OtherPlayer"));
            net.minecraft.client.model.ModelBiped otherModel = new net.minecraft.client.model.ModelBiped();
            otherModel.heldItemRight = 1;
            if (other.isUsingItem() || other.getItemInUseCount()!=0 || otherModel.heldItemRight != 1)
                throw new IllegalStateException("Local block-hit changed another player's rendering");
            System.out.println("[KC PARITY] Block-hit is visual-only; remote player use count and arm pose unchanged");
            capture("08-third-person-block-hit.png");
        }
        if (frame == 420) { mc.gameSettings.thirdPersonView = 0; mc.thePlayer.rotationPitch = 75F; mc.thePlayer.swingItem(); }
        if (frame == 440) {
            if(mc.thePlayer.isSprinting()&&!mc.thePlayer.capabilities.isFlying)throw new IllegalStateException("Native item use bypassed sprint guard");
            if (mc.objectMouseOver == null || mc.objectMouseOver.typeOfHit != net.minecraft.util.MovingObjectPosition.MovingObjectType.BLOCK
                    || cn.kazer.client189.animation.OldAnimationsAdapter.renderedUseCount(mc.thePlayer,mc.thePlayer.getHeldItem()) != 1)
                throw new IllegalStateException("Block-hit against blocks was not active");
            checkBlockClick();
            capture("09-first-person-block-hit.png");
            net.minecraft.client.settings.KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
            checkConfigRoundTrip();
        }
        if (frame == 460) {
            mc.displayGuiScreen(new cn.kazer.client189.ui.KazerResourcePacks(new KazerModulesGui()));
        }
        if(frame==442||frame==448) {
            net.minecraft.item.ItemStack use=new net.minecraft.item.ItemStack(frame==442?net.minecraft.init.Items.golden_apple:net.minecraft.init.Items.potionitem);
            mc.thePlayer.inventory.setInventorySlotContents(mc.thePlayer.inventory.currentItem,use);mc.thePlayer.setItemInUse(use,32);
            // The fixture switches faster than the native equip fade; use the new
            // renderer stack immediately so these screenshots test the actual item.
            net.minecraftforge.fml.relauncher.ReflectionHelper.setPrivateValue(net.minecraft.client.renderer.ItemRenderer.class,
                    mc.entityRenderer.itemRenderer,use,"itemToRender","field_78453_b");
            net.minecraft.client.settings.KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(),true);
            net.minecraft.client.settings.KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(),true);
        }
        if(frame==447)capture("19-first-person-eating.png");
        if(frame==453)capture("20-first-person-drinking.png");
        if(frame==455){mc.thePlayer.stopUsingItem();net.minecraft.client.settings.KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(),false);net.minecraft.client.settings.KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(),false);}
        if(frame==456) {
            net.minecraft.item.ItemStack bow=new net.minecraft.item.ItemStack(net.minecraft.init.Items.bow);
            mc.thePlayer.inventory.setInventorySlotContents(mc.thePlayer.inventory.currentItem,bow);mc.thePlayer.setItemInUse(bow,71980);
            net.minecraftforge.fml.relauncher.ReflectionHelper.setPrivateValue(net.minecraft.client.renderer.ItemRenderer.class,
                    mc.entityRenderer.itemRenderer,bow,"itemToRender","field_78453_b");
            if(cn.kazer.client189.animation.OldAnimationsAdapter.renderedUseCount(mc.thePlayer,bow)!=1
                    ||mc.thePlayer.getItemInUseCount()!=71980)throw new IllegalStateException("Bow timer modified by renderer");
        }
        if(frame==457)capture("23-oldanimations-bow-pullback.png");
        if(frame==458) {
            mc.thePlayer.stopUsingItem();net.minecraft.item.ItemStack rod=new net.minecraft.item.ItemStack(net.minecraft.init.Items.fishing_rod);
            mc.thePlayer.inventory.setInventorySlotContents(mc.thePlayer.inventory.currentItem,rod);
            net.minecraftforge.fml.relauncher.ReflectionHelper.setPrivateValue(net.minecraft.client.renderer.ItemRenderer.class,
                    mc.entityRenderer.itemRenderer,rod,"itemToRender","field_78453_b");
        }
        if(frame==459)capture("24-oldanimations-rod.png");
        if (frame == 480) capture("10-resource-packs.png");
        if (frame == 500) {
            KazerModulesGui settings = new KazerModulesGui();
            try {
                java.lang.reflect.Field page = KazerModulesGui.class.getDeclaredField("page"); page.setAccessible(true);
                page.set(settings, java.lang.Enum.valueOf((Class)page.getType(), "SETTINGS"));
            } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
            mc.displayGuiScreen(settings);
        }
        if (frame == 520) capture("11-settings.png");
        if (frame == 540) {
            mc.displayGuiScreen(null); mc.thePlayer.rotationPitch = 15F;
            CheatBreaker.getInstance().moduleManager.motionBlurModule.setState(true);
        }
        int modeFrame = frame - 540;
        if(modeFrame>=0&&modeFrame<160)mc.thePlayer.rotationYaw+=.5F;
        if (modeFrame >= 0 && modeFrame < 160 && modeFrame % 40 == 0) {
            com.cheatbreaker.client.module.type.motionblur.MotionBlurModule blur = CheatBreaker.getInstance().moduleManager.motionBlurModule;
            for (com.cheatbreaker.client.config.Setting setting : blur.getSettingsList())
                if (setting.getLabel().equals("Mode")) setting.setValue(
                        com.cheatbreaker.client.module.type.motionblur.MotionBlurMode.values()[modeFrame / 40].label(), false);
        }
        if (modeFrame >= 0 && modeFrame < 160 && modeFrame % 40 == 20)
            capture("12-blur-" + (modeFrame / 40) + ".png");
        if (frame == 710) {
            CheatBreaker.getInstance().moduleManager.motionBlurModule.setState(false);
            com.google.gson.JsonObject title=new com.google.gson.JsonObject();
            title.addProperty("@type","type.googleapis.com/lunarclient.apollo.title.v1.DisplayTitleMessage");
            title.addProperty("title_type","TITLE_TYPE_TITLE");title.addProperty("scale",1F);
            title.addProperty("adventure_json_message","{\"text\":\"KAZER 1.8.9\",\"color\":\"aqua\"}");
            title.addProperty("display_time","10s");apolloPacket(title);
        }
        if(frame==730)capture("13-apollo-title.png");
        if(frame==750)checkBlurCard();
        if(frame==770) {
            CheatBreaker.getInstance().getTitleManager().lIIIIlIIllIIlIIlIIIlIIllI().clear();
            KazerModulesGui visuals=new KazerModulesGui();mc.displayGuiScreen(visuals);
            try { java.lang.reflect.Field selected=KazerModulesGui.class.getDeclaredField("selectedModule");
                selected.setAccessible(true);selected.set(visuals,CheatBreaker.getInstance().moduleManager.blockHitAnimation);
            } catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
        }
        if(frame==790)capture("14-legacy-visuals.png");
        if(frame==810) {
            try { java.lang.reflect.Field scroll=KazerModulesGui.class.getDeclaredField("settingsScroll");
                scroll.setAccessible(true);scroll.setInt(mc.currentScreen,110);
            } catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
        }
        if(frame==830)capture("15-legacy-visuals-items.png");
        if(frame==850) {
            checkVisualSettings();
            checkNativeOptions();
            System.out.println("[KC PARITY] Completed isolated UI, config, Apollo, four blur modes, card toggle and non-destructive block-hit sequence");
            mc.displayGuiScreen(new net.minecraft.client.gui.GuiIngameMenu());
            if(!(mc.currentScreen instanceof cn.kazer.client189.ui.KazerPauseMenu))
                throw new IllegalStateException("Pause menu did not match 1.7");
        }
        if(frame==870)capture("16-pause-menu.png");
        if(frame==880) {
            checkChatHit();checkGlCache();
            verifyTabBadgeColors();
            openIconSearch("1.7 Visuals");
        }
        if(frame==900)capture("17-visuals-icon-search.png");
        if(frame==910)openIconSearch("Chat");
        if(frame==930)capture("18-chat-icon-search.png");
        if(frame==930) {
            net.minecraft.client.gui.GuiChat chat=new net.minecraft.client.gui.GuiChat();mc.displayGuiScreen(chat);
            net.minecraft.client.gui.GuiTextField input=net.minecraftforge.fml.relauncher.ReflectionHelper.getPrivateValue(
                net.minecraft.client.gui.GuiChat.class,chat,"inputField","field_146415_a");
            input.textboxKeyTyped('\u4e2d',0);input.textboxKeyTyped('\u6587',0);
            if(!"\u4e2d\u6587".equals(input.getText()))throw new IllegalStateException("Unicode commit rejected");
            cn.kazer.client189.input.ImeHooks.textField(input);
            System.out.println("[KC PARITY] Chinese commit and native candidate positioning completed");
        }
        if(frame==934)capture("22-chat-chinese.png");
        if(frame==935) {
            com.cheatbreaker.client.nethandler.apollo.ApolloProtocol.Command tag=new com.cheatbreaker.client.nethandler.apollo.ApolloProtocol.Command();
            tag.kind=com.cheatbreaker.client.nethandler.apollo.ApolloProtocol.Kind.NAMETAG;tag.player=mc.thePlayer.getUniqueID();
            tag.lines=java.util.Collections.singletonList(mc.thePlayer.getName());CheatBreaker.getInstance().netHandler.getApollo().apply(tag);
            mc.theWorld.getScoreboard().setObjectiveInDisplaySlot(2,null);mc.displayGuiScreen(new GuiInventory(mc.thePlayer));
        }
        if(frame==945)capture("21-single-inventory-nametag.png");
        if(frame==950){System.out.println("[KC PARITY] V23_SMOKE_OK: sprint guard, inventory without labels, version footer, IME adapters and UI checks completed");mc.shutdown();}
    }
    private static void checkChatHit() {
        Minecraft mc=Minecraft.getMinecraft();
        com.cheatbreaker.client.module.type.ChatModule module=CheatBreaker.getInstance().moduleManager.chatModule;
        float oldX=module.getXTranslation(),oldY=module.getYTranslation();Object oldScale=module.scale.getValue();
        try {
            module.setTranslations(90,-85);module.scale.setValue(1.5F,false);
            mc.displayGuiScreen(new net.minecraft.client.gui.GuiChat());
            net.minecraft.util.ChatComponentText line=new net.minecraft.util.ChatComponentText("Click Here");
            line.getChatStyle().setChatClickEvent(new net.minecraft.event.ClickEvent(net.minecraft.event.ClickEvent.Action.SUGGEST_COMMAND,"/test-v22"));
            mc.ingameGUI.getChatGUI().printChatMessage(line);module.updateChatDimensions();
            net.minecraft.client.gui.ScaledResolution res=new net.minecraft.client.gui.ScaledResolution(mc);
            float[] points=module.getScaledPoints(res,true);float scale=((Number)module.scale.getValue()).floatValue();
            int x=Math.round(((points[0]+2)*scale+5)*res.getScaleFactor());
            int y=Math.round((res.getScaledHeight()-(points[1]+module.getHeight())*scale+4)*res.getScaleFactor());
            net.minecraft.util.IChatComponent hit=mc.ingameGUI.getChatGUI().getChatComponent(x,y);
            if(hit==null||hit.getChatStyle().getChatClickEvent()==null
                    ||!"/test-v22".equals(hit.getChatStyle().getChatClickEvent().getValue()))throw new IllegalStateException("Moved/scaled chat click missed");
            if(mc.ingameGUI.getChatGUI().getChatComponent(8*res.getScaleFactor(),31*res.getScaleFactor())!=null)
                throw new IllegalStateException("Old chat coordinates are still clickable");
            System.out.println("[KC PARITY] Moved chat with 1.5 scale preserves clickable server components; old location inactive");
        }finally{module.setTranslations(oldX,oldY);module.scale.setValue(oldScale,false);mc.displayGuiScreen(null);}
    }
    private static void checkGlCache() {
        int blend=org.lwjgl.opengl.GL11.glIsEnabled(3042)?1:0,depth=org.lwjgl.opengl.GL11.glIsEnabled(2929)?1:0;
        cn.kazer.client189.compat.LegacyGL.glPushAttrib(org.lwjgl.opengl.GL11.GL_ALL_ATTRIB_BITS);
        net.minecraft.client.renderer.GlStateManager.enableBlend();net.minecraft.client.renderer.GlStateManager.disableDepth();
        cn.kazer.client189.compat.LegacyGL.glPopAttrib();
        if((org.lwjgl.opengl.GL11.glIsEnabled(3042)?1:0)!=blend||(org.lwjgl.opengl.GL11.glIsEnabled(2929)?1:0)!=depth)
            throw new IllegalStateException("Attribute scope did not restore driver state");
        for(int mode=0;mode<2;mode++) {
            long start=System.nanoTime();
            for(int i=0;i<500;i++) {
                if(mode==0){org.lwjgl.opengl.GL11.glPushAttrib(org.lwjgl.opengl.GL11.GL_ALL_ATTRIB_BITS);org.lwjgl.opengl.GL11.glPopAttrib();cn.kazer.client189.compat.LegacyGL.sync();}
                else{cn.kazer.client189.compat.LegacyGL.glPushAttrib(org.lwjgl.opengl.GL11.GL_ALL_ATTRIB_BITS);cn.kazer.client189.compat.LegacyGL.glPopAttrib();}
            }
            System.out.println("[KC PERF] "+(mode==0?"GPU query fallback":"CPU cache snapshot")+" 500 scopes ms="+(System.nanoTime()-start)/1000000D);
        }
    }
    private static void verifyTabBadgeColors() {
        CheatBreaker core=CheatBreaker.getInstance();
        try {
            java.lang.reflect.Field online=core.getPresenceClient().getClass().getDeclaredField("sameServerUsers");
            online.setAccessible(true);Object previous=online.get(core.getPresenceClient());
            try {
                java.util.Map<java.util.UUID,Integer> peers=new java.util.HashMap<>();
                java.util.UUID owner=com.cheatbreaker.client.util.KazerBadgeColors.CANRX_UUID;
                peers.put(owner,0xAA0000);
                online.set(core.getPresenceClient(),peers);
                String red=RuntimeHooks.tabName("CANRX",new net.minecraft.client.network.NetworkPlayerInfo(
                        new com.mojang.authlib.GameProfile(owner,"CANRX")));
                Minecraft mc=Minecraft.getMinecraft();
                String white=RuntimeHooks.tabName(mc.thePlayer.getName(),new net.minecraft.client.network.NetworkPlayerInfo(
                        new com.mojang.authlib.GameProfile(mc.thePlayer.getUniqueID(),mc.thePlayer.getName())));
                java.lang.reflect.Field colors=RuntimeHooks.class.getDeclaredField("TAB_COLORS");colors.setAccessible(true);
                java.util.Map map=(java.util.Map)colors.get(null);
                if(!Integer.valueOf(0xAA0000).equals(map.get(red))||!Integer.valueOf(0xFFFFFF).equals(map.get(white)))
                    throw new IllegalStateException("TAB badge colors leaked between rows");
            } finally {online.set(core.getPresenceClient(),previous);}
            System.out.println("[KC PARITY] TAB keeps each UUID's badge color after another row is measured");
        } catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
    }
    private static void openIconSearch(String query) {
        Minecraft mc=Minecraft.getMinecraft();KazerModulesGui gui=new KazerModulesGui();mc.displayGuiScreen(gui);
        try {
            java.lang.reflect.Field field=KazerModulesGui.class.getDeclaredField("search");field.setAccessible(true);
            ((net.minecraft.client.gui.GuiTextField)field.get(gui)).setText(query);
            java.lang.reflect.Field left=KazerModulesGui.class.getDeclaredField("left");left.setAccessible(true);
            java.lang.reflect.Field top=KazerModulesGui.class.getDeclaredField("top");top.setAccessible(true);
            org.lwjgl.input.Mouse.setCursorPosition((left.getInt(gui)+145)*2,mc.displayHeight-(top.getInt(gui)+90)*2);
        } catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
    }
    private static void checkVisualSettings() {
        KazerModulesGui gui=(KazerModulesGui)Minecraft.getMinecraft().currentScreen;
        cn.kazer.client189.animation.BlockHitModule m=CheatBreaker.getInstance().moduleManager.blockHitAnimation;
        try {
            java.lang.reflect.Field left=KazerModulesGui.class.getDeclaredField("left");left.setAccessible(true);
            java.lang.reflect.Field top=KazerModulesGui.class.getDeclaredField("top");top.setAccessible(true);
            java.lang.reflect.Field scroll=KazerModulesGui.class.getDeclaredField("settingsScroll");scroll.setAccessible(true);scroll.setInt(gui,0);
            java.lang.reflect.Method click=KazerModulesGui.class.getDeclaredMethod("handleSettingsClick",int.class,int.class);click.setAccessible(true);
            int x=left.getInt(gui)+108+10,y=top.getInt(gui)+40+8+58;
            click.invoke(gui,x+8,y+8);
            if(m.flag(m.animations)||m.flag(m.redArmor))throw new IllegalStateException("Animations switch did not hide/disable children");
            click.invoke(gui,x+8,y+8);
            if(!m.flag(m.animations)||!m.flag(m.redArmor))throw new IllegalStateException("Animations switch did not restore children");
            click.invoke(gui,x+8,y+21+8);
            if(m.flag(m.redArmor))throw new IllegalStateException("Upstream toggle did not turn off");
            m.load();if(m.flag(m.redArmor))throw new IllegalStateException("Upstream option was not persisted");
            click.invoke(gui,x+8,y+21+8);
            if(!m.flag(m.redArmor))throw new IllegalStateException("Upstream toggle did not turn on");
            System.out.println("[KC PARITY] Group master switches, version switches, hit testing and isolated persistence passed");
        } catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
    }
    private static void checkNativeOptions() {
        Minecraft mc=Minecraft.getMinecraft();int oldKey=mc.gameSettings.keyBindForward.getKeyCode(),oldDistance=mc.gameSettings.renderDistanceChunks;
        try {
            mc.gameSettings.keyBindForward.setKeyCode(37);mc.gameSettings.renderDistanceChunks=5;mc.gameSettings.saveOptions();
            java.io.File shared=new java.io.File(mc.mcDataDir,"config/KazerClient/game-options.cfg");
            String data=new String(java.nio.file.Files.readAllBytes(shared.toPath()),java.nio.charset.StandardCharsets.UTF_8);
            if(!data.contains("mc/key_key.forward:37")||!data.contains("mc/renderDistance:5"))throw new IllegalStateException("Native settings export hook did not run");
            mc.gameSettings.keyBindForward.setKeyCode(17);mc.gameSettings.renderDistanceChunks=8;mc.gameSettings.loadOptions();
            if(mc.gameSettings.keyBindForward.getKeyCode()!=37||mc.gameSettings.renderDistanceChunks!=5)
                throw new IllegalStateException("Native settings import hook did not run");
            System.out.println("[KC PARITY] Native key/options save+load exported and imported shared preferences");
        }catch(java.io.IOException error){throw new IllegalStateException(error);}
        finally {mc.gameSettings.keyBindForward.setKeyCode(oldKey);mc.gameSettings.renderDistanceChunks=oldDistance;mc.gameSettings.saveOptions();
            net.minecraft.client.settings.KeyBinding.resetKeyBindingArrayAndHash();}
    }
    private static void checkBlockClick() {
        Minecraft mc=Minecraft.getMinecraft();
        net.minecraft.util.MovingObjectPosition target=mc.objectMouseOver;
        net.minecraft.block.state.IBlockState original=mc.theWorld.getBlockState(target.getBlockPos());
        try {
            if(!cn.kazer.client189.animation.OldAnimationsAdapter.cancelBlockClick())throw new IllegalStateException("Block-click guard inactive");
            checkNativeUseAndDisable(target);
            net.minecraftforge.fml.relauncher.ReflectionHelper.findMethod(Minecraft.class,mc,
                new String[]{"clickMouse","func_147116_af"}).invoke(mc);
            net.minecraftforge.fml.relauncher.ReflectionHelper.findMethod(Minecraft.class,mc,
                new String[]{"sendClickBlockToController","func_147115_a"},boolean.class).invoke(mc,true);
            if(mc.playerController.getIsHittingBlock()||!original.equals(mc.theWorld.getBlockState(target.getBlockPos())))
                throw new IllegalStateException("Visual block-hit started breaking a block");
            net.minecraft.client.settings.KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(),true);
            cn.kazer.client189.animation.OldAnimationsAdapter.tick();
            net.minecraft.client.entity.EntityOtherPlayerMP remote=new net.minecraft.client.entity.EntityOtherPlayerMP(
                mc.theWorld,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"OtherPlayer"));
            mc.objectMouseOver=new net.minecraft.util.MovingObjectPosition(remote);
            if(cn.kazer.client189.animation.OldAnimationsAdapter.cancelBlockClick())throw new IllegalStateException("Normal entity attacks were suppressed");
            if(remote.swingProgress!=0||remote.isUsingItem())throw new IllegalStateException("Local visual state leaked to remote player");
            mc.objectMouseOver=null;
            for(int i=0;i<8;i++)cn.kazer.client189.animation.OldAnimationsAdapter.tick();
            if(cn.kazer.client189.animation.OldAnimationsAdapter.cancelBlockClick()
                    ||cn.kazer.client189.animation.OldAnimationsAdapter.useSwing(0F,0F)!=mc.thePlayer.getSwingProgress(0F))
                throw new IllegalStateException("Air-target sword swing loop is active");
            System.out.println("[KC PARITY] Holding both buttons into air does not create a cosmetic sword swing loop");
            System.out.println("[KC PARITY] Native block click/dig cancelled; block unchanged; entity attacks and remote swing retained");
            mc.objectMouseOver=target;
            net.minecraft.item.ItemStack sword=mc.thePlayer.getHeldItem();
            try {for(net.minecraft.item.Item item:new net.minecraft.item.Item[]{net.minecraft.init.Items.golden_apple,net.minecraft.init.Items.potionitem}) {
                net.minecraft.item.ItemStack held=new net.minecraft.item.ItemStack(item);
                mc.thePlayer.inventory.setInventorySlotContents(mc.thePlayer.inventory.currentItem,held);
                mc.thePlayer.setItemInUse(held,32);
                cn.kazer.client189.animation.OldAnimationsAdapter.tick();cn.kazer.client189.animation.OldAnimationsAdapter.tick();
                if(!cn.kazer.client189.animation.OldAnimationsAdapter.cancelBlockClick()
                        ||cn.kazer.client189.animation.OldAnimationsAdapter.useSwing(0F,.5F)<=0F
                        ||cn.kazer.client189.animation.OldAnimationsAdapter.renderedUseCount(mc.thePlayer,held)!=1
                        ||mc.thePlayer.getItemInUseCount()!=32)
                    throw new IllegalStateException("Eating/drinking block-target swing inactive");
                net.minecraftforge.fml.relauncher.ReflectionHelper.findMethod(Minecraft.class,mc,
                        new String[]{"clickMouse","func_147116_af"}).invoke(mc);
                if(mc.playerController.getIsHittingBlock()||!original.equals(mc.theWorld.getBlockState(target.getBlockPos())))
                    throw new IllegalStateException("Eating swing damaged a block");
            }}finally{mc.thePlayer.stopUsingItem();mc.thePlayer.inventory.setInventorySlotContents(mc.thePlayer.inventory.currentItem,sword);}
            System.out.println("[KC PARITY] Golden apple and potion swing locally against blocks without digging");
        } catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
        finally {mc.objectMouseOver=target;net.minecraft.client.settings.KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(),false);}
    }
    private static void checkNativeUseAndDisable(net.minecraft.util.MovingObjectPosition target) throws ReflectiveOperationException {
        Minecraft mc=Minecraft.getMinecraft();
        cn.kazer.client189.animation.BlockHitModule m=CheatBreaker.getInstance().moduleManager.blockHitAnimation;
        net.minecraft.item.ItemStack sword=mc.thePlayer.getHeldItem();
        mc.thePlayer.stopUsingItem();
        if(cn.kazer.client189.animation.OldAnimationsAdapter.renderedUseCount(mc.thePlayer,sword)!=0
                ||cn.kazer.client189.animation.OldAnimationsAdapter.cancelBlockClick())
            throw new IllegalStateException("Held right button manufactured item use");
        for(net.minecraft.item.Item food:new net.minecraft.item.Item[]{net.minecraft.init.Items.bread,net.minecraft.init.Items.golden_apple}) {
            net.minecraft.item.ItemStack held=new net.minecraft.item.ItemStack(food);
            mc.thePlayer.inventory.setInventorySlotContents(mc.thePlayer.inventory.currentItem,held);
            if(cn.kazer.client189.animation.OldAnimationsAdapter.renderedUseCount(mc.thePlayer,held)!=0)
                throw new IllegalStateException("Idle food manufactured consume animation");
        }
        mc.thePlayer.inventory.setInventorySlotContents(mc.thePlayer.inventory.currentItem,sword);
        // Simulate an already active dig without damaging any fixture block.
        net.minecraftforge.fml.relauncher.ReflectionHelper.setPrivateValue(net.minecraft.client.multiplayer.PlayerControllerMP.class,
                mc.playerController,true,"isHittingBlock","field_78778_j");
        net.minecraftforge.fml.relauncher.ReflectionHelper.findMethod(Minecraft.class,mc,
                new String[]{"rightClickMouse","func_147121_ag"}).invoke(mc);
        if(mc.playerController.getIsHittingBlock()||!mc.thePlayer.isUsingItem())
            throw new IllegalStateException("Native right click failed to abort digging/start genuine item use");
        m.setState(false);
        try {
            int count=mc.thePlayer.getItemInUseCount();
            if(cn.kazer.client189.animation.OldAnimationsAdapter.renderedUseCount(mc.thePlayer,sword)!=count
                    ||cn.kazer.client189.animation.OldAnimationsAdapter.useSwing(.25F,.5F)!=.25F
                    ||cn.kazer.client189.animation.OldAnimationsAdapter.skipNoneTransform()
                    ||cn.kazer.client189.animation.OldAnimationsAdapter.blockingArmYaw(-.5235988F)!=-.5235988F
                    ||cn.kazer.client189.animation.OldAnimationsAdapter.healthFlash(true)!=true
                    ||cn.kazer.client189.animation.OldAnimationsAdapter.redArmor())
                throw new IllegalStateException("Disabled module did not restore native animation branches");
        } finally {m.setState(true);}
        int count=mc.thePlayer.getItemInUseCount();float equipped=.6F;
        if(Math.abs(cn.kazer.client189.animation.OldAnimationsAdapter.equipClamp(-.6F,-.4F,.4F,sword,
                mc.thePlayer.inventory.currentItem,equipped)-.4F)>.00001F ||mc.thePlayer.getItemInUseCount()!=count)
            throw new IllegalStateException("Same-slot switch animation differs from upstream");
        mc.objectMouseOver=target;
        System.out.println("[KC PARITY] V25: genuine native use, dig-to-use transition, idle food, full disable and item switching passed");
    }
    private static void checkBlurCard() {
        Minecraft mc=Minecraft.getMinecraft();KazerModulesGui gui=new KazerModulesGui();mc.displayGuiScreen(gui);
        com.cheatbreaker.client.module.type.motionblur.MotionBlurModule blur=CheatBreaker.getInstance().moduleManager.motionBlurModule;
        try {
            java.lang.reflect.Field search=KazerModulesGui.class.getDeclaredField("search");search.setAccessible(true);
            ((net.minecraft.client.gui.GuiTextField)search.get(gui)).setText("Motion Blur");
            java.lang.reflect.Field left=KazerModulesGui.class.getDeclaredField("left");left.setAccessible(true);
            java.lang.reflect.Field top=KazerModulesGui.class.getDeclaredField("top");top.setAccessible(true);
            java.lang.reflect.Method click=KazerModulesGui.class.getDeclaredMethod("handleModuleClick",int.class,int.class);click.setAccessible(true);
            int x=left.getInt(gui)+108+9+30,y=top.getInt(gui)+40+31+108-8;
            for(com.cheatbreaker.client.module.type.motionblur.MotionBlurMode mode:com.cheatbreaker.client.module.type.motionblur.MotionBlurMode.values()) {
                for(com.cheatbreaker.client.config.Setting setting:blur.getSettingsList())if(setting.getLabel().equals("Mode"))setting.setValue(mode.label(),false);
                blur.setState(false);click.invoke(gui,x,y);
                if(!blur.isEnabled())throw new IllegalStateException("Blur card did not enable "+mode);
                click.invoke(gui,x,y);
                if(blur.isEnabled()||com.cheatbreaker.client.module.type.motionblur.MotionBlurModule.getActive()!=null)
                    throw new IllegalStateException("Blur card did not disable "+mode);
            }
            System.out.println("[KC PARITY] Actual module card enabled/disabled all four blur modes; active blur cleared");
        }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
    }
    private static void capture(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(mc.mcDataDir, name, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        System.out.println("[KC PARITY] Screenshot " + name);
        int error=org.lwjgl.opengl.GL11.glGetError();
        if(error!=org.lwjgl.opengl.GL11.GL_NO_ERROR)throw new IllegalStateException("OpenGL error after "+name+": "+error);
    }
    private static void apolloPacket(com.google.gson.JsonObject packet) {
        byte[] bytes=packet.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Minecraft.getMinecraft().getNetHandler().handleCustomPayload(new net.minecraft.network.play.server.S3FPacketCustomPayload(
                com.cheatbreaker.client.nethandler.apollo.ApolloProtocol.JSON_CHANNEL,
                new net.minecraft.network.PacketBuffer(io.netty.buffer.Unpooled.wrappedBuffer(bytes))));
    }
    private static void checkConfigRoundTrip() {
        CheatBreaker core = CheatBreaker.getInstance();
        com.cheatbreaker.client.module.AbstractModule fps = core.moduleManager.fpsModule;
        fps.setTranslations(37F, 49F);
        core.configManager.write();
        fps.setTranslations(0F, 0F);
        core.configManager.read();
        if (fps.getXTranslation() != 37F || fps.getYTranslation() != 49F)
            throw new IllegalStateException("Shared HUD configuration did not round-trip");
        if (!core.moduleManager.blockHitAnimation.isEnabled())
            throw new IllegalStateException("Shared profile overwrote 1.8-only block-hit preferences");
        System.out.println("[KC PARITY] Shared profile and isolated animation configuration round-trip passed");
    }
}
