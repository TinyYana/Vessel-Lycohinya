package org.maboroshi.vessel.config.settings;

import de.exlll.configlib.Comment;
import de.exlll.configlib.ConfigLib;
import de.exlll.configlib.Configuration;
import de.exlll.configlib.NameFormatters;
import de.exlll.configlib.YamlConfigurationProperties;
import de.exlll.configlib.YamlConfigurations;
import java.io.File;
import java.nio.file.Path;

public class MainConfig {
    public static MainConfiguration load(File dataFolder) {
        YamlConfigurationProperties properties = ConfigLib.BUKKIT_DEFAULT_PROPERTIES.toBuilder()
                .setNameFormatter(NameFormatters.LOWER_KEBAB_CASE)
                .build();
        Path configFile = new File(dataFolder, "config.yml").toPath();
        return YamlConfigurations.update(configFile, MainConfiguration.class, properties);
    }

    @Configuration
    public static class MainConfiguration {
        @Comment("Enable debug mode to see detailed logs in the console.")
        public boolean debug = false;

        @Comment({
            "The cooldown time in milliseconds between capture and release actions to prevent rapid reuse.",
            "Recommended minimum value: 500."
        })
        public long cooldown = 500L;

        @Comment("GriefPrevention integration settings (only used if GriefPrevention is installed).")
        public GriefPreventionSettings griefprevention = new GriefPreventionSettings();
    }

    @Configuration
    public static class GriefPreventionSettings {
        @Comment({
            "The GriefPrevention claim permission required to capture an entity inside a claim.",
            "Accepted values: ACCESS, CONTAINER, BUILD. Invalid values fall back to CONTAINER with a warning."
        })
        public String capturePermission = "CONTAINER";

        @Comment({
            "The GriefPrevention claim permission required to release an entity inside a claim.",
            "Accepted values: ACCESS, CONTAINER, BUILD. Invalid values fall back to BUILD with a warning."
        })
        public String releasePermission = "BUILD";
    }
}
