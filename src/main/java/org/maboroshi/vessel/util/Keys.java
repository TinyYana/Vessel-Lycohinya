package org.maboroshi.vessel.util;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public final class Keys {
    public static NamespacedKey VESSEL_TEMPLATE;
    public static NamespacedKey MOB_DATA;
    public static NamespacedKey MOB_NAME;
    public static NamespacedKey VESSEL_ID;
    public static NamespacedKey SPAWN_REASON;
    public static NamespacedKey FROM_VESSEL;
    public static NamespacedKey MYTHIC_ID;

    // Versioned payload envelope (org.maboroshi.vessel.storage). Absence of SCHEMA_VERSION on an
    // item that has MOB_DATA means the payload is legacy v0 (raw EntitySnapshot#getAsString(), no envelope).
    public static NamespacedKey SCHEMA_VERSION;
    public static NamespacedKey DATA_VERSION;
    public static NamespacedKey CODEC_ID;
    public static NamespacedKey CHECKSUM;
    public static NamespacedKey ENTITY_TYPE;

    private Keys() {}

    public static void init(Plugin plugin) {
        VESSEL_TEMPLATE = new NamespacedKey(plugin, "template");
        MOB_DATA = new NamespacedKey(plugin, "mob_data");
        MOB_NAME = new NamespacedKey(plugin, "mob_name");
        VESSEL_ID = new NamespacedKey(plugin, "id");
        SPAWN_REASON = new NamespacedKey(plugin, "spawn_reason");
        MYTHIC_ID = new NamespacedKey(plugin, "mythic_id");
        FROM_VESSEL = new NamespacedKey(plugin, "from_vessel");
        SCHEMA_VERSION = new NamespacedKey(plugin, "schema_version");
        DATA_VERSION = new NamespacedKey(plugin, "data_version");
        CODEC_ID = new NamespacedKey(plugin, "codec_id");
        CHECKSUM = new NamespacedKey(plugin, "checksum");
        ENTITY_TYPE = new NamespacedKey(plugin, "entity_type");
    }
}
