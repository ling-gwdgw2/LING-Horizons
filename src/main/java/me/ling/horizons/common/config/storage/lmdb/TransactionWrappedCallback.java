package me.ling.horizons.common.config.storage.lmdb;

public interface TransactionWrappedCallback<T> {
    T exec(TransactionWrapper wrapper);
}
