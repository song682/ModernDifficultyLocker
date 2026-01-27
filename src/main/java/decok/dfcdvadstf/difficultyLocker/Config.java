package decok.dfcdvadstf.difficultyLocker;

import net.minecraftforge.common.config.Configuration;
import java.io.File;

public class Config {
    private static Configuration config;
    public boolean allowUnlock; // 默认不允许解锁

    public Config(File file) {
        config = new Configuration(file);
        config.load();

        ConfigOptions();
        saveConfigurationFile();
    }
    private void ConfigOptions(){
        allowUnlock = config.getBoolean("allowUnlock", "general", false, "Set true to enable to unlock the difficulty and confirm");
    }

    private void saveConfigurationFile() {
        config.save();
    }
}