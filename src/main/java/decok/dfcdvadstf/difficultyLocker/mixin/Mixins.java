package decok.dfcdvadstf.difficultyLocker.mixin;

import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;

import javax.annotation.Nonnull;

public enum Mixins implements IMixins {
    GUI_LOCKER(Side.CLIENT, "middle.MixinGuiOptions"),

    INTEGRATE_SERVER(Side.COMMON, "middle.MixinIntegratedServer");

    private final MixinBuilder builder;

    Mixins(Side side, String... mixins) {
        this.builder = new MixinBuilder().addSidedMixins(side, mixins);
    }

    @Nonnull
    @Override
    public MixinBuilder getBuilder() {
        return this.builder;
    }
}
