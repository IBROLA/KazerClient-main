package cn.kazer.client189.config;

import java.nio.file.Path;
import java.util.Locale;

/** Both game versions must receive the same game directory from the launcher. */
public final class KazerConfigPaths {
    private static final String FORBIDDEN = "<>:\"/\\|?*";
    private KazerConfigPaths() { }

    public static Path root(Path gameDirectory) {
        return gameDirectory.resolve("config").resolve("KazerClient");
    }

    public static Path global(Path gameDirectory) {
        return root(gameDirectory).resolve("global.cfg");
    }

    public static Path profile(Path gameDirectory, String profileName) {
        if (!isWindowsSafeProfileName(profileName) || profileName.equalsIgnoreCase("default")) {
            throw new IllegalArgumentException("Invalid editable profile name");
        }
        return root(gameDirectory).resolve("profiles").resolve(profileName + ".cfg");
    }

    /** 1.7's writer rewrites complete profiles; keep 1.8-only fields separate. */
    public static Path animation(Path gameDirectory) {
        return root(gameDirectory).resolve("1.8.9-animation.cfg");
    }

    /** Same Windows filename policy as the 1.7 client, including CJK names. */
    static boolean isWindowsSafeProfileName(String name) {
        if (name == null || name.isEmpty() || name.equals(".") || name.equals("..")
                || name.endsWith(" ") || name.endsWith(".")) return false;
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (ch < ' ' || Character.isSurrogate(ch) || FORBIDDEN.indexOf(ch) >= 0) return false;
        }
        String stem = name;
        int dot = stem.indexOf('.');
        if (dot >= 0) stem = stem.substring(0, dot);
        if (stem.endsWith(" ") || stem.endsWith(".")) return false;
        stem = stem.toUpperCase(Locale.ROOT);
        return !stem.equals("CON") && !stem.equals("PRN") && !stem.equals("AUX") && !stem.equals("NUL")
                && !device(stem, "COM") && !device(stem, "LPT");
    }

    private static boolean device(String stem, String prefix) {
        if (stem.length() != 4 || !stem.startsWith(prefix)) return false;
        char number = stem.charAt(3);
        return number >= '1' && number <= '9' || number == '\u00b9' || number == '\u00b2' || number == '\u00b3';
    }
}
