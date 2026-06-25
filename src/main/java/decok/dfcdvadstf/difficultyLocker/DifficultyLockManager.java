package decok.dfcdvadstf.difficultyLocker;

import net.minecraft.client.Minecraft;
import java.util.HashMap;
import java.util.Map;

public class DifficultyLockManager {
    private static final Map<String, Boolean> worldLockStates = new HashMap<>();

    public static boolean isDifficultyLocked() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) return false;

        String worldKey = getCurrentWorldKey();
        return worldLockStates.getOrDefault(worldKey, false);
    }

    public static void setDifficultyLocked(boolean locked) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) return;

        String worldKey = getCurrentWorldKey();
        worldLockStates.put(worldKey, locked);
    }

    private static String getCurrentWorldKey() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.isSingleplayer() && mc.theWorld != null) {
            // 对于单人游戏，使用世界文件夹名称
            return mc.getIntegratedServer().getWorldName();
        }
        return "unknown";
    }
}