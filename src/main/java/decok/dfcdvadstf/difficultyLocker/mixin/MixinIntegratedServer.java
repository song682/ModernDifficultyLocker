package decok.dfcdvadstf.difficultylocker.mixin;

import decok.dfcdvadstf.difficultyLocker.DifficultyLocker;
import decok.dfcdvadstf.difficultyLocker.WorldDifficultyData;
import net.minecraft.client.Minecraft;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;

/**
 * Mixin IntegratedServer 以在启动时应用锁定的难度
 */
@Mixin(IntegratedServer.class)
public abstract class MixinIntegratedServer {
    
    @Unique
    private static final Logger difficultyLocker$logger = LogManager.getLogger("MixinIntegratedServer");
    
    @Unique
    private String difficultyLocker$worldName;
    
    /**
     * 在服务器启动时加载难度锁定数据
     */
    @Inject(
        method = "startServer",
        at = @At("HEAD")
    )
    private void onStartServer(CallbackInfoReturnable<Boolean> cir) {
        IntegratedServer server = (IntegratedServer) (Object) this;
        difficultyLocker$worldName = server.getFolderName();
        
        difficultyLocker$logger.info("Starting integrated server for world '{}'", difficultyLocker$worldName);
        
        // 加载世界难度数据
        WorldDifficultyData.getInstance().loadWorldData(
            server.getActiveAnvilConverter().getSaveLoader(difficultyLocker$worldName, false),
            difficultyLocker$worldName
        );
    }
    
    /**
     * 在世界初始化后应用锁定的难度
     */
    @Inject(
        method = "loadAllWorlds",
        at = @At("TAIL")
    )
    private void onLoadAllWorlds(String saveName, String worldNameIn, long seed, 
                                  WorldType type, 
                                  String generatorOptions, CallbackInfo ci) {
        
        WorldDifficultyData data = WorldDifficultyData.getInstance();
        
        if (data.isLocked()) {
            IntegratedServer server = (IntegratedServer) (Object) this;
            EnumDifficulty lockedDifficulty = data.getLockedDifficultyEnum();
            
            difficultyLocker$logger.info("Applying locked difficulty '{}' to world '{}'", 
                lockedDifficulty, difficultyLocker$worldName);
            
            // 应用难度到所有世界维度
            for (int i = 0; i < server.worldServers.length; ++i) {
                WorldServer worldServer = server.worldServers[i];
                if (worldServer != null) {
                    worldServer.difficultySetting = lockedDifficulty;
                }
            }
            
            // 更新游戏设置
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null && mc.gameSettings != null) {
                mc.gameSettings.difficulty = lockedDifficulty;
                mc.gameSettings.saveOptions();
            }
        }
    }
    
    /**
     * 在服务器停止时保存数据
     */
    @Inject(
        method = "initiateShutdown",
        at = @At("HEAD")
    )
    private void onInitiateShutdown(CallbackInfo ci) {
        IntegratedServer server = (IntegratedServer) (Object) this;
        
        // 保存世界难度数据
        WorldDifficultyData.getInstance().saveWorldData(
            server.getActiveAnvilConverter().getSaveLoader(difficultyLocker$worldName, false)
        );
        
        difficultyLocker$logger.info("Shutting down integrated server for world '{}'", difficultyLocker$worldName);
    }
}
