package ru.wexside.recovery.warden;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Complete behavioral extraction of lillIililililil; duplicate destinations remain duplicates. */
public final class WardenAnarchyRoute {
    private final List<Integer> destinations = new ArrayList<>();
    private int index;
    private int home = -1;

    public void start(int home, List<Integer> candidates) {
        this.home = home;
        destinations.clear();
        for (int candidate : candidates) {
            if (candidate != home) {
                destinations.add(candidate);
            }
        }
        Collections.shuffle(destinations);
        index = 0;
    }

    public int home() {
        return home;
    }

    public int target() {
        return destinations.isEmpty() ? -1 : destinations.get(index);
    }

    public void advance() {
        if (!destinations.isEmpty()) {
            index = (index + 1) % destinations.size();
        }
    }

    public boolean ready() {
        return home >= 0 && !destinations.isEmpty();
    }
}
