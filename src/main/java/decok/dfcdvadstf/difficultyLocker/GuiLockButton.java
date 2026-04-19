package decok.dfcdvadstf.difficultyLocker;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;

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
