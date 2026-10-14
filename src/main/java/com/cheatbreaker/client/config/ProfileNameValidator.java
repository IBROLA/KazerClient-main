package com.cheatbreaker.client.config;

import java.util.Locale;

public final class ProfileNameValidator
{
    private static final String FORBIDDEN_CHARACTERS = "<>:\"/\\|?*";

    private ProfileNameValidator()
    {
    }

    public static boolean isWindowsSafeFileName(String name)
    {
        if (name == null || name.length() == 0 || name.equals(".") || name.equals("..")
            || name.endsWith(" ") || name.endsWith("."))
        {
            return false;
        }

        for (int var0 = 0; var0 < name.length(); ++var0)
        {
            char var1 = name.charAt(var0);

            if (var1 < ' ' || Character.isSurrogate(var1) || FORBIDDEN_CHARACTERS.indexOf(var1) >= 0)
            {
                return false;
            }
        }

        String var2 = name;
        int var3 = var2.indexOf('.');

        if (var3 >= 0)
        {
            var2 = var2.substring(0, var3);
        }

        if (var2.endsWith(" ") || var2.endsWith("."))
        {
            return false;
        }

        var2 = var2.toUpperCase(Locale.ROOT);
        return !var2.equals("CON") && !var2.equals("PRN") && !var2.equals("AUX") && !var2.equals("NUL")
            && !isNumberedDevice(var2, "COM") && !isNumberedDevice(var2, "LPT");
    }

    private static boolean isNumberedDevice(String name, String prefix)
    {
        if (name.length() != 4 || !name.startsWith(prefix))
        {
            return false;
        }

        char var0 = name.charAt(3);
        return var0 >= '1' && var0 <= '9' || var0 == '\u00B9' || var0 == '\u00B2' || var0 == '\u00B3';
    }
}
