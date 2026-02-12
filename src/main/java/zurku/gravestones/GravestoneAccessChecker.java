package zurku.gravestones;

import java.util.UUID;

/**
 * Callback interface for external plugins to control gravestone access.
 * When registered via {@link GravestoneManager#setAccessChecker(GravestoneAccessChecker)},
 * this checker is called before the built-in owner protection check.
 *
 * @see GravestoneManager#setAccessChecker(GravestoneAccessChecker)
 */
@FunctionalInterface
public interface GravestoneAccessChecker {

    /**
     * Result of an access check.
     */
    enum AccessResult {
        /** Allow access and skip the built-in ownership check. */
        ALLOW,
        /** Deny access immediately (ownership check is not reached). */
        DENY,
        /** Don't interfere — fall through to the built-in ownership check. */
        DEFER
    }

    /**
     * Called when a player attempts to collect or break a gravestone.
     *
     * @param accessorUuid UUID of the player attempting access
     * @param ownerUuid    UUID of the gravestone owner (may be null if unknown)
     * @param x            block X coordinate
     * @param y            block Y coordinate
     * @param z            block Z coordinate
     * @param worldName    name of the world containing the gravestone
     * @return {@link AccessResult#ALLOW} to grant access (bypasses ownership check),
     *         {@link AccessResult#DENY} to block access,
     *         {@link AccessResult#DEFER} to fall through to the built-in ownership check
     */
    AccessResult canAccess(UUID accessorUuid, UUID ownerUuid, int x, int y, int z, String worldName);
}
