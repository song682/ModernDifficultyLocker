package decok.dfcdvadstf.difficultyLocker;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
    modid = Tags.MODID,
    name = Tags.NAME,
    version = Tags.VERSION,
    acceptedMinecraftVersions = "1.7.10",
    acceptableRemoteVersions = "1.7.10"
)
public class DifficultyLocker {
    
    public static final Logger LOGGER = LogManager.getLogger(Tags.NAME);
    public static DifficultyLockerConfig config;

    private static Boolean createWorldUILoaded = null;

    /**
     * 检测 CreateWorldUI 模组是否已加载（结果会被缓存）
     */
    public static boolean isCreateWorldUILoaded() {
        if (createWorldUILoaded == null) {
            createWorldUILoaded = Loader.isModLoaded("createworldui");
            LOGGER.info("CreateWorldUI mod loaded: {}", createWorldUILoaded);
        }
        return createWorldUILoaded;
    }
    
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("Initializing " + Tags.NAME);
        config = new DifficultyLockerConfig(event.getSuggestedConfigurationFile());
    }
    
    @Mod.EventHandler
    public void onServerStarting(FMLServerStartingEvent event) {
        // 服务器启动时加载世界难度数据
        if (event.getServer() != null) {
            WorldDifficultyData.getInstance().loadWorldData(
                event.getServer().getActiveAnvilConverter().getSaveLoader(event.getServer().getFolderName(), false),
                event.getServer().getFolderName()
            );
        }
    }
    
    @Mod.EventHandler
    public void onServerStopping(FMLServerStoppingEvent event) {
        // 服务器停止时保存世界难度数据
        WorldDifficultyData.getInstance().clearWorldData();
    }
}
