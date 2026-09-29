package decok.dfcdvadstf.difficultyLocker;

import decok.dfcdvadstf.catframe.ui.GuiGraphicsExtractor;
import decok.dfcdvadstf.catframe.ui.Text;
import decok.dfcdvadstf.catframe.ui.components.Button;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

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
 */
public class CatFrameLockButton extends Button {
    private static final ResourceLocation LOCK_TEXTURES = new ResourceLocation("difficultylocker", "textures/gui/lock_button.png");
    private boolean locked;

    public CatFrameLockButton(int x, int y, boolean locked, OnPress onPress) {
        super(x, y, 20, 20, Text.literal(""), onPress);
        this.locked = locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public boolean isLocked() {
        return locked;
    }

    /**
     * 仅绘制锁纹理 —— 不调用 super，避免 Button 的背景与文本渲染。
     * 可见性与悬停状态已由 {@link #extractRenderState} 在调用前处理
     * （{@code !active} 时即为禁用行，{@code isHovered} 为悬停行）。
     *
     * <p>
     * Draw only the lock texture — no super call, so the Button background/text
     * are skipped. Visibility and hover state have already been handled by
     * {@link #extractRenderState} before this is called.
     * </p>
     */
    @Override
    protected void renderWidget(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.getTextureManager().bindTexture(LOCK_TEXTURES);

        // 重置GL颜色为纯白——保持纹理原始设计颜色，不继承前序组件的染色状态
        // Reset the GL colour to pure white — keep the texture's original colours,
        // never inherit any tint left over by previously drawn widgets.
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        int col = locked ? 0 : 20;
        int row;

        if (!this.active) row = 40;
        else if (this.isHovered) row = 20;
        else row = 0;

        this.vanillaGui.drawTexturedModalRect(this.x, this.y, col, row, 20, 20);
    }
}
