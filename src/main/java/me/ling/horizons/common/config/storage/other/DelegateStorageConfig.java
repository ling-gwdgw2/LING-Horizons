package me.ling.horizons.common.config.storage.other;

import me.ling.horizons.common.config.storage.StorageConfig;

import java.util.List;

public abstract class DelegateStorageConfig extends StorageConfig {
    public StorageConfig delegate;

    @Override
    public List<StorageConfig> getChildStorageConfigs() {
        return List.of(this.delegate);
    }
}
