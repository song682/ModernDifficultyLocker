package decok.dfcdvadstf.difficultyLocker;

import decok.dfcdvadstf.catframe.ui.GuiGraphicsExtractor;
import decok.dfcdvadstf.catframe.ui.Text;
import decok.dfcdvadstf.catframe.ui.components.Button;
import decok.dfcdvadstf.catframe.ui.components.WidgetSprites;
import decok.dfcdvadstf.catframe.ui.util.TextureStretching;
import net.minecraft.util.ResourceLocation;

/**
 * 世界设置界面的难度锁定按钮 - 继承 CatFrame 的 {@link Button}（CatFrame 生命周期），
 * 交互（点击回调、悬停检测、启用/禁用）由基类处理；
 * 渲染时仅按锁定状态与悬停/禁用状态绘制锁按钮纹理，不绘制按钮背景与文本，
 * 对标 CatFrame 内置的 {@code ImageButton} 的"纯纹理"渲染方式。
 *
 * <p>
 * Lock-difficulty button for the world-settings screen — extends CatFrame's
 * {@link Button} so it runs on the CatFrame lifecycle: interaction (click
 * callback, hover detection, enabled/disabled) is provided by the base class;
 * rendering draws only the lock texture for the current locked / hovered /
 * disabled state, skipping the button background and text like CatFrame's
 * built-in {@code ImageButton}.
 * </p>
 * <p>
 * 纹理来源为 {@code assets/difficultylocker/textures/gui/} 下按状态拆分的六张贴图
 * （locked / unlocked × 正常 / 悬停 / 禁用），由 {@link WidgetSprites} 按状态选图，
 * {@link TextureStretching#drawAuto} 依各自 {@code .mcmeta} 的 {@code static} 规则
 * 以 20×20 原始尺寸 1:1 绘制，纹理经 CatFrame UI 图集（{@code difficultylocker:gui}）复用。
 * </p>
 * <p>
 * Textures come from the six per-state images under
 * {@code assets/difficultylocker/textures/gui/} (locked / unlocked × normal /
 * hover / disabled): {@link WidgetSprites} picks the state image and
 * {@link TextureStretching#drawAuto} draws it 1:1 at the native 20×20 size using
 * each image's {@code static} {@code .mcmeta} rule, reusing the CatFrame UI atlas
 * ({@code difficultylocker:gui}).
 * </p>
 */
public class LockButton extends Button {

    /** 状态贴图的原始尺寸（六张贴图均为 20×20，与各自 mcmeta 的 default 一致）。 */
    private static final int TEX_SIZE = 20;

    /** 已锁定状态的三态贴图 / three-state sprites for the locked look */
    private static final WidgetSprites LOCKED_SPRITES = new WidgetSprites(
            texture("locked.png"),
            texture("locked_disabled.png"),
            texture("locked_highlighted.png"));

    /** 未锁定状态的三态贴图 / three-state sprites for the unlocked look */
    private static final WidgetSprites UNLOCKED_SPRITES = new WidgetSprites(
            texture("unlocked.png"),
            texture("unlocked_disabled.png"),
            texture("unlocked_highlighted.png"));

    private boolean locked;

    public LockButton(int x, int y, boolean locked, OnPress onPress) {
        super(x, y, 20, 20, Text.literal(""), onPress);
        this.locked = locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public boolean isLocked() {
        return locked;
    }

    /** 拼出本模组 GUI 贴图的资源位置 / build a GUI texture location of this mod */
    private static ResourceLocation texture(String file) {
        return new ResourceLocation(Tags.MODID, "textures/gui/" + file);
    }

    /**
     * 仅绘制锁纹理 —— 不调用 super，避免 Button 的背景与文本渲染。
     * 可见性与悬停状态已由 {@link #extractRenderState} 在调用前处理
     * （{@code !active} 时即为禁用行，{@code isHovered}/{@code focused} 为高亮行）。
     *
     * <p>
     * Draw only the lock texture — no super call, so the Button background/text
     * are skipped. Visibility and hover state have already been handled by
     * {@link #extractRenderState} before this is called.
     * </p>
     */
    @Override
    protected void renderWidget(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        WidgetSprites sprites = this.locked ? LOCKED_SPRITES : UNLOCKED_SPRITES;
        ResourceLocation sprite = sprites.get(this.active, this.isHovered || this.focused);

        // static 拉伸：目标尺寸即贴图原始尺寸，1:1 像素完美；
        // 纹理属于 difficultylocker:gui 图集时由 drawAuto 自动改用图集 UV。
        // Static stretch at the native texture size (1:1, pixel-perfect);
        // drawAuto switches to atlas UVs when the sprite is in difficultylocker:gui.
        TextureStretching.drawAuto(sprite, this.x, this.y, this.width, this.height,
                TextureStretching.StretchType.STATIC, TEX_SIZE, TEX_SIZE, 0, 0, 0, 0);
    }
}
