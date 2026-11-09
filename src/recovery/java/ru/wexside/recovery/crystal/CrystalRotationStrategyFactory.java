package ru.wexside.recovery.crystal;

/** Original liliIlIlllIill factory contract; caller supplies real strategies. */
public interface CrystalRotationStrategyFactory {
    CrystalRotationStrategy create(String name);
}
