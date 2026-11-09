package ru.wexside;

import net.minecraft.client.render.RenderTickCounter;
import ru.wexside.module.movement.TimerModule;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.NumberSetting;

/** Run inside Fabric after mixins and WexSideClient initialization. */
public final class ModuleHookChecks {
    public static void run() {
        if (WexSideClient.getInstance().getModuleManager().getModule(ru.wexside.module.render.PredictionsModule.class) == null
                || WexSideClient.getInstance().getModuleManager().getModule(ru.wexside.module.misc.HeadHitterModule.class) == null) {
            throw new AssertionError("Recovered module registrations");
        }
        TimerModule timer = WexSideClient.getInstance().getModuleManager().getModule(TimerModule.class);
        if (timer == null) throw new AssertionError("Timer registration");
        BooleanSetting enabled = (BooleanSetting) timer.getToggleSetting();
        NumberSetting speed = (NumberSetting) timer.getSettings().stream()
                .filter(setting -> "speed".equals(setting.getId())).findFirst().orElseThrow();
        boolean previousEnabled = enabled.isEnabled();
        double previousSpeed = speed.getValue();
        try {
            enabled.setEnabled(false);
            require(new RenderTickCounter.Dynamic(20.0f, 1000L, duration -> duration)
                    .beginRenderTick(1100L, true) == 2, "Disabled timer preserves vanilla tick count");
            speed.setValue(2.0);
            enabled.setEnabled(true);
            require(new RenderTickCounter.Dynamic(20.0f, 1000L, duration -> duration)
                    .beginRenderTick(1100L, true) == 4, "Enabled timer scales actual Dynamic tick count");
            require(new RenderTickCounter.Dynamic(20.0f, 1000L, duration -> duration + 50.0f)
                    .beginRenderTick(1100L, true) == 2, "Scale applies after targetMillisPerTick operator");
            require(new RenderTickCounter.Dynamic(20.0f, 1000L, duration -> duration)
                    .beginRenderTick(1100L, false) == 0, "Tick-disabled branch remains unchanged");
        } finally {
            speed.setValue(previousSpeed);
            enabled.setEnabled(previousEnabled);
        }
        System.out.println("MODULE HOOK CHECKS PASS: Timer Dynamic injection, operator order, disabled branch");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
