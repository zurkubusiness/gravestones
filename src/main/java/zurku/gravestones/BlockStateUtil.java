package zurku.gravestones;

import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;
import java.lang.reflect.Method;

@SuppressWarnings("removal")
public final class BlockStateUtil {

    private static Method getStateMethod;
    private static Method getBlockStateMethod;

    private BlockStateUtil() {}

    public static BlockState getBlockState(World world, int x, int y, int z) {
        BlockState state = null;

        try {
            if (getStateMethod == null) {
                getStateMethod = world.getClass().getMethod("getState", int.class, int.class, int.class, boolean.class);
            }
            state = (BlockState) getStateMethod.invoke(world, x, y, z, true);
            if (state != null) return state;
        } catch (Exception ignored) {}

        try {
            long key = ((long) (x >> 4) << 32) | ((long) (z >> 4) & 0xFFFFFFFFL);
            WorldChunk chunk = world.getChunkIfLoaded(key);
            if (chunk != null) {
                state = chunk.getState(x, y, z);
                if (state != null) return state;
            }
        } catch (Exception ignored) {}

        try {
            if (getBlockStateMethod == null) {
                getBlockStateMethod = world.getClass().getMethod("getBlockState", int.class, int.class, int.class);
            }
            state = (BlockState) getBlockStateMethod.invoke(world, x, y, z);
        } catch (Exception ignored) {}

        return state;
    }
}
