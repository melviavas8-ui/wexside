package ru.wexside.misc;

import java.util.Collection;
import java.util.List;
import net.minecraft.entity.LivingEntity;

public class TargetFilter {
    private final List<String> types;
    private final String sorting;
    private final int fov;

    public TargetFilter(Collection<String> types, String sorting, int fov) {
        this.types = List.copyOf(types);
        this.sorting = sorting;
        this.fov = fov;
    }

    public boolean matches(LivingEntity entity) {
        return entity != null;
    }

    public List<String> getTypes() {
        return this.types;
    }

    public String getSorting() {
        return this.sorting;
    }

    public int getFov() {
        return this.fov;
    }
}

