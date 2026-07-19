package decok.dfcdvadstf.difficultyLocker.mixin;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInterModComms;
import decok.dfcdvadstf.difficultyLocker.DifficultyLocker;
import decok.dfcdvadstf.difficultyLocker.WorldDifficultyData;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldType;
import net.minecraft.world.storage.ISaveHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin IntegratedServer 以在启动时应用锁定的难度
 *
 * 注意：通过 FMLCommonHandler 拿 MinecraftServer 实例——FML 的类不被混淆，
 * 绕开 Mixin 类代码体内静态调用不被 reobf 重映射的坑，
 * 也绕开 refmap 对继承成员映射不全的坑
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
        // 不要用 (MinecraftServer)(Object)this —— 字节码仍会绑到 IntegratedServer 的方法签名上
        // 也不能用 MinecraftServer.getServer() —— 该静态方法在 obf 下被改名为 func_71276_C
        // 走 FML 的 API：FMLCommonHandler 类名和方法名都不被混淆，安全
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        difficultyLocker$worldName = server.getFolderName();

        difficultyLocker$logger.info("Starting integrated server for world '{}'", difficultyLocker$worldName);

        // 注意：此时 loadAllWorlds 尚未执行，worldServers 还没初始化，拿不到正在运行世界的 SaveHandler。
        // 若在这里用 getSaveLoader() 新建一个临时 SaveHandler，其构造函数的 setSessionLock() 会覆盖
        // 存档的 session.lock，导致后续存盘 checkSessionLock 失败而中止（难度丢失、存档列表排序错乱）。
        // 因此难度数据的加载推迟到 loadAllWorlds 结束、能复用真实 SaveHandler 之后再进行。
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
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();

        if (server.worldServers == null || server.worldServers.length == 0 || server.worldServers[0] == null) {
            difficultyLocker$logger.warn("worldServers not ready, skip difficulty locking for '{}'", difficultyLocker$worldName);
            return;
        }

        // 复用正在运行世界的 SaveHandler，绝不能再用 getSaveLoader() 新建实例，
        // 否则新建时的 setSessionLock() 会覆盖 session.lock，令后续存盘失败。
        ISaveHandler saveHandler = server.worldServers[0].getSaveHandler();

        // 加载世界难度数据（原本在 startServer 中进行，因当时 SaveHandler 尚不存在而移到此处）
        data.loadWorldData(saveHandler, difficultyLocker$worldName);

        // 检测是否为HardCore模式
        boolean isHardcore = server.worldServers[0].getWorldInfo().isHardcoreModeEnabled();

        // 如果是HardCore模式，设置自动锁定
        if (isHardcore) {
            data.setHardcoreMode(true);
            // 保存HardCore模式的锁定状态（复用同一个 SaveHandler）
            data.saveWorldData(saveHandler);
        }

        if (data.isLocked()) {
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
            
            // IMC: 通知 CreateWorldUI 锁定难度已应用
            NBTTagCompound imcTag = new NBTTagCompound();
            imcTag.setInteger("difficultyId", data.getLockedDifficulty());
            FMLInterModComms.sendRuntimeMessage(DifficultyLocker.class, "createworldui", "world_lock_applied", imcTag);
            difficultyLocker$logger.info("Sent runtime IMC: world_lock_applied, difficulty={}", data.getLockedDifficulty());
        } else if (data.hasCurrentDifficulty()) {
            // 未锁定世界：恢复该存档上次记录的难度（1.8+ 式的每存档难度），
            // 避免退出重进后回到默认/全局难度
            EnumDifficulty savedDifficulty = data.getCurrentDifficultyEnum();

            difficultyLocker$logger.info("Restoring per-world difficulty '{}' for unlocked world '{}'",
                savedDifficulty, difficultyLocker$worldName);

            for (int i = 0; i < server.worldServers.length; ++i) {
                WorldServer worldServer = server.worldServers[i];
                if (worldServer != null) {
                    worldServer.difficultySetting = savedDifficulty;
                }
            }

            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null && mc.gameSettings != null) {
                mc.gameSettings.difficulty = savedDifficulty;
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
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();

        // 保存世界难度数据：复用正在运行世界的 SaveHandler，避免新建 SaveHandler 覆盖 session.lock。
        // 否则退出前的最后一次存盘会因 checkSessionLock 失败而中止，导致 LastPlayed 不更新 →
        // 存档列表排序退化成创建顺序，且本局难度修改无法持久化。
        if (server.worldServers != null && server.worldServers.length > 0 && server.worldServers[0] != null) {
            WorldDifficultyData data = WorldDifficultyData.getInstance();
            // 退出前快照当前世界难度，实现每存档难度持久化（未锁定世界重进后不再变回默认）
            data.setCurrentDifficulty(server.worldServers[0].difficultySetting.getDifficultyId());
            data.saveWorldData(server.worldServers[0].getSaveHandler());
        } else {
            difficultyLocker$logger.warn("worldServers not available at shutdown, skip saving difficulty data for '{}'", difficultyLocker$worldName);
        }

        difficultyLocker$logger.info("Shutting down integrated server for world '{}'", difficultyLocker$worldName);
    }
}
