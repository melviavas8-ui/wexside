package ru.wexside.recovery.crystal;

import ru.wexside.misc.RotationApplyResult;
import ru.wexside.util.RotationState;

/** Original IlliIlliliIllil strategy contract, with the richer recovered request. */
public interface CrystalRotationStrategy {
    RotationApplyResult step(RotationState state, CrystalRotationRequest request);
    void onDeactivated(RotationState state);
    void onActivated(RotationState state);
}
