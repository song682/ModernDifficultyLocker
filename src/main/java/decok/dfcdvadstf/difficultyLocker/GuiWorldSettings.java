package decok.dfcdvadstf.difficultyLocker;

import cpw.mods.fml.common.Optional;
import decok.dfcdvadstf.createworldui.api.gamerule.GameRuleApplier;
import decok.dfcdvadstf.createworldui.api.gamerule.GameRuleMonitorNSetter;
import decok.dfcdvadstf.createworldui.api.gamerule.GameRuleMonitorNSetter.GameruleValue;
import decok.dfcdvadstf.createworldui.gamerule.GuiScreenGameRuleEditor;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiOptionButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 世界设置界面 - 当 ModernDifficultyLocker 和 CreateWorldUI 同时加载时，
 * 替代 GuiOptions 中的难度按钮和锁定按钮，提供统一的世界设置入口。
 *
 * 包含三个主要按钮：
 * - 左侧：难度选择按钮 + 难度锁定按钮
 * - 右侧：游戏规则编辑按钮（仅在创造模式+作弊模式下可用）
 */
@SuppressWarnings("unchecked")
public class GuiWorldSettings extends GuiScreen implements GuiYesNoCallback {

    private static final int LOCK_BUTTON_ID = 5001;
    private static final int GAMERULES_BUTTON_ID = 5003;
    private static final int DONE_BUTTON_ID = 5004;

    private final GuiScreen parentScreen;
    private final GameSettings gameSettings;

    private GuiOptionButton difficultyButton;
    private GuiLockButton lockButton;
    private GuiButton gameRulesButton;

    private boolean pendingLockState = false;

    public GuiWorldSettings(GuiScreen parentScreen, GameSettings gameSettings) {
        this.parentScreen = parentScreen;
        this.gameSettings = gameSettings;
    }

    @Override
    public void initGui() {
        // 如果从GameRuleEditor返回，应用待生效的游戏规则
        applyPendingGameRules();

        this.buttonList.clear();

        WorldDifficultyData data = WorldDifficultyData.getInstance();
        boolean isLocked = data.isLocked();
        boolean isHardcore = mc.theWorld != null && mc.theWorld.getWorldInfo().isHardcoreModeEnabled();

        // 如果是HardCore模式，确保锁定状态
        if (isHardcore && !isLocked) {
            data.setHardcoreMode(true);
            isLocked = true;
            // 保存HardCore模式的锁定状态
            saveWorldData();
        }

        // === 难度按钮（左侧，缩小宽度给锁定按钮腾空间） ===
        int diffBtnX = this.width / 2 - 155;
        int diffBtnY = this.height / 6 - 12;
        String difficultyText = gameSettings.getKeyBinding(GameSettings.Options.DIFFICULTY);

        difficultyButton = new GuiOptionButton(
            GameSettings.Options.DIFFICULTY.returnEnumOrdinal(),
            diffBtnX, diffBtnY,
            GameSettings.Options.DIFFICULTY, difficultyText
        );
        difficultyButton.width = 128; // 缩小宽度：150 → 128，给锁定按钮留空间

        if (isHardcore) {
            difficultyButton.enabled = false;
            difficultyButton.displayString = I18n.format("options.difficulty") + ": " + I18n.format("options.difficulty.hardcore");
        } else if (isLocked) {
            difficultyButton.enabled = false;
        }

        // === 锁定按钮（难度按钮右侧） ===
        int lockBtnX = diffBtnX + 128 + 2; // 2px间距
        lockButton = new GuiLockButton(LOCK_BUTTON_ID, lockBtnX, diffBtnY, isLocked);

        if (isHardcore) {
            lockButton.enabled = false;
        } else if (isLocked && !DifficultyLocker.config.allowUnlock) {
            lockButton.enabled = false;
        }

        // === 游戏规则按钮（右侧） ===
        gameRulesButton = new GuiButton(GAMERULES_BUTTON_ID,
            this.width / 2 + 5, diffBtnY,
            150, 20, I18n.format("createworldui.button.gameRuleEditor")
        );

        // 仅在创造模式 + 作弊模式同时启用时可用
        boolean isCreative = isCreativeMode();
        boolean cheatsEnabled = areCheatsEnabled();
        gameRulesButton.enabled = isCreative && cheatsEnabled;

        // === 完成按钮 ===
        GuiButton doneButton = new GuiButton(DONE_BUTTON_ID,
            this.width / 2 - 100, this.height / 6 + 168,
            I18n.format("gui.done")
        );

        this.buttonList.add(difficultyButton);
        this.buttonList.add(lockButton);
        this.buttonList.add(gameRulesButton);
        this.buttonList.add(doneButton);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (!button.enabled) return;

        // 难度按钮 - 循环切换难度
        if (button.id == GameSettings.Options.DIFFICULTY.returnEnumOrdinal()
            && button instanceof GuiOptionButton) {
            gameSettings.setOptionValue(((GuiOptionButton) button).returnEnumOptions(), 1);
            button.displayString = gameSettings.getKeyBinding(GameSettings.Options.DIFFICULTY);
        }
        // 锁定按钮
        else if (button.id == LOCK_BUTTON_ID) {
            WorldDifficultyData data = WorldDifficultyData.getInstance();
            boolean currentLocked = data.isLocked();

            if (!currentLocked) {
                // 锁定操作（需要确认）
                pendingLockState = true;
                mc.displayGuiScreen(new GuiYesNo(this,
                    I18n.format("difficulty.lock.confirm.title"),
                    I18n.format("difficulty.lock.confirm.line",
                        I18n.format(mc.gameSettings.difficulty.getDifficultyResourceKey())),
                    1001));
            } else if (DifficultyLocker.config.allowUnlock) {
                // 解锁操作（需要确认）
                pendingLockState = false;
                mc.displayGuiScreen(new GuiYesNo(this,
                    I18n.format("difficulty.unlock.confirm.title"),
                    I18n.format("difficulty.unlock.confirm.line"),
                    1002));
            }
        }
        // 游戏规则按钮 - 打开GameRuleEditor
        else if (button.id == GAMERULES_BUTTON_ID) {
            openGameRuleEditor();
        }
        // 完成按钮
        else if (button.id == DONE_BUTTON_ID) {
            mc.gameSettings.saveOptions();
            mc.displayGuiScreen(parentScreen);
        }
    }

    /**
     * 打开 CreateWorldUI 的 GameRuleEditor
     * 传入当前世界的游戏规则作为可编辑数据
     *
     * 当 createworldui 模组未加载时，Forge 会自动剥离此方法（变为空操作）
     */
    @Optional.Method(modid = "createworldui")
    private void openGameRuleEditor() {
        Map<String, String> currentRules = new HashMap<>();
        if (mc.theWorld != null) {
            Map<String, GameruleValue> allRules = GameRuleMonitorNSetter.getAllGamerules(mc.theWorld);
            for (Map.Entry<String, GameruleValue> entry : allRules.entrySet()) {
                currentRules.put(entry.getKey(), entry.getValue().stringValue);
            }
        }
        mc.displayGuiScreen(new GuiScreenGameRuleEditor(this, currentRules));
    }

    /**
     * GuiYesNoCallback - 确认锁定/解锁对话框的回调
     */
    @Override
    public void confirmClicked(boolean confirmed, int id) {
        WorldDifficultyData data = WorldDifficultyData.getInstance();

        // 锁定确认 (id=1001)
        if (id == 1001) {
            if (confirmed) {
                data.setLocked(true);
                data.setLockedDifficulty(mc.gameSettings.difficulty);
                saveWorldData();

                if (difficultyButton != null) difficultyButton.enabled = false;
                if (lockButton != null) {
                    lockButton.setLocked(true);
                    if (!DifficultyLocker.config.allowUnlock) lockButton.enabled = false;
                }
                playClickSound();
            }
            mc.displayGuiScreen(this);
        }
        // 解锁确认 (id=1002)
        else if (id == 1002) {
            // HardCore模式下不允许解锁
            boolean isHardcore = mc.theWorld != null && mc.theWorld.getWorldInfo().isHardcoreModeEnabled();
            if (isHardcore) {
                mc.displayGuiScreen(this);
                return;
            }
            
            if (confirmed) {
                data.setLocked(false);
                saveWorldData();

                if (difficultyButton != null) difficultyButton.enabled = true;
                if (lockButton != null) {
                    lockButton.setLocked(false);
                    lockButton.enabled = true;
                }
                playClickSound();
            }
            mc.displayGuiScreen(this);
        }
    }

    private void saveWorldData() {
        // 复用正在运行世界的 SaveHandler，切勿再用 getSaveLoader() 新建实例，
        // 否则新建时的 setSessionLock() 会覆盖 session.lock，导致后续存盘中止、难度/排序数据丢失。
        if (mc.getIntegratedServer() != null
            && mc.getIntegratedServer().worldServers != null
            && mc.getIntegratedServer().worldServers.length > 0
            && mc.getIntegratedServer().worldServers[0] != null) {
            WorldDifficultyData.getInstance().saveWorldData(
                mc.getIntegratedServer().worldServers[0].getSaveHandler());
        }
    }

    private void playClickSound() {
        if (mc != null && mc.getSoundHandler() != null) {
            mc.getSoundHandler().playSound(
                PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));
        }
    }

    /**
     * 将 GameRuleEditor 保存的待应用规则直接应用到当前运行的世界
     * （GameRuleEditor 默认使用 GameRuleApplier.setPendingGameRules，
     *  该机制设计为世界加载时生效，但已在运行的世界需要直接应用）
     *
     * 当 createworldui 模组未加载时，Forge 会自动剥离此方法（变为空操作）
     */
    @Optional.Method(modid = "createworldui")
    private void applyPendingGameRules() {
        try {
            Map<String, String> pending = GameRuleApplier.getPendingGameRules();
            if (pending == null || pending.isEmpty() || mc.theWorld == null) return;

            // 应用到客户端世界
            for (Map.Entry<String, String> entry : pending.entrySet()) {
                GameRuleMonitorNSetter.setGamerule(mc.theWorld, entry.getKey(), entry.getValue());
            }

            // 应用到服务端所有维度
            if (mc.getIntegratedServer() != null) {
                for (WorldServer ws : mc.getIntegratedServer().worldServers) {
                    if (ws != null) {
                        for (Map.Entry<String, String> entry : pending.entrySet()) {
                            ws.getGameRules().setOrCreateGameRule(entry.getKey(), entry.getValue());
                        }
                    }
                }
            }

            DifficultyLocker.LOGGER.info("Applied {} game rules to current world from GameRuleEditor", pending.size());
            pending.clear();
        } catch (Exception e) {
            DifficultyLocker.LOGGER.error("Failed to apply pending game rules: {}", e.getMessage());
        }
    }

    private boolean isCreativeMode() {
        try {
            if (mc.theWorld != null && mc.theWorld.getWorldInfo() != null) {
                return mc.theWorld.getWorldInfo().getGameType().isCreative();
            }
        } catch (Exception ignored) {}
        return false;
    }

    private boolean areCheatsEnabled() {
        try {
            if (mc.getIntegratedServer() != null) {
                for (WorldServer ws : mc.getIntegratedServer().worldServers) {
                    if (ws != null && ws.getWorldInfo() != null) {
                        return ws.getWorldInfo().areCommandsAllowed();
                    }
                }
            }
            if (mc.theWorld != null && mc.theWorld.getWorldInfo() != null) {
                return mc.theWorld.getWorldInfo().areCommandsAllowed();
            }
        } catch (Exception ignored) {}
        return false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRendererObj,
            I18n.format("difficultylocker.worldsettings.title"),
            this.width / 2, 15, 0xFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);

        // 为禁用的GameRules按钮绘制Tooltip
        if (gameRulesButton != null && !gameRulesButton.enabled) {
            boolean hovered = mouseX >= gameRulesButton.xPosition &&
                mouseX < gameRulesButton.xPosition + gameRulesButton.width &&
                mouseY >= gameRulesButton.yPosition &&
                mouseY < gameRulesButton.yPosition + gameRulesButton.height;

            if (hovered) {
                List<String> tooltip = new ArrayList<>();
                boolean isCreative = isCreativeMode();
                boolean cheatsEnabled = areCheatsEnabled();

                if (!isCreative && !cheatsEnabled) {
                    tooltip.add(I18n.format("difficultylocker.gamerules.tooltip.needCreativeAndCheats"));
                } else if (!isCreative) {
                    tooltip.add(I18n.format("difficultylocker.gamerules.tooltip.needCreative"));
                } else {
                    tooltip.add(I18n.format("difficultylocker.gamerules.tooltip.needCheats"));
                }

                this.func_146283_a(tooltip, mouseX, mouseY);
            }
        }
    }
}
