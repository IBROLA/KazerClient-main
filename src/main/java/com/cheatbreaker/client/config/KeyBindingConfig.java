package com.cheatbreaker.client.config;

import java.util.List;

/**
 * Reads and writes CheatBreaker's key bindings as plain {@code <Name> Key=<keyCode>} lines in
 * {@code global.cfg}, and performs the one-shot migration from vanilla's {@code options.txt}.
 *
 * <p>Generalises the former {@code FreelookKeyConfig}, whose line format this keeps so existing
 * {@code Freelook Key=} entries continue to resolve.
 */
final class KeyBindingConfig {
    private static final String SUFFIX = " Key";
    private static final String OPTIONS_PREFIX = "key_";

    private KeyBindingConfig() {
    }

    /** Reads {@code <configName> Key=<code>} from global.cfg lines, or returns {@code fallback}. */
    static int resolve(List<String> lines, String configName, int fallback) {
        String key = configName + SUFFIX;
        for (String line : lines) {
            String[] split = line.split("=", 2);
            if (split.length != 2 || !key.equalsIgnoreCase(split[0].trim())) {
                continue;
            }
            return parse(split[1], fallback);
        }
        return fallback;
    }

    static String serialize(String configName, int keyCode) {
        return configName + SUFFIX + "=" + keyCode;
    }

    /**
     * Resolves a binding, migrating from options.txt only when global.cfg has no entry yet.
     * Once the entry exists it wins outright, which makes repeated startups idempotent.
     *
     * <p>{@code optionNames} is tried in order, so a binding whose options.txt name changed over
     * time (OptiFine wrote Zoom as {@code of.key.zoom}) still migrates its stored value instead of
     * silently falling back to the default.
     */
    static int resolveOrMigrate(List<String> globalLines, List<String> optionLines,
                                String configName, String[] optionNames, int defaultCode) {
        int legacy = defaultCode;
        for (String optionName : optionNames) {
            int found = readOption(optionLines, optionName, Integer.MIN_VALUE);
            if (found != Integer.MIN_VALUE) {
                legacy = found;
                break;
            }
        }
        return resolve(globalLines, configName, legacy);
    }

    /** Reads {@code key_<keyDescription>:<code>} from vanilla options.txt lines. */
    private static int readOption(List<String> optionLines, String keyDescription, int fallback) {
        String key = OPTIONS_PREFIX + keyDescription;
        for (String line : optionLines) {
            int separator = line.indexOf(':');
            if (separator <= 0 || !key.equals(line.substring(0, separator))) {
                continue;
            }
            return parse(line.substring(separator + 1), fallback);
        }
        return fallback;
    }

    private static int parse(String raw, int fallback) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
