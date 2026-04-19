package decok.dfcdvadstf.difficultyLocker;

import net.minecraftforge.common.config.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

public class DifficultyLockerConfig {
    
    private static final Logger LOGGER = LogManager.getLogger("DifficultyLockerConfig");
    
    // 配置项
    public boolean allowUnlock = false;
    public int defaultLockedDifficulty = 2; // 0=和平, 1=简单, 2=普通, 3=困难
    public boolean showLockButton = true;
    
    private final Configuration config;
    
    public DifficultyLockerConfig(File configFile) {
        config = new Configuration(configFile);
        loadConfig();
    }
    
    private void loadConfig() {
        config.load();
        
        allowUnlock = config.getBoolean(
            "allowUnlock",
            Configuration.CATEGORY_GENERAL,
            false,
            "Set true to enable unlocking the difficulty after it's locked\\n设置为true以允许在锁定后解锁难度"
        );
        
        defaultLockedDifficulty = config.getInt(
            "defaultLockedDifficulty",
            Configuration.CATEGORY_GENERAL,
            2,
            0,
            3,
            "Default difficulty when locked (0=Peaceful, 1=Easy, 2=Normal, 3=Hard)\\n锁定时的默认难度 (0=和平, 1=简单, 2=普通, 3=困难)"
        );
        
        showLockButton = config.getBoolean(
            "showLockButton",
            Configuration.CATEGORY_GENERAL,
            true,
            "Show the lock difficulty button in the create world GUI\\n在创建世界界面显示锁定难度按钮"
        );
        
        if (config.hasChanged()) {
            config.save();
        }
        
        LOGGER.info("Configuration loaded: allowUnlock={}, defaultLockedDifficulty={}, showLockButton={}",
            allowUnlock, defaultLockedDifficulty, showLockButton);
    }
    
    public void saveConfig() {
        config.get(Configuration.CATEGORY_GENERAL, "allowUnlock", false).set(allowUnlock);
        config.get(Configuration.CATEGORY_GENERAL, "defaultLockedDifficulty", 2).set(defaultLockedDifficulty);
        config.get(Configuration.CATEGORY_GENERAL, "showLockButton", true).set(showLockButton);
        
        if (config.hasChanged()) {
            config.save();
        }
    }
}
