package ru.wexside.misc;

import java.util.HashMap;
import java.util.Map;
import ru.wexside.misc.HumanizedRotationStrategy;
import ru.wexside.misc.RandomizedRotationStrategy;
import ru.wexside.misc.RotationStrategy;
import ru.wexside.misc.RotationStrategyFactory;
import ru.wexside.misc.SmoothRotationStrategy;
import ru.wexside.util.RandomizedDirectRotationStrategy;
import ru.wexside.util.SpookyRotationStrategy;

public final class SpookyTest
implements RotationStrategyFactory {
    private final Map<String, RotationStrategy> strategiesByName = new HashMap<String, RotationStrategy>();

    public void update() {
        this.strategiesByName.put("Simple", new SmoothRotationStrategy());
        this.strategiesByName.put("FT Snap", new HumanizedRotationStrategy());
        this.strategiesByName.put("RW", new RandomizedRotationStrategy());
        this.strategiesByName.put("Spooky", new RandomizedDirectRotationStrategy());
        this.strategiesByName.put("Spooky Test", new SpookyRotationStrategy());
    }

    @Override
    public RotationStrategy getStrategy(String string) {
        return this.strategiesByName.getOrDefault(string, this.strategiesByName.get("Simple"));
    }
}

