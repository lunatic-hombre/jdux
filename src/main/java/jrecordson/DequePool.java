package jrecordson;

import java.util.Deque;
import java.util.function.Supplier;

class DequePool<T> implements Pool<T> {

    final Deque<T> deque;
    final Supplier<T> supplier;

    DequePool(Deque<T> list, Supplier<T> supplier) {
        this.deque = list;
        this.supplier = supplier;
    }

    @Override
    public T get() {
        final T item = deque.poll();
        return item == null ? supplier.get() : item;
    }

    @Override
    public void put(T item) {
        deque.add(item);
    }
}
