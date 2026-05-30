package decok.dfcdvadstf.difficultyLocker.mixin;

import decok.dfcdvadstf.difficultyLocker.DifficultyLocker;
import decok.dfcdvadstf.difficultyLocker.GuiLockButton;
import decok.dfcdvadstf.difficultyLocker.GuiWorldSettings;
import decok.dfcdvadstf.difficultyLocker.WorldDifficultyData;
import cpw.mods.fml.client.FMLClientHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.*;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@SuppressWarnings("unchecked")
@Mixin(GuiOptions.class)
public abstract class MixinGuiOptions extends GuiScreen implements GuiYesNoCallback {

    @Shadow
    private GameSettings field_146443_h;

    @Unique
    private static final int LOCK_BUTTON_ID = 5001;

    @Unique
    private static final int WORLD_SETTINGS_BUTTON_ID = 5005;

    @Unique
    private GuiButton difficultyLocker$difficultyButton;
    @Unique
    private GuiLockButton difficultyLocker$lockButton;
    @Unique
    private GuiButton difficultyLocker$worldSettingsButton;
    @Unique
    private boolean difficultyLocker$pendingLockState = false;

    @Inject(method = "initGui", at = @At("TAIL"))
    private void addLockButton(CallbackInfo ci) {
        // 用 FML 自己的 API 拿 Minecraft 实例——FMLClientHandler 是 Forge 的类，类名和方法名都不会被混淆
        // 绕开了 Mixin 类代码体内静态调用不被 reobf 重映射的坑
        Minecraft mc = FMLClientHandler.instance().getClient();
        // 检查是否在单人世界中
        if (mc.theWorld == null || !mc.isIntegratedServerRunning()) {
            return;
        }

        // 查找难度按钮
        for (GuiButton b : (List<GuiButton>) this.buttonList) {
            if (b.id == GameSettings.Options.DIFFICULTY.returnEnumOrdinal()) {
                this.difficultyLocker$difficultyButton = b;
                break;
            }
        }

        if (difficultyLocker$difficultyButton == null) return;

        // ===== 检测 CreateWorldUI 是否加载 =====
        if (DifficultyLocker.isCreateWorldUILoaded()) {
            // 双模组模式：隐藏难度按钮，显示 "World Settings..." 按钮
            difficultyLocker$difficultyButton.visible = false;
            difficultyLocker$difficultyButton.enabled = false;

            difficultyLocker$worldSettingsButton = new GuiButton(
                WORLD_SETTINGS_BUTTON_ID,
                difficultyLocker$difficultyButton.xPosition,
                difficultyLocker$difficultyButton.yPosition,
                150, 20,
                I18n.format("difficultylocker.worldsettings.button")
            );
            this.buttonList.add(difficultyLocker$worldSettingsButton);
        } else {
            // 原始模式：难度按钮 + 锁定按钮
            WorldDifficultyData data = WorldDifficultyData.getInstance();
            boolean isLocked = data.isLocked();
            boolean isHardcore = mc.theWorld.getWorldInfo().isHardcoreModeEnabled();

            // 如果是HardCore模式，确保锁定状态
            if (isHardcore && !isLocked) {
                data.setHardcoreMode(true);
                isLocked = true;
                // 保存HardCore模式的锁定状态
                if (mc.getIntegratedServer() != null) {
                    data.saveWorldData(mc.getIntegratedServer().getActiveAnvilConverter()
                        .getSaveLoader(mc.getIntegratedServer().getFolderName(), false));
                }
            }

            int originalWidth = difficultyLocker$difficultyButton.width;
            int lockWidth = 20;
            int newWidth = originalWidth - lockWidth;

            difficultyLocker$difficultyButton.width = newWidth;

            int x = difficultyLocker$difficultyButton.xPosition + newWidth + 2;
            int y = difficultyLocker$difficultyButton.yPosition;

            difficultyLocker$lockButton = new GuiLockButton(LOCK_BUTTON_ID, x, y, isLocked);
            this.buttonList.add(difficultyLocker$lockButton);

            // HardCore模式或锁定时禁用难度按钮
            difficultyLocker$difficultyButton.enabled = !isLocked;

            if (isLocked && !DifficultyLocker.config.allowUnlock) {
                difficultyLocker$lockButton.enabled = false;
            }
            
            // HardCore模式下，锁定按钮不可用（不能解锁）
            if (isHardcore) {
                difficultyLocker$lockButton.enabled = false;
            }
        }
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"))
    private void onLockButton(GuiButton button, CallbackInfo ci) {
        Minecraft mc = FMLClientHandler.instance().getClient();
        // 检查是否在单人世界中
        if (mc.theWorld == null || !mc.isIntegratedServerRunning()) {
            return;
        }

        // World Settings 按钮点击 → 打开 GuiWorldSettings
        // 双重检查：确保 CreateWorldUI 已加载才尝试实例化 GuiWorldSettings
        if (button.id == WORLD_SETTINGS_BUTTON_ID && button.enabled && DifficultyLocker.isCreateWorldUILoaded()) {
            mc.displayGuiScreen(new GuiWorldSettings((GuiOptions)(Object)this, this.field_146443_h));
            return;
        }

        // 原始锁定按钮逻辑
        if (button.id == LOCK_BUTTON_ID && button.enabled) {
            WorldDifficultyData data = WorldDifficultyData.getInstance();
            boolean currentLocked = data.isLocked();
            boolean isHardcore = mc.theWorld.getWorldInfo().isHardcoreModeEnabled();

            // HardCore模式下不允许解锁
            if (isHardcore && currentLocked) {
                return;
            }

            if (!currentLocked) {
                difficultyLocker$pendingLockState = true;
                mc.displayGuiScreen(new GuiYesNo(this,
                    I18n.format("difficulty.lock.confirm.title"),
                    I18n.format("difficulty.lock.confirm.line", I18n.format(mc.gameSettings.difficulty.getDifficultyResourceKey())),
                    1001));
            }
            else if (DifficultyLocker.config.allowUnlock) {
                difficultyLocker$pendingLockState = false;
                mc.displayGuiScreen(new GuiYesNo(this,
                    I18n.format("difficulty.unlock.confirm.title"),
                    I18n.format("difficulty.unlock.confirm.line"),
                    1002));
            }
        }
    }

    /**
     * GuiYesNoCallback 接口的实现
     * 当用户确认或取消对话框时调用
     */
    @Override
    @Unique
    public void confirmClicked(boolean confirmed, int id) {
        Minecraft mc = FMLClientHandler.instance().getClient();
        WorldDifficultyData data = WorldDifficultyData.getInstance();
        
        // 锁定确认对话框 (id=1001)
        if (id == 1001) {
            if (confirmed) {
                data.setLocked(true);
                data.setLockedDifficulty(mc.gameSettings.difficulty);
                
                if (mc.getIntegratedServer() != null) {
                    data.saveWorldData(mc.getIntegratedServer().getActiveAnvilConverter()
                        .getSaveLoader(mc.getIntegratedServer().getFolderName(), false));
                }

                if (difficultyLocker$difficultyButton != null) {
                    difficultyLocker$difficultyButton.enabled = false;
                }

                if (difficultyLocker$lockButton != null) {
                    difficultyLocker$lockButton.setLocked(true);
                    if (!DifficultyLocker.config.allowUnlock) {
                        difficultyLocker$lockButton.enabled = false;
                    }
                }

                if (mc != null && mc.getSoundHandler() != null) {
                    mc.getSoundHandler().playSound(
                            PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F)
                    );
                }
            }
            if (mc != null) {
                mc.displayGuiScreen(this);
            }
        }
        // 解锁确认对话框 (id=1002)
        else if (id == 1002) {
            if (confirmed) {
                data.setLocked(false);
                
                if (mc.getIntegratedServer() != null) {
                    data.saveWorldData(mc.getIntegratedServer().getActiveAnvilConverter()
                        .getSaveLoader(mc.getIntegratedServer().getFolderName(), false));
                }

                if (difficultyLocker$difficultyButton != null) {
                    difficultyLocker$difficultyButton.enabled = true;
                }

                if (difficultyLocker$lockButton != null) {
                    difficultyLocker$lockButton.setLocked(false);
                    difficultyLocker$lockButton.enabled = true;
                }

                if (mc != null && mc.getSoundHandler() != null) {
                    mc.getSoundHandler().playSound(
                            PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F)
                    );
                }
            }
            if (mc != null) {
                mc.displayGuiScreen(this);
            }
        }
    }

    @Inject(method = "drawScreen", at = @At("TAIL"))
    private void hideLockButtonIfNotInSinglePlayer(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        Minecraft mc = FMLClientHandler.instance().getClient();
        boolean isSinglePlayer = mc.theWorld != null && mc.isIntegratedServerRunning();

        // 处理 World Settings 按钮可见性
        if (difficultyLocker$worldSettingsButton != null) {
            difficultyLocker$worldSettingsButton.visible = isSinglePlayer;
            // 确保 World Settings 模式下难度按钮也保持隐藏
            if (difficultyLocker$difficultyButton != null && isSinglePlayer && DifficultyLocker.isCreateWorldUILoaded()) {
                difficultyLocker$difficultyButton.visible = false;
            }
        }

        // 处理原始锁定按钮可见性
        if (difficultyLocker$lockButton != null) {
            difficultyLocker$lockButton.visible = isSinglePlayer;
            if (!isSinglePlayer && difficultyLocker$difficultyButton != null) {
                difficultyLocker$difficultyButton.width = 150;
            }
        }
    }
}
