package cn.kazer.client189.runtime;

import com.cheatbreaker.client.CheatBreaker;
import com.cheatbreaker.client.ui.util.KazerUiIcons;
import net.minecraft.client.gui.*;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.nbt.*;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import java.util.*;

/** Virtual pinned rows share native list indices but never enter servers.dat. */
public final class KazerPinnedServers {
    private KazerPinnedServers() {}
    private static final class PinnedData extends ServerData {
        PinnedData(String name,String address){super(name,address,false);}
    }
    public static boolean isPinned(ServerData server){return server instanceof PinnedData;}
    public static void loaded(Object object) {
        ServerList list=(ServerList)object;
        List<ServerData> servers=ReflectionHelper.getPrivateValue(ServerList.class,list,"servers","field_78858_b");
        List<String[]> configured=CheatBreaker.getInstance()==null
                ?Collections.singletonList(new String[]{"Kazer Network","kazer.cn"})
                :CheatBreaker.getInstance().globalSettings.pinnedServers;
        List<ServerData> pins=new ArrayList<>();
        for(String[] entry:configured) {
            if(entry==null||entry.length<2)continue;
            servers.removeIf(data->data.serverIP.equalsIgnoreCase(entry[1]));
            pins.add(new PinnedData(entry[0],entry[1]));
        }
        servers.addAll(0,pins);
    }
    public static Object serverNbt(Object data) {
        return isPinned((ServerData)data)?null:((ServerData)data).getNBTCompound();
    }
    public static void appendNbt(Object list,Object tag) {
        if(tag!=null)((NBTTagList)list).appendTag((NBTBase)tag);
    }
    public static boolean blockSwap(Object list,int from,int to) {
        ServerList servers=(ServerList)list;
        return isPinned(servers.getServerData(from))||isPinned(servers.getServerData(to));
    }
    public static Object entry(Object gui,Object data) {
        return isPinned((ServerData)data)?new Entry((GuiMultiplayer)gui,(ServerData)data)
                :new NormalEntry((GuiMultiplayer)gui,(ServerData)data);
    }
    public static void selected(Object gui,int index) {
        GuiMultiplayer screen=(GuiMultiplayer)gui;
        if(index<0||index>=screen.getServerList().countServers()
                ||!isPinned(screen.getServerList().getServerData(index)))return;
        GuiButton edit=ReflectionHelper.getPrivateValue(GuiMultiplayer.class,screen,"btnEditServer","field_146810_r");
        GuiButton delete=ReflectionHelper.getPrivateValue(GuiMultiplayer.class,screen,"btnDeleteServer","field_146808_t");
        edit.enabled=false;delete.enabled=false;
    }
    public static boolean canMove(Object gui,Object entry,int index,boolean up) {
        ServerList list=((GuiMultiplayer)gui).getServerList();
        if(index<0||index>=list.countServers()||isPinned(list.getServerData(index)))return false;
        int next=index+(up?-1:1);
        return next>=0&&next<list.countServers()&&!isPinned(list.getServerData(next));
    }
    private static final class Entry extends ServerListEntryNormal {
        private final GuiMultiplayer gui;
        private long lastClick;
        Entry(GuiMultiplayer gui,ServerData data){super(gui,data);this.gui=gui;}
        @Override public void drawEntry(int index,int x,int y,int width,int height,int mx,int my,boolean selected) {
            super.drawEntry(index,x,y,width,height,mx,my,selected);
            KazerUiIcons.draw(KazerUiIcons.Icon.STAR,x-13,y+10,9,0xFFFFD24D);
        }
        @Override public boolean mousePressed(int index,int mx,int my,int button,int localX,int localY) {
            gui.selectServer(index);
            long now=net.minecraft.client.Minecraft.getSystemTime();
            if(localX<32||now-lastClick<250)gui.connectToSelected();
            lastClick=now;return localX<32;
        }
    }
    private static final class NormalEntry extends ServerListEntryNormal {
        private final boolean micet;
        NormalEntry(GuiMultiplayer gui,ServerData data){super(gui,data);micet=com.cheatbreaker.client.ui.serverlist.MicetServer.matches(data.serverIP);}
        @Override public void drawEntry(int index,int x,int y,int width,int height,int mx,int my,boolean selected) {
            super.drawEntry(index,x,y,width,height,mx,my,selected);
            if(micet)KazerUiIcons.draw(KazerUiIcons.Icon.STAR,x-13,y+10,9,0xFFFFD24D);
        }
    }
}
