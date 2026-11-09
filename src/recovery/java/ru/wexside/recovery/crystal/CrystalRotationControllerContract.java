package ru.wexside.recovery.crystal;

import ru.wexside.util.Angle;

/** Exact controller surface consumed by CrystalAura, not a substitute controller implementation. */
public interface CrystalRotationControllerContract {
    /** Original IlIiIillIlIllil.liIIliilI: identity token of active request. */
    Object currentRequestOwner();

    /** Original IlIiIillIlIllil.iIiIiIilI: currently applied angle, possibly null. */
    Angle currentAngle();

    /** Original llIIliilI(request,"Simple") applies a request with the named strategy. */
    void apply(CrystalRotationRequest request, String strategyName);

    /** Original release(): clears controller request ownership and rotation state. */
    void release();
}
