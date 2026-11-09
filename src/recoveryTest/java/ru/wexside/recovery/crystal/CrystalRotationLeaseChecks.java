package ru.wexside.recovery.crystal;

/** Plain JVM regression for lease identity and release ordering. */
public final class CrystalRotationLeaseChecks {
    public static void main(String[] args) {
        CrystalRotationLease lease = new CrystalRotationLease();
        Object owner = new String("same");
        Object equalOwner = new String("same");
        int[] calls = {0};
        lease.acquire(owner);
        if (!lease.isOwnedBy(owner) || lease.isOwnedBy(equalOwner)) throw new AssertionError("Identity ownership");
        lease.release(equalOwner, () -> calls[0]++);
        if (calls[0] != 0 || lease.isOwnedBy(owner)) throw new AssertionError("Mismatched release must still clear");
        lease.acquire(owner);
        lease.release(owner, () -> {
            if (lease.isOwnedBy(owner)) throw new AssertionError("Clear precedes release callback");
            calls[0]++;
        });
        if (calls[0] != 1) throw new AssertionError("Exactly one matching release callback");
        lease.acquire(owner);
        lease.clear();
        if (lease.isOwnedBy(owner)) throw new AssertionError("Clear");
        System.out.println("CRYSTAL ROTATION LEASE CHECKS PASS");
    }
}
