package decok.dfcdvadstf.difficultyLocker;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.storage.ISaveHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 管理存档的难度锁定状态
 * 每个存档独立存储：是否锁定 + 锁定的难度值
 */
public class WorldDifficultyData {
    private static final Logger LOGGER = LogManager.getLogger("WorldDifficultyData");
    private static final String DATA_FILENAME = "difficultylocker.dat";
    
    // 当前存档的锁定状态
    private boolean locked = false;
    private int lockedDifficulty = 2; // 默认普通难度 (0=和平, 1=简单, 2=普通, 3=困难)
    // 未锁定世界的每存档难度快照（-1 = 尚未记录，加载时不覆盖游戏默认难度）
    private int currentDifficulty = -1;
    private String currentWorldName = "";
    
    // 单例实例
    private static WorldDifficultyData instance;
    
    // 标记是否为HardCore模式
    private boolean isHardcoreMode = false;
    
    public static WorldDifficultyData getInstance() {
        if (instance == null) {
            instance = new WorldDifficultyData();
        }
        return instance;
    }
    
    /**
     * 设置当前世界并加载其锁定数据
     */
    public void loadWorldData(ISaveHandler saveHandler, String worldName) {
        this.currentWorldName = worldName;
        this.locked = false;
        this.lockedDifficulty = 2; // 默认普通难度
        this.currentDifficulty = -1;
        this.isHardcoreMode = false;
        
        if (saveHandler == null) {
            LOGGER.warn("SaveHandler is null, cannot load difficulty data");
            return;
        }
        
        try {
            File worldDir = saveHandler.getWorldDirectory();
            File dataFile = new File(worldDir, DATA_FILENAME);
            
            if (dataFile.exists()) {
                NBTTagCompound nbt = CompressedStreamTools.readCompressed(new FileInputStream(dataFile));
                this.locked = nbt.getBoolean("Locked");
                this.lockedDifficulty = nbt.getInteger("LockedDifficulty");
                this.isHardcoreMode = nbt.getBoolean("IsHardcoreMode"); // 读取HardCore标记
                // 读取每存档难度快照（旧存档没有此键时保持 -1，表示不覆盖游戏默认）
                this.currentDifficulty = nbt.hasKey("CurrentDifficulty") ? nbt.getInteger("CurrentDifficulty") : -1;
                LOGGER.info("Loaded difficulty data for world '{}': locked={}, difficulty={}, current={}, hardcore={}", 
                    worldName, locked, lockedDifficulty, currentDifficulty, isHardcoreMode);
            } else {
                LOGGER.info("No difficulty data found for world '{}', using defaults", worldName);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load difficulty data for world '{}': {}", worldName, e.getMessage());
        }
    }
    
    /**
     * 保存当前世界的锁定数据
     */
    public void saveWorldData(ISaveHandler saveHandler) {
        if (saveHandler == null || currentWorldName.isEmpty()) {
            LOGGER.warn("Cannot save difficulty data: saveHandler is null or world name is empty");
            return;
        }
        
        try {
            File worldDir = saveHandler.getWorldDirectory();
            if (!worldDir.exists()) {
                worldDir.mkdirs();
            }
            
            File dataFile = new File(worldDir, DATA_FILENAME);
            NBTTagCompound nbt = new NBTTagCompound();
            nbt.setBoolean("Locked", locked);
            nbt.setInteger("LockedDifficulty", lockedDifficulty);
            nbt.setInteger("CurrentDifficulty", currentDifficulty); // 保存每存档难度快照
            nbt.setBoolean("IsHardcoreMode", isHardcoreMode); // 保存HardCore标记
            
            CompressedStreamTools.writeCompressed(nbt, new FileOutputStream(dataFile));
            LOGGER.info("Saved difficulty data for world '{}': locked={}, difficulty={}, hardcore={}", 
                currentWorldName, locked, lockedDifficulty, isHardcoreMode);
        } catch (IOException e) {
            LOGGER.error("Failed to save difficulty data for world '{}': {}", currentWorldName, e.getMessage());
        }
    }
    
    /**
     * 清除当前世界数据（用于创建新世界时）
     */
    public void clearWorldData() {
        this.currentWorldName = "";
        this.locked = false;
        this.lockedDifficulty = 2;
        this.currentDifficulty = -1;
        this.isHardcoreMode = false;
    }
    
    // Getters and Setters
    
    public boolean isLocked() {
        return locked;
    }
    
    public void setLocked(boolean locked) {
        this.locked = locked;
    }
    
    public int getLockedDifficulty() {
        return lockedDifficulty;
    }
    
    public void setLockedDifficulty(int difficulty) {
        if (difficulty >= 0 && difficulty <= 3) {
            this.lockedDifficulty = difficulty;
        }
    }
    
    public EnumDifficulty getLockedDifficultyEnum() {
        return EnumDifficulty.getDifficultyEnum(lockedDifficulty);
    }
    
    public void setLockedDifficulty(EnumDifficulty difficulty) {
        if (difficulty != null) {
            this.lockedDifficulty = difficulty.getDifficultyId();
        }
    }
    
    // ===== 每存档难度快照（未锁定世界的难度持久化）=====
    
    public int getCurrentDifficulty() {
        return currentDifficulty;
    }
    
    public void setCurrentDifficulty(int difficulty) {
        if (difficulty >= 0 && difficulty <= 3) {
            this.currentDifficulty = difficulty;
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
        this.isHardcoreMode = hardcoreMode;
        // 如果是HardCore模式，自动锁定为困难难度
        if (hardcoreMode) {
            this.locked = true;
            this.lockedDifficulty = 3; // 困难难度
            LOGGER.info("HardCore mode enabled, auto-locking to hard difficulty");
        }
    }
    
    /**
     * 获取是否为HardCore模式
     */
    public boolean isHardcoreMode() {
        return isHardcoreMode;
    }
}
