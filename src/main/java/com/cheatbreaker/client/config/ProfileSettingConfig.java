package com.cheatbreaker.client.config;

final class ProfileSettingConfig {
    private ProfileSettingConfig() {
    }

    static void apply(Setting setting, String persistedValue) {
        try {
            if (setting.getType() == Setting.Type.INTEGER) {
                boolean rainbow = persistedValue.contains("rainbow");
                int value = Integer.parseInt(persistedValue.split(";", 2)[0]);
                if (setting.getMinimumValue() != null && value < setting.getMinimumValue().intValue()
                        || setting.getMaximumValue() != null && value > setting.getMaximumValue().intValue()) {
                    return;
                }
                setting.rainbow = rainbow;
                setting.setValue(value);
            } else if (setting.getType() == Setting.Type.FLOAT) {
                float value = Float.parseFloat(persistedValue);
                if (setting.getMinimumValue() != null && value < setting.getMinimumValue().floatValue()
                        || setting.getMaximumValue() != null && value > setting.getMaximumValue().floatValue()) {
                    return;
                }
                setting.setValue(value);
            } else if (setting.getType() == Setting.Type.DOUBLE) {
                double value = Double.parseDouble(persistedValue);
                if (setting.getMinimumValue() != null && value < setting.getMinimumValue().doubleValue()
                        || setting.getMaximumValue() != null && value > setting.getMaximumValue().doubleValue()) {
                    return;
                }
                setting.setValue(value);
            }
        } catch (NumberFormatException ignored) {
        }
    }
}
