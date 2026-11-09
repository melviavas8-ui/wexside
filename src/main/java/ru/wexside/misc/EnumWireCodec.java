package ru.wexside.misc;

import java.util.Optional;

public interface EnumWireCodec<E> {
    public E decode(byte var1);

    public Optional<E> tryDecode(byte var1);
}

