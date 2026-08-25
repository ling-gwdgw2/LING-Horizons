package me.ling.horizons.common.config.section;

import me.ling.horizons.common.config.ConfigBuildCtx;
import me.ling.horizons.common.config.Serialization;

public abstract class SectionStorageConfig {
    static {
        Serialization.CONFIG_TYPES.add(SectionStorageConfig.class);
    }

    public abstract SectionStorage build(ConfigBuildCtx ctx);
}
