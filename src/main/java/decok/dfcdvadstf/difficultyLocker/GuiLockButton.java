package decok.dfcdvadstf.difficultyLocker;

import decok.dfcdvadstf.catframe.ui.components.WidgetSprites;
import decok.dfcdvadstf.catframe.ui.util.TextureStretching;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * 原版 GuiOptions 界面里的难度锁定按钮（属原版 {@link GuiButton} 阵营）。
 * 纹理与原 CatFrame 阵营的 {@link LockButton} 完全一致：按锁定状态与
 * 启用/悬停状态从 {@code textures/gui/} 下的六张分状态贴图选图，
 * 经 {@link TextureStretching#drawAuto} 按 {@code static} 规则 1:1 绘制。
 *
 * <p>
 * Lock-difficulty button for the vanilla GuiOptions screen (vanilla
 * {@link GuiButton} lineage). Textures match the CatFrame-side
 * {@link LockButton}: the six per-state images under {@code textures/gui/} are
 * chosen by lock / enabled / hover state and drawn 1:1 via
 * {@link TextureStretching#drawAuto} following their {@code static} rule.
 * </p>
 */
public class GuiLockButton extends GuiButton {

    /** 状态贴图的原始尺寸（六张贴图均为 20×20，与各自 mcmeta 的 default 一致）。 */
    private static final int TEX_SIZE = 20;

    private static final WidgetSprites LOCKED_SPRITES = new WidgetSprites(
            texture("locked.png"),
            texture("locked_disabled.png"),
            texture("locked_highlighted.png"));

    private static final WidgetSprites UNLOCKED_SPRITES = new WidgetSprites(
            texture("unlocked.png"),
            texture("unlocked_disabled.png"),
            texture("unlocked_highlighted.png"));

    private boolean locked;

    public GuiLockButton(int id, int x, int y, boolean locked) {
        super(id, x, y, 20, 20, "");
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

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!this.visible) return;

        boolean hovered = mouseX >= this.xPosition && mouseX < this.xPosition + this.width &&
                mouseY >= this.yPosition && mouseY < this.yPosition + this.height;

        WidgetSprites sprites = this.locked ? LOCKED_SPRITES : UNLOCKED_SPRITES;
        ResourceLocation sprite = sprites.get(this.enabled, hovered);

        // 原版屏幕的渲染循环不保证混合状态，而 drawAuto 结束后会关闭 GL_BLEND；
        // 这里保存并复原，避免影响同屏其它原版按钮的绘制。
        // Vanilla screens do not guarantee the blend state and drawAuto disables
        // GL_BLEND when done — save and restore so sibling buttons are unaffected.
        boolean blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        TextureStretching.drawAuto(sprite, this.xPosition, this.yPosition, this.width, this.height,
                TextureStretching.StretchType.STATIC, TEX_SIZE, TEX_SIZE, 0, 0, 0, 0);
        if (blendEnabled) {
            GL11.glEnable(GL11.GL_BLEND);
        } else {
            GL11.glDisable(GL11.GL_BLEND);
        }

        // 重置GL颜色为纯白——保持纹理原始设计颜色，不继承前序按钮的染色状态
        // Reset the GL colour to pure white — keep the texture's original colours.
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
