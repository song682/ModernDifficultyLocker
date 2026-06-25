package decok.dfcdvadstf.difficultyLocker;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.event.FMLInterModComms;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.MinecraftForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

import com.google.common.collect.ImmutableList;

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
        
        // IMC: 向 CreateWorldUI 推送配置信息
        NBTTagCompound imcTag = new NBTTagCompound();
        imcTag.setBoolean("showLockButton", config.showLockButton);
        imcTag.setBoolean("allowUnlock", config.allowUnlock);
        imcTag.setInteger("defaultLockedDifficulty", config.defaultLockedDifficulty);
        FMLInterModComms.sendMessage("createworldui", "difficultylocker_config", imcTag);
        LOGGER.info("Sent IMC config to createworldui: showLockButton={}, allowUnlock={}",
            config.showLockButton, config.allowUnlock);
    }
    
    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        // 注册事件总线以接收运行时 IMC
        MinecraftForge.EVENT_BUS.register(this);
        LOGGER.info("Registered tick handler for runtime IMC");
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
    
    // ===== Runtime IMC 轮询 =====
    
    private int pollTick = 0;
    
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) return;
        if (++pollTick % 20 != 0) return; // 每秒查一次
        
        ImmutableList<FMLInterModComms.IMCMessage> messages = FMLInterModComms.fetchRuntimeMessages(DifficultyLocker.class);
        for (FMLInterModComms.IMCMessage msg : messages) {
            if ("createworldui".equals(msg.getSender()) && "lock_state_change".equals(msg.key)) {
                NBTTagCompound nbt = msg.getNBTValue();
                int diffId = nbt.getInteger("difficultyId");
                boolean locked = nbt.getBoolean("locked");
                if (locked && diffId >= 0 && diffId <= 3) {
                    WorldDifficultyData data = WorldDifficultyData.getInstance();
                    data.setLocked(true);
                    data.setLockedDifficulty(diffId);
                    LOGGER.info("IMC: Received lock_state_change: diff={}, locked={}", diffId, locked);
                }
            }
            if ("createworldui".equals(msg.getSender()) && "world_created".equals(msg.key)) {
                NBTTagCompound nbt = msg.getNBTValue();
                String wName = nbt.getString("worldName");
                int diffId = nbt.getInteger("difficulty");
                boolean locked = nbt.getBoolean("locked");
                
                LOGGER.info("IMC: World created notification from CreateWorldUI: world='{}', diff={}, locked={}",
                    wName, diffId, locked);
                
                if (locked && diffId >= 0 && diffId <= 3) {
                    WorldDifficultyData data = WorldDifficultyData.getInstance();
                    data.setLocked(true);
                    data.setLockedDifficulty(diffId);
                    LOGGER.info("IMC: Synced world_created lock state to WorldDifficultyData");
                }
            }
        }
    }
}
