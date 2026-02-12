# Gravestones Plugin API

This document describes the public API available for external plugins to integrate with GravestonePlugin.

## Getting Started

Access the plugin instance and manager:

```java
GravestonePlugin plugin = GravestonePlugin.getInstance();
if (plugin != null) {
    GravestoneManager manager = plugin.getGravestoneManager();
    // Use the API
}
```

---

## Access Control

### GravestoneAccessChecker

Register a custom access checker to control who can collect or break gravestones. The checker is called **before** the built-in owner protection check and returns a tri-state `AccessResult`.

```java
@FunctionalInterface
public interface GravestoneAccessChecker {

    enum AccessResult {
        ALLOW,  // Allow access, skip the built-in ownership check
        DENY,   // Deny access immediately
        DEFER   // Don't interfere, fall through to built-in ownership check
    }

    AccessResult canAccess(UUID accessorUuid, UUID ownerUuid, int x, int y, int z, String worldName);
}
```

**Registration:**

```java
GravestoneManager manager = GravestonePlugin.getInstance().getGravestoneManager();
manager.setAccessChecker((accessor, owner, x, y, z, world) -> {
    // Full control: ALLOW bypasses ownership, DENY blocks, DEFER lets built-in check decide
    if (myPlugin.isAdmin(accessor)) {
        return GravestoneAccessChecker.AccessResult.ALLOW;  // Bypass ownership
    }
    if (myPlugin.isBlockedTerritory(accessor, x, y, z, world)) {
        return GravestoneAccessChecker.AccessResult.DENY;   // Block access
    }
    return GravestoneAccessChecker.AccessResult.DEFER;      // Let normal ownership check handle it
});
```

**AccessResult behavior:**

| Result | Access | Ownership Check |
|--------|--------|-----------------|
| `ALLOW` | Granted | Skipped |
| `DENY` | Blocked | Skipped |
| `DEFER` | -- | Runs normally |

**Notes:**
- Called in `CollectGravestoneInteraction`, `BreakGravestoneInteraction`, and `GravestoneBlockState.canOpen()`/`canDestroy()`
- If no checker is registered, the built-in owner protection check applies as normal
- The checker receives `ownerUuid` which may be `null` if the gravestone owner is unknown

---

## Events

All events implement `IEvent<Void>` and are dispatched on the Hytale event bus. Register listeners using the standard event registry.

### GravestonePreCreateEvent (Cancellable)

Fired **before** a gravestone is placed. Cancel to prevent creation.

```java
eventRegistry.registerGlobal(GravestonePreCreateEvent.class, event -> {
    if (shouldPreventGravestone(event.getOwnerUuid(), event.getX(), event.getY(), event.getZ())) {
        event.setCancelled(true);
    }
});
```

| Method | Returns | Description |
|--------|---------|-------------|
| `getOwnerUuid()` | `UUID` | Player who died |
| `getX()`, `getY()`, `getZ()` | `int` | Death position |
| `getWorldName()` | `String` | World name |
| `isCancelled()` | `boolean` | Whether creation is cancelled |
| `setCancelled(boolean)` | `void` | Cancel or un-cancel creation |

**Note:** When cancelled, items are not preserved in a gravestone. The death config's vanilla behavior applies (items may be lost depending on server config).

### GravestoneCreatedEvent

Fired **after** a gravestone is successfully created and data is saved.

```java
eventRegistry.registerGlobal(GravestoneCreatedEvent.class, event -> {
    log("Gravestone created for " + event.getOwnerUuid() + " at " + event.getX() + "," + event.getY() + "," + event.getZ());
});
```

| Method | Returns | Description |
|--------|---------|-------------|
| `getOwnerUuid()` | `UUID` | Player who died |
| `getX()`, `getY()`, `getZ()` | `int` | Gravestone position |
| `getWorldName()` | `String` | World name |

### GravestoneCollectedEvent

Fired when a player collects items from a gravestone (right-click).

```java
eventRegistry.registerGlobal(GravestoneCollectedEvent.class, event -> {
    log(event.getCollectorUuid() + " collected gravestone of " + event.getOwnerUuid());
});
```

| Method | Returns | Description |
|--------|---------|-------------|
| `getCollectorUuid()` | `UUID` | Player who collected |
| `getOwnerUuid()` | `UUID` | Gravestone owner (may be null) |
| `getX()`, `getY()`, `getZ()` | `int` | Gravestone position |
| `getWorldName()` | `String` | World name |

### GravestoneBrokenEvent

Fired when a gravestone block is broken (by any means).

```java
eventRegistry.registerGlobal(GravestoneBrokenEvent.class, event -> {
    log("Gravestone broken at " + event.getX() + "," + event.getY() + "," + event.getZ());
});
```

| Method | Returns | Description |
|--------|---------|-------------|
| `getBreakerUuid()` | `UUID` | Player who broke it (may be null) |
| `getOwnerUuid()` | `UUID` | Gravestone owner (may be null) |
| `getX()`, `getY()`, `getZ()` | `int` | Gravestone position |
| `getWorldName()` | `String` | World name (may be empty) |

**Note:** `breakerUuid` is null when the break event doesn't provide entity context (e.g., timer-based despawn, programmatic removal).

---

## Query Methods

### Get Player's Gravestones

```java
List<GravestoneData> graves = manager.getPlayerGravestones(playerUuid);
// Returns unmodifiable list, or empty list if none
```

### Get Gravestone at Position

```java
GravestoneData data = manager.getGravestoneAt(x, y, z, worldName);
// Returns null if no gravestone at that exact position
```

### Count Player's Gravestones

```java
int count = manager.getGravestoneCount(playerUuid);
```

### Get Gravestone Owner

```java
UUID owner = manager.getGravestoneOwner(x, y, z);
// Searches across all worlds by position suffix
```

---

## Programmatic Actions

### Destroy a Gravestone

Removes the gravestone data and breaks the block in-world:

```java
boolean removed = manager.destroyGravestone(x, y, z, worldName);
// Returns true if a gravestone was found and removed
```

---

## GravestoneData

Read-only data model for gravestone information:

| Method | Returns | Description |
|--------|---------|-------------|
| `getPlayerId()` | `UUID` | Owner's UUID |
| `getX()`, `getY()`, `getZ()` | `int` | Block coordinates |
| `getWorldName()` | `String` | World name |
| `getCreatedTime()` | `long` | Creation timestamp (millis) |
| `getLocationKey()` | `String` | Unique key: `worldName:x,y,z` |
