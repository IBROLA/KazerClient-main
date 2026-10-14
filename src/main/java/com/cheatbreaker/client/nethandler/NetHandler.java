package com.cheatbreaker.client.nethandler;

import com.cheatbreaker.client.CheatBreaker;
import com.cheatbreaker.client.event.type.PluginMessageEvent;
import com.cheatbreaker.client.module.AbstractModule;
import com.cheatbreaker.client.module.staff.StaffModule;
import com.cheatbreaker.client.module.type.cooldowns.CooldownsModule;
import com.cheatbreaker.client.nethandler.client.ICBNetHandlerClient;
import com.cheatbreaker.client.nethandler.server.*;
import com.cheatbreaker.client.util.hologram.Hologram;
import com.cheatbreaker.client.util.title.Title;
import com.cheatbreaker.client.nethandler.apollo.ApolloProtocol;
import com.cheatbreaker.client.nethandler.apollo.ApolloState;
import com.google.common.base.Charsets;
import com.google.gson.Gson;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.event.HoverEvent;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.util.ChatComponentStyle;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import java.util.*;
import java.util.List;

public class NetHandler implements ICBNetHandler, ICBNetHandlerClient {

    @Getter private String world = "";
    private boolean competitiveGamemode = false;
    private boolean lIIIIIIIIIlIllIIllIlIIlIl = false;

    @Getter
    private Map<UUID, List<String>> nametagsMap = new HashMap<>();
    @Getter private final ApolloState apollo = new ApolloState();

    public NetHandler() { }

    public void onJoinGame(boolean firstJoin) {
        if (firstJoin) apollo.disconnect();
        else apollo.serverSwitch();
        initialize();
    }
    public void onDisconnect() {
        apollo.disconnect();
        nametagsMap.clear();
        lIIIIlIIllIIlIIlIIIlIIllI();
    }
    public List<String> nametagOverride(UUID player) {
        return apollo.nametags().containsKey(player) ? apollo.nametags().get(player) : nametagsMap.get(player);
    }

    private void initialize() {
        this.competitiveGamemode = false;
        this.world = "";
        this.nametagsMap = new HashMap<>();
        for (AbstractModule cBModule : CheatBreaker.getInstance().getModuleManager().staffModules) {
            ((StaffModule)cBModule).disableStaffModule();
        }
        CheatBreaker.getInstance().getBorderManager().lIIIIlIIllIIlIIlIIIlIIllI();
        Hologram.IIIllIllIlIlllllllIlIlIII().clear();
        this.lIIIIlIIllIIlIIlIIIlIIllI();
    }
    public void lIIIIlIIllIIlIIlIIIlIIllI() {
        CheatBreaker.getInstance().getTitleManager().lIIIIlIIllIIlIIlIIIlIIllI().clear();
//        IIIlllIllIIllIllIlIIIllII.lIIIIlIIllIIlIIlIIIlIIllI(null);
    }

    public void onPluginMessage(PluginMessageEvent pluginMessageEvent) {
        try {
            if (pluginMessageEvent.getChannel().equals(ApolloProtocol.CHANNEL)
                    || pluginMessageEvent.getChannel().equals(ApolloProtocol.JSON_CHANNEL)) {
                for (ApolloProtocol.Command command : ApolloProtocol.decode(pluginMessageEvent.getChannel(), pluginMessageEvent.getPayload()))
                    apollo.apply(command);
            } else if (pluginMessageEvent.getChannel().equals("REGISTER")) {
                String string = new String(pluginMessageEvent.getPayload(), Charsets.UTF_8);
                this.lIIIIIIIIIlIllIIllIlIIlIl = string.contains(CheatBreaker.getInstance().getPluginMessageChannel());
                PacketBuffer lIlIllllllllIlIIIllIIllII2 = new PacketBuffer(Unpooled.buffer());
                lIlIllllllllIlIIIllIIllII2.writeBytes(CheatBreaker.getInstance().getPluginMessageChannel().getBytes(Charsets.UTF_8));
                if (Minecraft.getMinecraft().getNetHandler() != null && this.lIIIIIIIIIlIllIIllIlIIlIl) {
                    Minecraft.getMinecraft().getNetHandler().addToSendQueue(new C17PacketCustomPayload("REGISTER", lIlIllllllllIlIIIllIIllII2));
                }
                // REGISTER can arrive repeatedly; it is not a world/connection reset.
            } else if (pluginMessageEvent.getChannel().equals(CheatBreaker.getInstance().getPluginMessageChannel())) {
                Packet packet = Packet.handle(this, pluginMessageEvent.getPayload());
                if (CheatBreaker.getInstance().getGlobalSettings().isDebug) {
                    ChatComponentText chatComponentText = new ChatComponentText( "[Kazer] ");
                    ChatComponentText chatComponentText2 = new ChatComponentText("Received: " + packet.getClass().getSimpleName());
                    chatComponentText2.getChatStyle().setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(new Gson().toJson(packet))));
                    chatComponentText.appendSibling(chatComponentText2);
                    Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(chatComponentText);
                }
            }
        }
        catch (AssertionError | Exception throwable) {
            org.apache.logging.log4j.LogManager.getLogger().debug("Ignored invalid plugin message on {}", pluginMessageEvent.getChannel(), throwable);
        }
    }

    @Override
    public void handleCooldown(PacketCooldown packet) {
        CooldownsModule.lIIIIlIIllIIlIIlIIIlIIllI(packet.getMessage(), packet.getDurationMs(), packet.getIconId());
    }

    @Override
    public void handleNotification(PacketNotification cBPacketNotification) {
        CheatBreaker.getInstance().getModuleManager().notifications.queueNotification(cBPacketNotification.getLevel(), cBPacketNotification.getLevel(), cBPacketNotification.getDurationMs());
    }

    @Override
    public void handleStaffModState(PacketStaffModState packet) {
        for (AbstractModule cBModule : CheatBreaker.getInstance().getModuleManager().staffModules) {
            if (!cBModule.getName().equals(packet.getMod().replaceAll("_", "").toLowerCase())) continue;
            cBModule.setState(packet.isState());
        }
    }

    @Override
    public void handleNametagsUpdate(PacketUpdateNametags packet) {
//        if (packet.getPlayersMap() != null) {
//            IIIlllIllIIllIllIlIIIllII.lIIIIlIIllIIlIIlIIIlIIllI(new HashMap());
//            for (Map.Entry<UUID, List<String>> entry : packet.getPlayersMap().entrySet()) {
//                IIIlllIllIIllIllIlIIIllII.lIllIllIlIIllIllIlIlIIlIl().put(entry.getKey().toString(), entry.getValue());
//            }
//        } else {
//            IIIlllIllIIllIllIlIIIllII.lIIIIlIIllIIlIIlIIIlIIllI(null);
//        }
    }

    @Override
    public void handleTeammates(PacketTeammates packet) {
        // Team view was removed from Kazer Client.
    }

    @Override
    public void handleOverrideNametags(PacketOverrideNametags packet) {
        if (packet.getTags() == null) {
            this.nametagsMap.remove(packet.getPlayer());
        } else {
            this.nametagsMap.put(packet.getPlayer(), Collections.unmodifiableList(new ArrayList<>(packet.getTags())));
        }
    }

    @Override
    public void handleAddHologram(PacketAddHologram var1) {
        Hologram iIIlIllIIllllIIIllllIllll = new Hologram(var1.getUuid(), var1.getX(), var1.getY(), var1.getZ());
        Hologram.IIIllIllIlIlllllllIlIlIII().add(iIIlIllIIllllIIIllllIllll);
        iIIlIllIIllllIIIllllIllll.lIIIIlIIllIIlIIlIIIlIIllI(var1.getLines().toArray(new String[0]));
    }

    @Override
    public void handleUpdateHologram(PacketUpdateHologram var1) {
        Hologram.IIIllIllIlIlllllllIlIlIII().stream().filter(hologram -> hologram.lIIIIIIIIIlIllIIllIlIIlIl().equals(var1.getUuid())).forEach(hologram -> hologram.lIIIIlIIllIIlIIlIIIlIIllI(var1.getLines().toArray(new String[0])));
    }

    @Override
    public void handleRemoveHologram(PacketRemoveHologram var1) {
        Hologram.IIIllIllIlIlllllllIlIlIII().removeIf(hologram -> hologram.lIIIIIIIIIlIllIIllIlIIlIl().equals(var1.getUuid()));
    }

    @Override
    public void handleTitle(PacketTitle packet) {
        Title.TitleType titleEnum = Title.TitleType.title;
        if (packet.getType().toLowerCase().equals("subtitle")) {
            titleEnum = Title.TitleType.subtitle;
        }
        CheatBreaker.getInstance().getTitleManager().lIIIIlIIllIIlIIlIIIlIIllI().add(new Title(packet.getMessage(), titleEnum, packet.getScale(), packet.getDisplayTimeMs(), packet.getFadeInTimeMs(), packet.getFadeOutTimeMs()));
    }

    @Override
    public void handleServerRule(PacketServerRule packetServerRule) {
        switch (packetServerRule.getRule()) {
            case COMPETITIVE_GAMEMODE: {
                this.competitiveGamemode = packetServerRule.isEnabled();
            }
            default:
                break;
        }
    }

    @Override
    public void handleUpdateWorld(PacketUpdateWorld var1) {
        System.out.println("[KC] World Update: " + var1.getWorld());
        this.world = var1.getWorld();
    }

    @Override
    public void handleServerUpdate(PacketServerUpdate packet) {
        System.out.println("[KC] Retrieved " + packet.getServer());
//        CBClient.getInstance().lIIIIlIIllIIlIIlIIIlIIllI(packet.getServer());
    }

    @Override
    public void handleWorldBorder(PacketWorldBorder packet) {
        CheatBreaker.getInstance().getBorderManager().lIIIIlIIllIIlIIlIIIlIIllI(packet.getId(), packet.getWorld(), packet.getColor(), packet.getMinX(), packet.getMinZ(), packet.getMaxX(), packet.getMaxZ(), packet.isCanShrinkExpand(), packet.isCancelsExit());
    }

    @Override
    public void handleWorldBorderUpdate(PacketWorldBorderUpdate packet) {
        CheatBreaker.getInstance().getBorderManager().lIIIIlIIllIIlIIlIIIlIIllI(packet.getId(), packet.getMinX(), packet.getMinZ(), packet.getMaxX(), packet.getMaxZ(), packet.getDurationTicks());
    }

    @Override
    public void handleWorldBorderRemove(PacketWorldBorderRemove packet) {
        CheatBreaker.getInstance().getBorderManager().lIIIIlIIllIIlIIlIIIlIIllI(packet.getId());
    }

    // Util Methods

    public void sendPacketToQueue(Packet packet) {
        Object object;
        if (packet != null && CheatBreaker.getInstance().getGlobalSettings().isDebug) {
            object = new ChatComponentText(EnumChatFormatting.RED + "[C" + EnumChatFormatting.WHITE + "B" + EnumChatFormatting.RED + "] " + EnumChatFormatting.RESET);
            ChatComponentText chatComponentText = new ChatComponentText(EnumChatFormatting.GRAY + "Sent: " + EnumChatFormatting.WHITE + packet.getClass().getSimpleName());
            chatComponentText.getChatStyle().setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(new Gson().toJson(packet))));
            ((ChatComponentStyle)object).appendSibling(chatComponentText);
            Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage((IChatComponent)object);
        }
        object = new C17PacketCustomPayload(CheatBreaker.getInstance().getPluginMessageChannel(), new net.minecraft.network.PacketBuffer(Unpooled.wrappedBuffer(Packet.getPacketData(packet))));
        Minecraft.getMinecraft().thePlayer.sendQueue.addToSendQueue((net.minecraft.network.Packet) object);
    }
}
