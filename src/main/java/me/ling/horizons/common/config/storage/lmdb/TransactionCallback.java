package me.ling.horizons.common.config.storage.lmdb;

import org.lwjgl.system.MemoryStack;

public interface TransactionCallback<T> {
    T exec(MemoryStack stack, long transaction);
}
