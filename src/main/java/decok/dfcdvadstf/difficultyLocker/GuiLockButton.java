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

        // 重置GL颜色为纯白——不然前面的按钮可能把颜色改成了灰色
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        boolean hovered = mouseX >= this.xPosition && mouseX < this.xPosition + this.width &&
                mouseY >= this.yPosition && mouseY < this.yPosition + this.height;

        int col = locked ? 0 : 20;
        int row;

        if (!this.enabled) row = 40;
        else if (hovered) row = 20;
        else row = 0;

        drawTexturedModalRect(this.xPosition, this.yPosition, col, row, 20, 20);
    }
}
