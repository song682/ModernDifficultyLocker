package decok.dfcdvadstf.difficultyLocker;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

@Mod(modid = Tags.MODID, name = Tags.NAME, version = Tags.VERSION, acceptedMinecraftVersions = Tags.ACCEPTED_MC_VERSION, acceptableRemoteVersions = Tags.ACCEPTED_MC_VERSION)
public class DifficultyLocker {
    public static Logger logger;
    public static Config config;

    @EventHandler
    public void preInit (FMLPreInitializationEvent event) {
        config = new Config(event.getSuggestedConfigurationFile());
        logger = event.getModLog();
        logger.info("Difficulty Locker is preinitializing.....");
    }

    @EventHandler
    public void init (FMLInitializationEvent event) {
        logger.info("Difficulty Locker is initializing");
    }
}
