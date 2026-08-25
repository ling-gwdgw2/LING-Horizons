package me.ling.horizons.common.config.compressors;

import me.ling.horizons.common.util.MemoryBuffer;

public interface StorageCompressor {
    MemoryBuffer compress(MemoryBuffer saveData);

    MemoryBuffer decompress(MemoryBuffer saveData);

    void close();
}
