package ru.wexside.misc;

import ru.wexside.misc.RotationApplyResult;
import ru.wexside.util.RotationIntent;
import ru.wexside.util.RotationState;

public interface RotationStrategy {
    public RotationApplyResult applyRotation(RotationState var1, RotationIntent var2);

    public void onDeactivated(RotationState var1);

    public void onActivated(RotationState var1);
}

