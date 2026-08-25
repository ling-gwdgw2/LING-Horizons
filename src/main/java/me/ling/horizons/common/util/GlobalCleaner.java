package me.ling.horizons.common.util;

import java.lang.ref.Cleaner;

public class GlobalCleaner {
    public static final Cleaner CLEANER = Cleaner.create();
}
