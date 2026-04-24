package decok.dfcdvadstf.difficultyLocker;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class GuiLockButton extends GuiButton {
    private static final ResourceLocation LOCK_TEXTURES = new ResourceLocation("difficultylocker", "textures/gui/lock_button.png");
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

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY){
        if (!this.visible) return;
        mc.getTextureManager().bindTexture(LOCK_TEXTURES);

        // 重置GL颜色状态，防止被前一个按钮的绘制残留影响
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        // 与原版 GuiButton 一致的亮度分级
        float brightness;
        if (!this.enabled) {
            brightness = 0.5F;
        } else {
            boolean hovered = mouseX >= this.xPosition && mouseX < this.xPosition + this.width &&
                    mouseY >= this.yPosition && mouseY < this.yPosition + this.height;
            brightness = hovered ? 1.0F : 0.8F;
        }
        GL11.glColor4f(brightness, brightness, brightness, 1.0F);

        int col = locked ? 0 : 20;
        int row;

        if (!this.enabled) row = 40;
        else if (brightness > 0.9F) row = 20; // hovered
        else row = 0;

        drawTexturedModalRect(this.xPosition, this.yPosition, col, row, 20, 20);

        // 恢复GL颜色状态
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
