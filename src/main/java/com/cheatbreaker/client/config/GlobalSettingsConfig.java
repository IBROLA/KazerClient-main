package com.cheatbreaker.client.config;

import java.util.List;

/** Applies one persisted global setting without requiring Minecraft startup. */
public final class GlobalSettingsConfig {
    private static final String VIGNETTE = "Vignette";
    private static final String VIGNETTE_STRENGTH = "Vignette Strength";
    private static final String LEGACY_VIGNETTE = "Container Vignette";
    private static final String LEGACY_VIGNETTE_STRENGTH = "Container Vignette Strength";

    private GlobalSettingsConfig() {
    }

    static void applyGlobalSettingLines(List<String> lines, List<Setting> settings) {
        boolean hasVignette = containsKey(lines, VIGNETTE);
        boolean hasVignetteStrength = containsKey(lines, VIGNETTE_STRENGTH);

        for (String line : lines) {
            String[] split = line.split("=", 2);
            if (split.length != 2) {
                continue;
            }

            if (LEGACY_VIGNETTE.equalsIgnoreCase(split[0])) {
                if (!hasVignette) {
                    applyGlobalSettingLine(VIGNETTE + "=" + split[1], settings);
                }
            } else if (LEGACY_VIGNETTE_STRENGTH.equalsIgnoreCase(split[0])) {
                if (!hasVignetteStrength) {
                    String strength = "70".equals(split[1]) ? "50" : split[1];
                    applyGlobalSettingLine(VIGNETTE_STRENGTH + "=" + strength, settings);
                }
            } else {
                applyGlobalSettingLine(line, settings);
            }
        }
    }

    private static boolean containsKey(List<String> lines, String key) {
        for (String line : lines) {
            String[] split = line.split("=", 2);
            if (split.length == 2 && key.equalsIgnoreCase(split[0])) {
                return true;
            }
        }
        return false;
    }

    static void applyGlobalSettingLine(String line, List<Setting> settings) {
        String[] split = line.split("=", 2);
        if (split.length != 2) {
            return;
        }

        for (Setting setting : settings) {
            if (setting.getLabel().equalsIgnoreCase("label") || !setting.getLabel().equalsIgnoreCase(split[0])) {
                continue;
            }

            switch (setting.getType()) {
                case BOOLEAN:
                    setting.setValue(Boolean.parseBoolean(split[1]));
                    break;
                case INTEGER:
                    String value = split[1];
                    setting.rainbow = value.contains("rainbow");
                    int integer = Integer.parseInt(value.split(";", 2)[0]);
                    if (setting.getMinimumValue() != null && integer < setting.getMinimumValue().intValue()
                            || setting.getMaximumValue() != null && integer > setting.getMaximumValue().intValue()) {
                        break;
                    }
                    setting.setValue(integer);
                    break;
                case FLOAT:
                    float floatValue = Float.parseFloat(split[1]);
                    if (setting.getMinimumValue() != null && floatValue < setting.getMinimumValue().floatValue()
                            || setting.getMaximumValue() != null && floatValue > setting.getMaximumValue().floatValue()) {
                        break;
                    }
                    setting.setValue(floatValue);
                    break;
                case DOUBLE:
                    double doubleValue = Double.parseDouble(split[1]);
                    if (setting.getMinimumValue() != null && doubleValue < setting.getMinimumValue().doubleValue()
                            || setting.getMaximumValue() != null && doubleValue > setting.getMaximumValue().doubleValue()) {
                        break;
                    }
                    setting.setValue(doubleValue);
                    break;
                case STRING_ARRAY:
                    for (String accepted : setting.getAcceptedValues()) {
                        if (accepted.equalsIgnoreCase(split[1])) {
                            setting.setValue(split[1]);
                            break;
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
