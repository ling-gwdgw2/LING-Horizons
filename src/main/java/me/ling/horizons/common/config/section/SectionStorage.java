package me.ling.horizons.common.config.section;

import me.ling.horizons.common.config.IMappingStorage;
import me.ling.horizons.common.world.WorldSection;

public abstract class SectionStorage implements IMappingStorage {
    public abstract int loadSection(WorldSection into);

    public abstract void saveSection(WorldSection section);
}
