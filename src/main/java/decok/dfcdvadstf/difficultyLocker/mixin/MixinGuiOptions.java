package decok.dfcdvadstf.difficultyLocker.mixin;

import decok.dfcdvadstf.difficultyLocker.GuiLockButton;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(GuiOptions.class)
public abstract class MixinGuiOptions extends GuiScreen {

    @Shadow
    private GameSettings field_146443_h;

    @Unique
    private static final int LOCK_BUTTON_ID = 5001;

    @Unique
    private GuiButton modernDifficultyLocker$difficultyButton;
    @Unique
    private GuiLockButton modernDifficultyLocker$lockButton;
    @Unique
    private boolean modernDifficultyLocker$difficultyLocked = false;

    @Inject(method = "initGui", at = @At("TAIL"))
    private void addLockButton(CallbackInfo ci) {

        // Find difficulty button
        for (GuiButton b : (List<GuiButton>) this.buttonList) {
            if (b.id == GameSettings.Options.DIFFICULTY.returnEnumOrdinal()) {
                this.modernDifficultyLocker$difficultyButton = b;
                break;
            }
        }

        if (modernDifficultyLocker$difficultyButton == null) return;

        // Resize difficulty button
        int originalWidth = modernDifficultyLocker$difficultyButton.width; // 150
        int lockWidth = 20;
        int newWidth = originalWidth - lockWidth;

        modernDifficultyLocker$difficultyButton.width = newWidth;

        // Add lock button
        int x = modernDifficultyLocker$difficultyButton.xPosition + newWidth;
        int y = modernDifficultyLocker$difficultyButton.yPosition;

        modernDifficultyLocker$lockButton = new GuiLockButton(LOCK_BUTTON_ID, x, y, modernDifficultyLocker$difficultyLocked);
        this.buttonList.add(modernDifficultyLocker$lockButton);
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"))
    private void onLockButton(GuiButton button, CallbackInfo ci) {
        if (button.id == LOCK_BUTTON_ID) {

            modernDifficultyLocker$difficultyLocked = !modernDifficultyLocker$difficultyLocked;

            // Disable difficulty button
            modernDifficultyLocker$difficultyButton.enabled = !modernDifficultyLocker$difficultyLocked;

            // Update lock button texture + animation
            modernDifficultyLocker$lockButton.setLocked(modernDifficultyLocker$difficultyLocked);

            // Play click sound
            mc.getSoundHandler().playSound(
                    PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F)
            );
        }
    }
}
