package decok.dfcdvadstf.difficultyLocker;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class GuiLockButton extends GuiButton {
    private static final ResourceLocation LOCK_TEXTURES = new ResourceLocation("difficultylocker", "textures/gui/lock_button.png");
    private boolean locked;
    private float animScale = 1.0F;

    public GuiLockButton(int id, int x, int y, boolean locked) {
        super(id, x, y, 20, 20, "");
        this.locked = locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
        this.animScale = 1.3F;
        // trigger pop animation
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY){
        if (!this.visible) return;
        mc.getTextureManager().bindTexture(LOCK_TEXTURES);
        boolean hovered = mouseX >= this.xPosition && mouseX < this.xPosition + this.width &&
                            mouseY >= this.yPosition && mouseY < this.yPosition + this.height;

        int col = locked? 0 : 20;
        int row;

        if (!this.enabled) row = 40;
        else if (hovered) row = 20;
        else row = 0;

        // --- Animation ---
        if (animScale > 1.0F) {
            animScale -= 0.05F;
            if (animScale < 1.0F) animScale = 1.0F;
        }

        GL11.glPushMatrix();
        GL11.glTranslatef(this.xPosition + 10, this.yPosition + 10, 0);
        GL11.glScalef(animScale, animScale, 1);
        GL11.glTranslatef(-10, -10, 0); drawTexturedModalRect(0, 0, col, row, 20, 20);
        GL11.glPopMatrix();
    }
}
