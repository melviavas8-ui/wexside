package ru.wexside.recovery.crystal;

/** Identity lease recovered from original IiIlllIllililil bytecode. */
public final class CrystalRotationLease {
    private Object owner;

    public void acquire(Object owner) {
        this.owner = owner;
    }

    public boolean isOwnedBy(Object owner) {
        return this.owner != null && this.owner == owner;
    }

    public void clear() {
        this.owner = null;
    }

    public void release(Object owner, Runnable releaseAction) {
        boolean owned = this.isOwnedBy(owner);
        this.owner = null;
        if (owned) {
            releaseAction.run();
        }
    }
}
