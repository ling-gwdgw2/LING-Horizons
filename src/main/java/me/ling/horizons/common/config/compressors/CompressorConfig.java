package me.ling.horizons.common.config.compressors;

import me.ling.horizons.common.config.ConfigBuildCtx;
import me.ling.horizons.common.config.Serialization;

public abstract class CompressorConfig {
    static {
        Serialization.CONFIG_TYPES.add(CompressorConfig.class);
    }

    public abstract StorageCompressor build(ConfigBuildCtx ctx);
}
