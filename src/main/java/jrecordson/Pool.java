package jrecordson;

import java.util.ArrayDeque;
import java.util.function.Supplier;

public interface Pool<T> {

    T get();

    void put(T obj);

    static <T> Pool<T> create(Supplier<T> supplier) {
        return new DequePool<>(new ArrayDeque<>(), supplier);
    }

}
