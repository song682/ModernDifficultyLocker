package decok.dfcdvadstf.difficultyLocker;

import cpw.mods.fml.common.Optional;
import decok.dfcdvadstf.catframe.ui.Text;
import decok.dfcdvadstf.catframe.ui.components.Button;
import decok.dfcdvadstf.catframe.ui.components.Tooltip;
import decok.dfcdvadstf.catframe.ui.screens.Screen;
import decok.dfcdvadstf.createworldui.api.gamerule.GameRuleApplier;
import decok.dfcdvadstf.createworldui.api.gamerule.GameRuleMonitorNSetter;
import decok.dfcdvadstf.createworldui.api.gamerule.GameRuleMonitorNSetter.GameruleValue;
import decok.dfcdvadstf.createworldui.ui.gamerule.IngameGameRuleScreen;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.HashMap;
import java.util.Map;

/**
 * 世界设置界面 - 当 ModernDifficultyLocker 和 CreateWorldUI 同时加载时，
 * 替代 GuiOptions 中的难度按钮和锁定按钮，提供统一的世界设置入口。
 *
 * 界面基类为前置模组 CatFrame 的 {@link Screen}（CatFrame 生命周期）：
 * 组件在 {@link #init()} 中经 {@code addRenderableWidget} 注册，
 * 渲染、鼠标/键盘事件派发与组件级 tooltip 均由 CatFrame 基类统一处理。
 *
 * 包含三个主要按钮：
 * - 左侧：难度选择按钮 + 难度锁定按钮
 * - 右侧：游戏规则编辑按钮（仅在创造模式+作弊模式下可用）
 *
 * <p>
 * World settings screen — used when ModernDifficultyLocker and CreateWorldUI are
 * both loaded; replaces the difficulty and lock buttons in GuiOptions with a
 * unified world-settings entry. Extends CatFrame's {@link Screen} so this screen
 * runs on the CatFrame lifecycle: widgets are registered in {@link #init()} via
 * {@code addRenderableWidget}, and rendering, event dispatch and widget-level
 * tooltips are handled uniformly by the CatFrame base class.
 * </p>
 */
@SuppressWarnings("unchecked")
public class GuiWorldSettings extends Screen implements GuiYesNoCallback {

    private final GuiScreen parentScreen;
    private final GameSettings gameSettings;

    private Button difficultyButton;
    private CatFrameLockButton lockButton;
    private Button gameRulesButton;

    private boolean pendingLockState = false;

    public GuiWorldSettings(GuiScreen parentScreen, GameSettings gameSettings) {
        super(Text.translatable("difficultylocker.worldsettings.title"));
        this.parentScreen = parentScreen;
        this.gameSettings = gameSettings;
    }

    @Override
    protected void init() {
        // 如果从GameRuleEditor返回，应用待生效的游戏规则
        applyPendingGameRules();

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
        // === Difficulty button (left, narrowed to make room for the lock button) ===
        int diffBtnX = this.width / 2 - 155;
        int diffBtnY = this.height / 6 - 12;

        difficultyButton = Button.builder(
            Text.literal(gameSettings.getKeyBinding(GameSettings.Options.DIFFICULTY)),
            btn -> cycleDifficulty()
        ).pos(diffBtnX, diffBtnY).width(128).height(20).build();

        if (isHardcore) {
            difficultyButton.setActive(false);
            difficultyButton.setMessage(Text.literal(
                I18n.format("options.difficulty") + ": " + I18n.format("options.difficulty.hardcore")));
        } else if (isLocked) {
            difficultyButton.setActive(false);
        }

        // === 锁定按钮（难度按钮右侧） ===
        int lockBtnX = diffBtnX + 128 + 2; // 2px间距
        lockButton = new CatFrameLockButton(lockBtnX, diffBtnY, isLocked, btn -> handleLockButtonClick());

        if (isHardcore) {
            lockButton.setActive(false);
        } else if (isLocked && !DifficultyLocker.config.allowUnlock) {
            lockButton.setActive(false);
        }

        // === 游戏规则按钮（右侧） ===
        gameRulesButton = Button.builder(
            Text.literal(I18n.format("createworldui.button.gameRuleEditor")),
            btn -> openGameRuleEditor()
        ).pos(this.width / 2 + 5, diffBtnY).width(150).height(20).build();

        // 仅在创造模式 + 作弊模式同时启用时可用
        boolean isCreative = isCreativeMode();
        boolean cheatsEnabled = areCheatsEnabled();
        if (isCreative && cheatsEnabled) {
            gameRulesButton.setActive(true);
        } else {
            gameRulesButton.setActive(false);
            // 禁用原因通过 CatFrame 组件级 tooltip 说明（悬停时由 WidgetTooltipHolder 自动显示）
            // The disabled reason is conveyed via CatFrame's widget tooltip (auto-shown on hover).
            gameRulesButton.setTooltip(Tooltip.create(buildGameRulesDisabledTooltip(isCreative, cheatsEnabled)));
        }

        // === 完成按钮 ===
        Button doneButton = Button.builder(
            Text.literal(I18n.format("gui.done")),
            btn -> {
                mc.gameSettings.saveOptions();
                mc.displayGuiScreen(parentScreen);
            }
        ).pos(this.width / 2 - 100, this.height / 6 + 168).width(200).height(20).build();

        addRenderableWidget(difficultyButton);
        addRenderableWidget(lockButton);
        addRenderableWidget(gameRulesButton);
        addRenderableWidget(doneButton);
    }

    /**
     * 难度按钮点击 - 循环切换难度
     * Difficulty button click — cycle the difficulty and refresh the button text.
     */
    private void cycleDifficulty() {
        gameSettings.setOptionValue(GameSettings.Options.DIFFICULTY, 1);
        difficultyButton.setMessage(Text.literal(gameSettings.getKeyBinding(GameSettings.Options.DIFFICULTY)));
    }

    /**
     * 锁定按钮点击 - 弹出锁定/解锁确认对话框
     * Lock button click — show the lock/unlock confirmation dialog.
     */
    private void handleLockButtonClick() {
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

    /**
     * 构建游戏规则按钮的禁用原因 tooltip
     * Build the disabled-reason tooltip for the game-rules button.
     */
    private String buildGameRulesDisabledTooltip(boolean isCreative, boolean cheatsEnabled) {
        if (!isCreative && !cheatsEnabled) {
            return I18n.format("difficultylocker.gamerules.tooltip.needCreativeAndCheats");
        } else if (!isCreative) {
            return I18n.format("difficultylocker.gamerules.tooltip.needCreative");
        } else {
            return I18n.format("difficultylocker.gamerules.tooltip.needCheats");
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
        // 必须从服务端世界读取规则：1.7.10 没有 GameRules 的服务端→客户端同步机制，
        // WorldClient 的副本永远只有 9 条原版规则，模组新增的规则只存在于服务端世界。
        // Must read rules from the SERVER-side world: 1.7.10 has no server-to-client
        // GameRules sync, so the WorldClient copy only ever holds the 9 vanilla rules;
        // mod-added rules exist solely in the server world.
        World ruleSource = null;
        if (mc.getIntegratedServer() != null
            && mc.getIntegratedServer().worldServers != null
            && mc.getIntegratedServer().worldServers.length > 0
            && mc.getIntegratedServer().worldServers[0] != null) {
            ruleSource = mc.getIntegratedServer().worldServers[0];
        }
        // 兜底：无集成服务端（如远程服务器）时退回客户端副本 / Fallback: use the client copy when no integrated server (e.g. remote server)
        if (ruleSource == null) {
            ruleSource = mc.theWorld;
        }
        if (ruleSource != null) {
            Map<String, GameruleValue> allRules = GameRuleMonitorNSetter.getAllGamerules(ruleSource);
            for (Map.Entry<String, GameruleValue> entry : allRules.entrySet()) {
                currentRules.put(entry.getKey(), entry.getValue().stringValue);
            }
        }
        mc.displayGuiScreen(new IngameGameRuleScreen(this, currentRules));
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

                if (difficultyButton != null) difficultyButton.setActive(false);
                if (lockButton != null) {
                    lockButton.setLocked(true);
                    if (!DifficultyLocker.config.allowUnlock) lockButton.setActive(false);
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

                if (difficultyButton != null) difficultyButton.setActive(true);
                if (lockButton != null) {
                    lockButton.setLocked(false);
                    lockButton.setActive(true);
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
        // 背景与已注册组件（四个按钮）由 CatFrame 基类渲染
        // Background and registered widgets (the four buttons) are rendered by the CatFrame base.
        super.drawScreen(mouseX, mouseY, partialTicks);

        // 标题在组件之后绘制：与按钮区域不重叠，后绘不会遮盖任何组件
        // Title drawn after the widgets: it does not overlap the button area, so nothing is covered.
        this.drawCenteredString(this.fontRendererObj,
            this.getTitle().getString(),
            this.width / 2, 15, 0xFFFFFF);

        // 禁用按钮的 tooltip 由各组件自身的 WidgetTooltipHolder 自动泵动，帧末由 CatFrame 统一延迟绘制
        // Disabled-button tooltips are pumped by each widget's WidgetTooltipHolder and drawn
        // deferred by CatFrame at the end of the frame.
    }
}
