package decok.dfcdvadstf.difficultyLocker.mixin;

import decok.dfcdvadstf.difficultyLocker.DifficultyLockManager;
import decok.dfcdvadstf.difficultyLocker.DifficultyLocker;
import decok.dfcdvadstf.difficultyLocker.GuiLockButton;
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
    private GuiButton modernDifficultyLocker$difficultyButton;
    @Unique
    private GuiLockButton modernDifficultyLocker$lockButton;
    @Unique
    private boolean modernDifficultyLocker$pendingLockState = false;

    @Inject(method = "initGui", at = @At("TAIL"))
    private void addLockButton(CallbackInfo ci) {
        // 检查是否在单人世界中
        if (this.mc.theWorld == null || !this.mc.isIntegratedServerRunning()) {
            return;
        }

        // Find difficulty button
        for (GuiButton b : (List<GuiButton>) this.buttonList) {
            if (b.id == GameSettings.Options.DIFFICULTY.returnEnumOrdinal()) {
                this.modernDifficultyLocker$difficultyButton = b;
                break;
            }
        }

        if (modernDifficultyLocker$difficultyButton == null) return;

        // 获取当前世界的锁定状态
        boolean isLocked = DifficultyLockManager.isDifficultyLocked();

        // Resize difficulty button
        int originalWidth = modernDifficultyLocker$difficultyButton.width; // 150
        int lockWidth = 20;
        int newWidth = originalWidth - lockWidth;

        modernDifficultyLocker$difficultyButton.width = newWidth;

        // Add lock button
        int x = modernDifficultyLocker$difficultyButton.xPosition + newWidth;
        int y = modernDifficultyLocker$difficultyButton.yPosition;

        modernDifficultyLocker$lockButton = new GuiLockButton(LOCK_BUTTON_ID, x, y, isLocked);
        this.buttonList.add(modernDifficultyLocker$lockButton);

        // 根据锁定状态设置难度按钮是否可用
        modernDifficultyLocker$difficultyButton.enabled = !isLocked;

        // 如果已锁定且不允许解锁，禁用锁按钮
        if (isLocked && !DifficultyLocker.config.allowUnlock) {
            modernDifficultyLocker$lockButton.enabled = false;
        }
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"))
    private void onLockButton(GuiButton button, CallbackInfo ci) {
        // 检查是否在单人世界中
        if (this.mc.theWorld == null || !this.mc.isIntegratedServerRunning()) {
            return;
        }

        if (button.id == LOCK_BUTTON_ID && button.enabled) {
            // 获取当前锁定状态
            boolean currentLocked = DifficultyLockManager.isDifficultyLocked();

            // 如果当前未锁定，进行锁定操作（需要确认）
            if (!currentLocked) {
                modernDifficultyLocker$pendingLockState = true;this.mc.displayGuiScreen(new GuiYesNo(this, I18n.format("difficulty.lock.confirm.title"), I18n.format("difficulty.lock.confirm.line"), 1001));
            }
            // 如果当前已锁定且允许解锁，进行解锁操作（需要确认）
            else if (DifficultyLocker.config.allowUnlock) {
                modernDifficultyLocker$pendingLockState = false;
                this.mc.displayGuiScreen(new GuiYesNo(this, I18n.format("difficulty.unlock.confirm.title"), I18n.format("difficulty.unlock.confirm.line"), 1002));
            }
            // 如果已锁定且不允许解锁，什么都不做（按钮应该已经被禁用）
        }
    }

    /**
     * GuiYesNoCallback 接口的实现
     * 当用户确认或取消对话框时调用
     */
    @Override
    @Unique
    public void confirmClicked(boolean confirmed, int id) {
        // 锁定确认对话框 (id=1001)
        if (id == 1001) {
            if (confirmed) {
                // 用户确认锁定，保存锁定状态
                DifficultyLockManager.setDifficultyLocked(true);

                // 禁用难度按钮
                if (modernDifficultyLocker$difficultyButton != null) {
                    modernDifficultyLocker$difficultyButton.enabled = false;
                }

                // 更新锁按钮纹理
                if (modernDifficultyLocker$lockButton != null) {
                    modernDifficultyLocker$lockButton.setLocked(true);

                    // 如果默认不允许解锁，锁定后禁用锁按钮
                    if (!DifficultyLocker.config.allowUnlock) {
                        modernDifficultyLocker$lockButton.enabled = false;
                    }
                }

                // 播放点击声音
                if (mc != null && mc.getSoundHandler() != null) {
                    mc.getSoundHandler().playSound(
                            PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F)
                    );
                }
            }
            // 返回设置界面
            if (mc != null) {
                mc.displayGuiScreen(this);
            }
        }
        // 解锁确认对话框 (id=1002)
        else if (id == 1002) {
            if (confirmed) {
                // 用户确认解锁，保存解锁状态
                DifficultyLockManager.setDifficultyLocked(false);

                // 启用难度按钮
                if (modernDifficultyLocker$difficultyButton != null) {
                    modernDifficultyLocker$difficultyButton.enabled = true;
                }

                // 更新锁按钮纹理
                if (modernDifficultyLocker$lockButton != null) {
                    modernDifficultyLocker$lockButton.setLocked(false);
                    modernDifficultyLocker$lockButton.enabled = true; // 确保锁按钮可用
                }

                // 播放点击声音
                if (mc != null && mc.getSoundHandler() != null) {
                    mc.getSoundHandler().playSound(
                            PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F)
                    );
                }
            }
            // 返回设置界面
            if (mc != null) {
                mc.displayGuiScreen(this);
            }
        }
    }

    @Inject(method = "drawScreen", at = @At("TAIL"))
    private void hideLockButtonIfNotInSinglePlayer(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        // 检查是否在单人世界中，如果不在，隐藏锁按钮
        if (modernDifficultyLocker$lockButton != null) {
            boolean isSinglePlayer = this.mc.theWorld != null && this.mc.isIntegratedServerRunning();
            modernDifficultyLocker$lockButton.visible = isSinglePlayer;

            // 如果不在单人世界，也恢复难度按钮的原始宽度
            if (!isSinglePlayer && modernDifficultyLocker$difficultyButton != null) {
                modernDifficultyLocker$difficultyButton.width = 150;
            }
        }
    }
}