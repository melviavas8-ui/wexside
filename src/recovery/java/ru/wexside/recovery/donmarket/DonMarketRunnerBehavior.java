package ru.wexside.recovery.donmarket;

import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import org.joml.Matrix4f;

/** Retained module bridge from alternate temp_dump liIiIlIiIiIiIil, not a registered module. */
public final class DonMarketRunnerBehavior<S, C> {
    public interface BooleanValue { boolean get(); void set(boolean value); }
    public interface Controller { void tick(); void pause(); void reset(); }
    public interface Overlay {
        void tick(); boolean click(int button); boolean type(char character);
        void render(Matrix4f matrix); boolean input(int key);
    }
    public interface ControllerFactory<S,C> {
        Controller create(S session, C coordinator, BooleanSupplier active, Supplier<Rule> rule,
                          IntSupplier clickDelay, IntSupplier budget, Runnable completion);
    }
    public interface TickRegistrar { void register(Runnable listener); }
    public record Metadata(String id, String name, String description, String category, String[] aliases) {}
    public record Settings(BooleanValue enabled, IntSupplier minimumRate, IntSupplier maximumUnits,
                           IntSupplier clickDelay, IntSupplier budget) {
        public Settings { Objects.requireNonNull(enabled); Objects.requireNonNull(minimumRate);
            Objects.requireNonNull(maximumUnits); Objects.requireNonNull(clickDelay); Objects.requireNonNull(budget); }
    }
    public record InitialState(boolean paused, Controller controller) {}
    public record Rule(long minimumRate, int maximumUnits, Set<String> excludedNames) {}
    /** Original builder arguments. The meaning of original steps(double) remains an adapter contract. */
    public record HiddenNumberRecipe(String id, String label, String description, double minimum,
                                     double maximum, double initialValue, double steps, int originalIntegerOption,
                                     Supplier<Boolean> visible) {}
    public interface HiddenNumberFactory<N> { N build(HiddenNumberRecipe recipe); }

    private static DonMarketRunnerBehavior<?,?> instance;
    private final Metadata metadata;
    private final Settings settings;
    private final Supplier<S> session;
    private final Supplier<C> coordinator;
    private final ControllerFactory<S,C> factory;
    private final Overlay overlay;
    private boolean paused;
    private Controller controller;

    /** All erased initializer inputs are supplied explicitly. No constructor defaults are inferred. */
    public DonMarketRunnerBehavior(Metadata metadata, Settings settings, Supplier<S> session,
            Supplier<C> coordinator, ControllerFactory<S,C> factory, Overlay overlay, InitialState initial) {
        this.metadata=Objects.requireNonNull(metadata); this.settings=Objects.requireNonNull(settings);
        this.session=Objects.requireNonNull(session); this.coordinator=Objects.requireNonNull(coordinator);
        this.factory=Objects.requireNonNull(factory); this.overlay=Objects.requireNonNull(overlay);
        this.paused=Objects.requireNonNull(initial).paused(); this.controller=initial.controller();
    }
    /** Explicit bridge for the original static INSTANCE; original installation code is absent. */
    public static void installInstance(DonMarketRunnerBehavior<?,?> value) { instance=value; }
    public Metadata metadata() { return metadata; }
    public Controller controller() { return controller; }
    public boolean isPaused() { return paused; }
    public boolean isActive() { return settings.enabled().get() && !paused; }
    public Rule rule() { return new Rule((long)settings.minimumRate().getAsInt(), settings.maximumUnits().getAsInt(), Set.of()); }
    public void registerTick(TickRegistrar registrar) { Objects.requireNonNull(registrar).register(this::tick); }
    public <N> N hiddenNumber(HiddenNumberFactory<N> builder, String id, String label,
                              double minimum, double maximum, double initialValue, double steps) {
        return builder.build(new HiddenNumberRecipe(id,label,label,minimum,maximum,initialValue,steps,0,()->false));
    }
    public void togglePause() {
        paused=!paused;
        if(paused && controller!=null) controller.pause();
    }
    public void startOrTogglePause() {
        if(!settings.enabled().get()) { paused=false; settings.enabled().set(true); return; }
        togglePause();
    }
    public void disable() { settings.enabled().set(false); }
    public void stopAndReset() {
        settings.enabled().set(false); paused=false;
        if(controller!=null) controller.reset();
    }
    /** Original alternate IllllIIilII bci60..225; preserves repeated provider reads. */
    public void tick() {
        overlay.tick();
        if(controller==null) {
            if(session.get()==null) return;
            if(coordinator.get()==null) return;
            controller=factory.create(session.get(),coordinator.get(),this::isActive,this::rule,
                    settings.clickDelay(),settings.budget(),this::disable);
        }
        controller.tick();
    }
    public static boolean click(int button) {
        var current=instance;
        return current!=null && current.controller!=null && current.overlay.click(button);
    }
    public static boolean type(char character) { return instance!=null && instance.overlay.type(character); }
    public static void render(Matrix4f matrix) {
        var current=instance;
        if(current!=null && current.controller!=null) current.overlay.render(matrix);
    }
    public static boolean input(int key) { return instance!=null && instance.overlay.input(key); }
}
