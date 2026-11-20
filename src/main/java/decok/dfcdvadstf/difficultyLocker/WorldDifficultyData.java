package decok.dfcdvadstf.difficultyLocker;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.storage.MapStorage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 管理存档的难度锁定状态
 * 每个存档独立存储：是否锁定 + 锁定的难度值
 *
 * 存储层基于原版 {@link WorldSavedData} + {@link MapStorage}：
 * 数据由运行中世界的 MapStorage 持有，编码与落盘全部交给该世界既有的 SaveHandler
 * （文件位于 {@code <world>/data/difficultylocker.dat}）。
 * 因为全程不新建 SaveHandler，其构造函数的 setSessionLock() 不会覆盖 session.lock；
 * 也不会在客户端线程直接写存档文件，写盘统一发生在世界存盘时。
 */
public class WorldDifficultyData extends WorldSavedData {
    private static final Logger LOGGER = LogManager.getLogger("WorldDifficultyData");

    /** MapStorage 中使用的数据名（落盘为 {@code <world>/data/difficultylocker.dat}） */
    public static final String DATA_NAME = "difficultylocker";

    /** 当前存档已绑定的数据对象，未进入世界时为 null */
    private static WorldDifficultyData active;

    /** 未绑定存档时的临时对象，保证调用方拿不到 null（其状态不会落盘） */
    private static WorldDifficultyData detached;

    // 当前存档的锁定状态
    private boolean locked = false;
    private int lockedDifficulty = 2; // 默认普通难度 (0=和平, 1=简单, 2=普通, 3=困难)
    // 未锁定世界的每存档难度快照（-1 = 尚未记录，加载时不覆盖游戏默认难度）
    private int currentDifficulty = -1;
    private String currentWorldName = "";

    // 标记是否为HardCore模式
    private boolean isHardcoreMode = false;

    /**
     * MapStorage 通过反射按单个 String 参数构造实例（{@link MapStorage#loadData}），
     * 因此必须保留这个公有构造。
     */
    public WorldDifficultyData(String mapName) {
        super(mapName);
    }

    /**
     * 当前存档的难度数据；尚未绑定存档时返回一个不会落盘的临时对象
     */
    public static WorldDifficultyData getInstance() {
        if (active != null) {
            return active;
        }
        if (detached == null) {
            detached = new WorldDifficultyData(DATA_NAME);
        }
        return detached;
    }

    /**
     * 绑定到指定世界，并从其 MapStorage 载入难度数据（不存在时登记一份默认数据）
     *
     * 必须传入正在运行世界（服务端维度）的 World 实例：MapStorage 会复用该世界的
     * SaveHandler，绝不能自己 new SaveHandler 或经 getSaveLoader() 新建，
     * 否则构造时的 setSessionLock() 会覆盖 session.lock，令后续存盘 checkSessionLock 失败而中止。
     */
    public static void loadWorldData(World world, String worldName) {
        if (world == null || world.mapStorage == null) {
            LOGGER.warn("World or its MapStorage is null, cannot load difficulty data");
            return;
        }

        WorldSavedData saved = world.mapStorage.loadData(WorldDifficultyData.class, DATA_NAME);
        WorldDifficultyData data;

        if (saved instanceof WorldDifficultyData) {
            data = (WorldDifficultyData) saved;
            LOGGER.info("Loaded difficulty data for world '{}': locked={}, difficulty={}, current={}, hardcore={}",
                worldName, data.locked, data.lockedDifficulty, data.currentDifficulty, data.isHardcoreMode);
        } else {
            data = new WorldDifficultyData(DATA_NAME);
            world.mapStorage.setData(DATA_NAME, data);
            LOGGER.info("No difficulty data found for world '{}', using defaults", worldName);
        }

        data.currentWorldName = worldName;
        active = data;
    }

    /**
     * 解除与当前存档的绑定。
     *
     * 只断开静态引用，不清空数据内容：解除绑定发生在世界最后一次存盘之前，
     * 若此时重置字段，MapStorage 会把默认值写回存档。
     */
    public static void unloadWorldData() {
        active = null;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        this.locked = nbt.getBoolean("Locked");
        this.lockedDifficulty = nbt.getInteger("LockedDifficulty");
        this.isHardcoreMode = nbt.getBoolean("IsHardcoreMode");
        // 读取每存档难度快照（旧数据没有此键时保持 -1，表示不覆盖游戏默认）
        this.currentDifficulty = nbt.hasKey("CurrentDifficulty") ? nbt.getInteger("CurrentDifficulty") : -1;
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        nbt.setBoolean("Locked", locked);
        nbt.setInteger("LockedDifficulty", lockedDifficulty);
        nbt.setInteger("CurrentDifficulty", currentDifficulty); // 保存每存档难度快照
        nbt.setBoolean("IsHardcoreMode", isHardcoreMode); // 保存HardCore标记
    }

    // Getters and Setters

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        if (this.locked == locked) {
            return;
        }
        this.locked = locked;
        markDirty();
    }

    public int getLockedDifficulty() {
        return lockedDifficulty;
    }

    public void setLockedDifficulty(int difficulty) {
        if (difficulty >= 0 && difficulty <= 3 && this.lockedDifficulty != difficulty) {
            this.lockedDifficulty = difficulty;
            markDirty();
        }
    }

    public EnumDifficulty getLockedDifficultyEnum() {
        return EnumDifficulty.getDifficultyEnum(lockedDifficulty);
    }

    public void setLockedDifficulty(EnumDifficulty difficulty) {
        if (difficulty != null) {
            setLockedDifficulty(difficulty.getDifficultyId());
        }
    }

    // ===== 每存档难度快照（未锁定世界的难度持久化）=====

    public int getCurrentDifficulty() {
        return currentDifficulty;
    }

    public void setCurrentDifficulty(int difficulty) {
        if (difficulty >= 0 && difficulty <= 3 && this.currentDifficulty != difficulty) {
            this.currentDifficulty = difficulty;
            markDirty();
        }
    }

    /**
     * 是否存在有效的每存档难度记录（全新世界返回 false，加载时不应覆盖游戏默认难度）
     */
    public boolean hasCurrentDifficulty() {
        return currentDifficulty >= 0 && currentDifficulty <= 3;
    }

    public EnumDifficulty getCurrentDifficultyEnum() {
        return EnumDifficulty.getDifficultyEnum(currentDifficulty);
    }

    public String getCurrentWorldName() {
        return currentWorldName;
    }

    /**
     * 设置是否为HardCore模式
     */
    public void setHardcoreMode(boolean hardcoreMode) {
        boolean changed = this.isHardcoreMode != hardcoreMode;
        this.isHardcoreMode = hardcoreMode;

        // 如果是HardCore模式，自动锁定为困难难度
        if (hardcoreMode && (!this.locked || this.lockedDifficulty != 3)) {
            this.locked = true;
            this.lockedDifficulty = 3; // 困难难度
            changed = true;
            LOGGER.info("HardCore mode enabled, auto-locking to hard difficulty");
        }

        if (changed) {
            markDirty();
        }
    }

    /**
     * 获取是否为HardCore模式
     */
    public boolean isHardcoreMode() {
        return isHardcoreMode;
    }
}
