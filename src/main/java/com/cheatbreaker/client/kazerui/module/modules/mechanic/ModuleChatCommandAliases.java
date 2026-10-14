package com.cheatbreaker.client.kazerui.module.modules.mechanic;

import com.cheatbreaker.client.kazerui.module.ChatSendEvent;
import com.cheatbreaker.client.kazerui.module.Module;
import com.cheatbreaker.client.kazerui.module.ModuleManager;
import com.cheatbreaker.client.kazerui.module.Page;
import com.cheatbreaker.client.kazerui.module.setting.BoolSetting;
import com.cheatbreaker.client.kazerui.module.setting.ButtonSetting;
import com.cheatbreaker.client.kazerui.module.setting.TextSetting;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;

public final class ModuleChatCommandAliases extends Module {
    private final class Alias {
        final String prefix;
        final BoolSetting active;
        final TextSetting alias, command;
        final BoolSetting caseSensitive;
        final ButtonSetting remove;

        Alias(String prefix) {
            this.prefix = prefix;
            alias = add(new TextSetting(prefix + "alias")).labelKey("alias");
            command = add(new TextSetting(prefix + "command")).labelKey("command");
            caseSensitive = add(new BoolSetting(prefix + "caseSensitive", false)).labelKey("caseSensitive");
            active = add(new BoolSetting(prefix + "active", true)).label(this::name);
            remove = add(new ButtonSetting(prefix + "remove", () -> removeAlias(this))).labelKey("remove");
        }

        private String name() {
            String a = alias(), c = command();
            if (c.isEmpty() && a.isEmpty()) return lang("empty");
            String cs = caseSensitive.on() ? " (" + lang("caseSensitive") + ")" : "";
            return a + " -> " + c + cs;
        }

        String alias() { return slash(alias.get()); }
        String command() { return slash(command.get()); }

        void drop() {
            removeSetting(active);
            removeSetting(alias);
            removeSetting(command);
            removeSetting(caseSensitive);
            removeSetting(remove);
        }
    }

    private final List<Alias> aliases = new ArrayList<Alias>();
    private String loadedIds;
    private final ButtonSetting addButton = add(new ButtonSetting("add", this::addAlias));

    ModuleChatCommandAliases() {
        super("CHAT_COMMAND_ALIASES_CHILD", false);
        loadAliases();
        ChatSendEvent.listen(this::onSend);
    }

    private List<String> ids() {
        List<String> out = new ArrayList<String>();
        String raw = ModuleManager.store(key(), "aliases", "");
        for (String s : raw.split(",")) if (!s.isEmpty()) out.add(s);
        return out;
    }
    private void loadAliases() {
        loadedIds=ModuleManager.store(key(),"aliases","");
        for(Alias alias:aliases)alias.drop();aliases.clear();
        for(String id:ids())if(id.matches("alias[0-9]{1,6}\\.")&&aliases.size()<100)aliases.add(new Alias(id));
    }
    @Override public void tick(com.cheatbreaker.client.kazerui.event.UiTick.ClientTickEvent event) {
        if(event.phase==com.cheatbreaker.client.kazerui.event.UiTick.Phase.END
                && !ModuleManager.store(key(),"aliases","").equals(loadedIds))loadAliases();
    }

    private void save() {
        StringBuilder b = new StringBuilder();
        for (Alias a : aliases) { if (b.length() > 0) b.append(','); b.append(a.prefix); }
        ModuleManager.put(key(), "aliases", b.toString());
        loadedIds=b.toString();
    }

    private void addAlias() {
        if (aliases.size()>=100) return;
        int n = 0;
        for (Alias a : aliases) n = Math.max(n, Integer.parseInt(a.prefix.substring(5, a.prefix.length() - 1)) + 1);
        aliases.add(new Alias("alias" + n + "."));
        save();
    }

    private void removeAlias(Alias a) {
        aliases.remove(a);
        a.drop();
        save();
    }

    @Override protected void layout(Page page) {
        for (Alias a : aliases) page.group(a.active, g -> g.add(a.alias, a.command, a.caseSensitive, a.remove));
        page.add(addButton);
    }

    private static String slash(String s) {
        if (s == null) s = "";
        s=s.trim();
        if (!s.isEmpty() && s.charAt(0) != '/') s = "/" + s;
        return s.trim();
    }

    private void onSend(ChatSendEvent e) {
        if (!isEnabled() || e.cancelled || Minecraft.getMinecraft().thePlayer == null || !e.getMessage().startsWith("/")) return;
        String replaced = replace(e.getMessage());
        if (replaced == null || replace(replaced) != null) return;
        e.setMessage(replaced);
    }

    private String replace(String message) {
        for (Alias a : aliases) {
            if (!a.active.on()) continue;
            String alias = a.alias(), command = a.command();
            if (alias.isEmpty() || command.isEmpty()) continue;
            String replacement=replaceAlias(message,alias,command,a.caseSensitive.on());
            if(replacement!=null)return replacement;
        }
        return null;
    }
    /** Match only the command token; never lowercase replacement text/arguments. */
    static String replaceAlias(String message,String alias,String command,boolean caseSensitive) {
        if(message==null||alias==null||command==null||alias.isEmpty()||command.isEmpty()||!message.startsWith("/"))return null;
        int end=0;while(end<message.length()&&!Character.isWhitespace(message.charAt(end)))end++;
        if(end!=alias.length()||!message.regionMatches(!caseSensitive,0,alias,0,end))return null;
        return command+message.substring(end);
    }
}
