package me.ling.horizons.common.world.other;

import com.mojang.serialization.Dynamic;
import me.ling.horizons.common.Logger;
import me.ling.horizons.common.config.IMappingStorage;
import me.ling.horizons.common.util.Pair;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.lwjgl.system.MemoryUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;


//There are independent mappings for biome and block states, these get combined in the shader and allow for more
// variaty of things
public class Mapper {
    private static final int BLOCK_STATE_TYPE = 1;
    private static final int BIOME_TYPE = 2;

    private final IMappingStorage storage;
    public static final long UNKNOWN_MAPPING = -1;
    public static final long AIR = 0;

    private final ReentrantLock blockLock = new ReentrantLock();
    private final ConcurrentHashMap<BlockState, StateEntry> block2stateEntry = new ConcurrentHashMap<>(2000, 0.75f, 10);
    private volatile StateEntry[] blockId2stateEntryArray = new StateEntry[16];
    private volatile int blockStateCount = 0;

    private final ReentrantLock biomeLock = new ReentrantLock();
    private final ConcurrentHashMap<String, BiomeEntry> biome2biomeEntry = new ConcurrentHashMap<>(2000, 0.75f, 10);
    private final ConcurrentHashMap<Holder<Biome>, BiomeEntry> holder2biomeEntry = new ConcurrentHashMap<>(512, 0.75f, 10);
    private volatile BiomeEntry[] biomeId2biomeEntryArray = new BiomeEntry[16];
    private volatile int biomeCount = 0;

    private Consumer<StateEntry> newStateCallback;
    private Consumer<BiomeEntry> newBiomeCallback;
    public Mapper(IMappingStorage storage) {
        this.storage = storage;
        //Insert air since its a special entry (index 0)
        var airEntry = new StateEntry(0, Blocks.AIR.defaultBlockState());
        this.block2stateEntry.put(airEntry.state, airEntry);
        this.blockId2stateEntryArray[0] = airEntry;
        this.blockStateCount = 1;

        this.loadFromStorage();
    }


    public static boolean isAir(long id) {
        //Note: air can mean void, cave or normal air, as the block state is remapped during ingesting
        return (id&(((1L<<20)-1)<<27)) == 0;
    }

    public static int getBlockId(long id) {
        return (int) ((id>>27)&((1<<20)-1));
    }

    public static int getBiomeId(long id) {
        return (int) ((id>>47)&0x1FF);
    }

    public static int getLightId(long id) {
        return (int) ((id>>56)&0xFF);
    }

    public static long withLight(long id, int light) {
        return (id&(~(0xFFL<<56)))|(Integer.toUnsignedLong(light&0xFF)<<56);
    }

    public static long withBlockBiome(long id, int block, int biome) {
        return (id&(0xFFL<<56))|(Integer.toUnsignedLong(block)<<27)|(Integer.toUnsignedLong(biome)<<47);
    }

    public static long airWithLight(int light) {
        return Integer.toUnsignedLong(light&0xFF)<<56;
    }

    public void setStateCallback(Consumer<StateEntry> stateCallback) {
        this.newStateCallback = stateCallback;
    }

    public void setBiomeCallback(Consumer<BiomeEntry> biomeCallback) {
        this.newBiomeCallback = biomeCallback;
    }

    private void loadFromStorage() {
        //TODO: FIXME: have/store the minecraft version the mappings are from (the data version)
        // SharedConstants.getGameVersion().dataVersion().id()
        // then use this to create an update path instead

        var mappings = this.storage.getIdMappingsData();
        List<StateEntry> sentries = new ArrayList<>();
        List<BiomeEntry> bentries = new ArrayList<>();
        List<Pair<byte[], Integer>> sentryErrors = new ArrayList<>();

        boolean[] forceResave = new boolean[1];
        for (var entry : mappings.int2ObjectEntrySet()) {
            int entryType = entry.getIntKey()>>>30;
            int id = entry.getIntKey() & ((1<<30)-1);
            if (entryType == BLOCK_STATE_TYPE) {
                var sentry = StateEntry.deserialize(id, entry.getValue(), forceResave);
                if (sentry.state.isAir()) {
                    Logger.error("Deserialization was air, removed block");
                    sentryErrors.add(new Pair<>(entry.getValue(), id));
                    continue;
                }
                sentries.add(sentry);
                var oldEntry = this.block2stateEntry.putIfAbsent(sentry.state, sentry);
                if (oldEntry != null) {
                    //forceResave[0] |= true;
                    Logger.warn("Multiple mappings for blockstate, using old state, expect things to possibly go really badly. " + oldEntry.id + ":" + sentry.id + ":" + sentry.state );
                }
            } else if (entryType == BIOME_TYPE) {
                var bentry = BiomeEntry.deserialize(id, entry.getValue());
                bentries.add(bentry);
                if (this.biome2biomeEntry.put(bentry.biome, bentry) != null) {
                    throw new IllegalStateException("Multiple mappings for biome entry");
                }
            } else {
                throw new IllegalStateException("Unknown entryType");
            }
        }

        if (!sentryErrors.isEmpty()) {
            forceResave[0] |= true;
            //Insert garbage types into the mapping for those blocks, TODO:FIXME: Need to upgrade the type or have a solution to error blocks
            var rand = new Random();
            for (var error : sentryErrors) {
                while (true) {
                    var state = new StateEntry(error.right(), Block.BLOCK_STATE_REGISTRY.byId(rand.nextInt(Block.BLOCK_STATE_REGISTRY.size() - 1)));
                    if (this.block2stateEntry.put(state.state, state) == null) {
                        sentries.add(state);
                        break;
                    }
                }
            }
        }

        //Insert into the arrays
        int maxBlockId = 0;
        for (var entry : sentries) {
            if (entry.id > maxBlockId) maxBlockId = entry.id;
        }
        StateEntry[] blockArray = new StateEntry[Math.max(16, maxBlockId + 1)];
        blockArray[0] = this.blockId2stateEntryArray[0]; // Air
        int bCount = 1;
        for (var entry : sentries) {
            blockArray[entry.id] = entry;
            bCount++;
        }
        this.blockId2stateEntryArray = blockArray;
        this.blockStateCount = Math.max(bCount, maxBlockId + 1);

        int maxBiomeId = -1;
        for (var entry : bentries) {
            if (entry.id > maxBiomeId) maxBiomeId = entry.id;
        }
        BiomeEntry[] biomeArray = new BiomeEntry[Math.max(16, maxBiomeId + 1)];
        int bioCount = 0;
        for (var entry : bentries) {
            biomeArray[entry.id] = entry;
            bioCount++;
        }
        this.biomeId2biomeEntryArray = biomeArray;
        this.biomeCount = Math.max(bioCount, maxBiomeId + 1);

        if (forceResave[0]) {
            Logger.warn("Forced state resave triggered");
            this.forceResaveStates();
        }
    }

    public final int getBlockStateCount() {
        return this.blockStateCount;
    }

    private StateEntry registerNewBlockState(BlockState state) {
        this.blockLock.lock();
        try {
            var entry = this.block2stateEntry.get(state);
            if (entry != null) {
                return entry;
            }

            int id = this.blockStateCount;
            entry = new StateEntry(id, state);

            StateEntry[] current = this.blockId2stateEntryArray;
            if (id >= current.length) {
                StateEntry[] newArray = Arrays.copyOf(current, Math.max(id + 1, current.length * 2));
                newArray[id] = entry;
                this.blockId2stateEntryArray = newArray;
            } else {
                current[id] = entry;
            }
            this.blockStateCount = id + 1;
            this.block2stateEntry.put(state, entry);

            byte[] serialized = entry.serialize();
            ByteBuffer buffer = MemoryUtil.memAlloc(serialized.length);
            buffer.put(serialized);
            buffer.rewind();
            this.storage.putIdMapping(entry.id | (BLOCK_STATE_TYPE << 30), buffer);
            MemoryUtil.memFree(buffer);

            if (this.newStateCallback != null) this.newStateCallback.accept(entry);
            return entry;
        } finally {
            this.blockLock.unlock();
        }
    }

    private BiomeEntry registerNewBiome(String biome) {
        this.biomeLock.lock();
        try {
            var entry = this.biome2biomeEntry.get(biome);
            if (entry != null) {
                return entry;
            }
            int id = this.biomeCount;
            entry = new BiomeEntry(id, biome);

            BiomeEntry[] current = this.biomeId2biomeEntryArray;
            if (id >= current.length) {
                BiomeEntry[] newArray = Arrays.copyOf(current, Math.max(id + 1, current.length * 2));
                newArray[id] = entry;
                this.biomeId2biomeEntryArray = newArray;
            } else {
                current[id] = entry;
            }
            this.biomeCount = id + 1;
            this.biome2biomeEntry.put(biome, entry);

            byte[] serialized = entry.serialize();
            ByteBuffer buffer = MemoryUtil.memAlloc(serialized.length);
            buffer.put(serialized);
            buffer.rewind();
            this.storage.putIdMapping(entry.id | (BIOME_TYPE << 30), buffer);
            MemoryUtil.memFree(buffer);

            if (this.newBiomeCallback != null) this.newBiomeCallback.accept(entry);
            return entry;
        } finally {
            this.biomeLock.unlock();
        }
    }


    //TODO:FIXME: IS VERY SLOW NEED TO MAKE IT LOCK FREE, or at minimum use a concurrent map
    public long getBaseId(byte light, BlockState state, Holder<Biome> biome) {
        if (state.isAir()) return Byte.toUnsignedLong(light) <<56;//Special case and fast return for air, dont care about the biome
        return composeMappingId(light, this.getIdForBlockState(state), this.getIdForBiome(biome));
    }

    public BlockState getBlockStateFromBlockId(int blockId) {
        if (blockId <= 0) {
            return Blocks.AIR.defaultBlockState();
        }
        StateEntry[] array = this.blockId2stateEntryArray;
        if (blockId >= array.length) {
            return Blocks.AIR.defaultBlockState();
        }
        StateEntry entry = array[blockId];
        return entry == null ? Blocks.AIR.defaultBlockState() : entry.state;
    }

    public int getIdForBlockState(BlockState state) {
        if (state.isAir()) {
            return 0;
        }
        var mapping = this.block2stateEntry.get(state);
        if (mapping == null) {
            mapping = this.registerNewBlockState(state);
        }
        return mapping.id;
    }

    public int getBlockStateOpacity(long mappingId) {
        return this.getBlockStateOpacity(getBlockId(mappingId));
    }

    public int getBlockStateOpacity(int blockId) {
        if (blockId <= 0) {
            return 0;
        }
        StateEntry[] array = this.blockId2stateEntryArray;
        if (blockId >= array.length) {
            return 0;
        }
        StateEntry entry = array[blockId];
        return entry == null ? 0 : entry.opacity;
    }

    public int getIdForBiome(Holder<Biome> biome) {
        if (biome == null) {
            return 0;
        }
        var entry = this.holder2biomeEntry.get(biome);
        if (entry != null) {
            return entry.id;
        }
        var opt = biome.unwrapKey();
        if (opt.isEmpty()) {
            return 0;
        }
        String biomeId = opt.get().location().toString();
        entry = this.biome2biomeEntry.get(biomeId);
        if (entry == null) {
            entry = this.registerNewBiome(biomeId);
        }
        this.holder2biomeEntry.put(biome, entry);
        return entry.id;
    }

    public static long composeMappingId(byte light, int blockId, int biomeId) {
        if (blockId == AIR) {//Dont care about biome for air
            return Byte.toUnsignedLong(light)<<56;
        }
        return (Byte.toUnsignedLong(light)<<56)|(Integer.toUnsignedLong(biomeId) << 47)|(Integer.toUnsignedLong(blockId)<<27);
    }

    public StateEntry[] getStateEntries() {
        this.blockLock.lock();
        try {
            StateEntry[] current = this.blockId2stateEntryArray;
            int count = this.blockStateCount;
            StateEntry[] out = new StateEntry[count];
            System.arraycopy(current, 0, out, 0, count);
            return out;
        } finally {
            this.blockLock.unlock();
        }
    }

    public BiomeEntry[] getBiomeEntries() {
        this.biomeLock.lock();
        try {
            BiomeEntry[] current = this.biomeId2biomeEntryArray;
            int count = this.biomeCount;
            BiomeEntry[] out = new BiomeEntry[count];
            System.arraycopy(current, 0, out, 0, count);
            return out;
        } finally {
            this.biomeLock.unlock();
        }
    }

    public void forceResaveStates() {
        var blocks = new ArrayList<>(this.block2stateEntry.values());
        var biomes = new ArrayList<>(this.biome2biomeEntry.values());


        for (var entry : blocks) {
            if (entry.state.isAir() && entry.id == 0) {
                continue;
            }
            byte[] serialized = entry.serialize();
            ByteBuffer buffer = MemoryUtil.memAlloc(serialized.length);
            buffer.put(serialized);
            buffer.rewind();
            this.storage.putIdMapping(entry.id | (BLOCK_STATE_TYPE<<30), buffer);
            MemoryUtil.memFree(buffer);
        }

        for (var entry : biomes) {
            byte[] serialized = entry.serialize();
            ByteBuffer buffer = MemoryUtil.memAlloc(serialized.length);
            buffer.put(serialized);
            buffer.rewind();
            this.storage.putIdMapping(entry.id | (BIOME_TYPE<<30), buffer);
            MemoryUtil.memFree(buffer);
        }
        this.storage.flush();
    }

    public void close() {

    }


    public static final class StateEntry {
        public final int id;
        public final BlockState state;
        public final int opacity;
        public StateEntry(int id, BlockState state) {
            this.id = id;
            this.state = state;
            //Override opacity of leaves to be solid
            if (state.getBlock() instanceof LeavesBlock) {
                this.opacity = 15;
            } else {
                // MC 1.21.1: getLightBlock() requires (BlockGetter, BlockPos) parameters
                // Use EmptyBlockGetter.INSTANCE and BlockPos.ZERO (same as vanilla's BlockBehaviour.Cache)
                this.opacity = state.getLightBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            }
        }

        public byte[] serialize() {
            try {
                var serialized = new CompoundTag();
                serialized.putInt("id", this.id);
                serialized.put("block_state", BlockState.CODEC.encodeStart(NbtOps.INSTANCE, this.state).result().get());
                var out = new ByteArrayOutputStream();
                NbtIo.writeCompressed(serialized, out);
                return out.toByteArray();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public static StateEntry deserialize(int id, byte[] data, boolean[] forceResave) {
            try {
                var compound = NbtIo.readCompressed(new ByteArrayInputStream(data), NbtAccounter.unlimitedHeap());
                // MC 1.21.1: CompoundTag.getIntOr() → contains() + getInt()
                if ((compound.contains("id") ? compound.getInt("id") : -1) != id) {
                    throw new IllegalStateException("Encoded id != expected id");
                }
                // MC 1.21.1: CompoundTag.getCompound() returns empty CompoundTag if not found (not Optional)
                var bsc = compound.getCompound("block_state");
                var state = BlockState.CODEC.parse(NbtOps.INSTANCE, bsc);
                if (state.isError()) {
                    Logger.info("Could not decode blockstate, attempting fixes, error: "+ state.error().get().message());
                    // MC 1.21.1: WorldVersion.dataVersion() → getDataVersion(), version() → getVersion()
                    bsc = (CompoundTag) DataFixers.getDataFixer().update(References.BLOCK_STATE, new Dynamic<>(NbtOps.INSTANCE,bsc),0, SharedConstants.getCurrentVersion().getDataVersion().getVersion()).getValue();
                    state = BlockState.CODEC.parse(NbtOps.INSTANCE, bsc);
                    if (state.isError()) {
                        Logger.error("Could not decode blockstate setting to air. id:" + id + " error: " + state.error().get().message());
                        return new StateEntry(id, Blocks.AIR.defaultBlockState());
                    } else {
                        Logger.info("Fixed blockstate to: " + state.getOrThrow());
                        forceResave[0] |= true;
                        return new StateEntry(id, state.getOrThrow());
                    }
                } else {
                    return new StateEntry(id, state.getOrThrow());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static final class BiomeEntry {
        public final int id;
        public final String biome;

        public BiomeEntry(int id, String biome) {
            this.id = id;
            this.biome = biome;
        }

        public byte[] serialize() {
            try {
                var serialized = new CompoundTag();
                serialized.putInt("id", this.id);
                serialized.putString("biome_id", this.biome);
                var out = new ByteArrayOutputStream();
                NbtIo.writeCompressed(serialized, out);
                return out.toByteArray();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public static BiomeEntry deserialize(int id, byte[] data) {
            try {
                var compound = NbtIo.readCompressed(new ByteArrayInputStream(data), NbtAccounter.unlimitedHeap());
                // MC 1.21.1: CompoundTag.getIntOr() → contains() + getInt()
                if ((compound.contains("id") ? compound.getInt("id") : -1) != id) {
                    throw new IllegalStateException("Encoded id != expected id");
                }
                // MC 1.21.1: CompoundTag.getStringOr() → contains() + getString()
                String biome = compound.contains("biome_id") ? compound.getString("biome_id") : null;
                return new BiomeEntry(id, biome);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
