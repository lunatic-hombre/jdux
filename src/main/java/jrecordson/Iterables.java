package jrecordson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static jrecordson.Shorthands.skip;

class Iterables {

    /**
     * Takes a single-use iterator and turns it into multi-use by maintaining records in a list for future iteration.
     */
    public static <E> LazyLoadingIterable<E> recalling(Iterator<E> iterator) {
        return new ListLoadingIterable<>(iterator);
    }

    /**
     * Given some iterators, treat them as a single one, with each leading into the next.
     */
    @SafeVarargs
    public static <E> Iterator<E> concat(Iterator<? extends E>... iterators) {
        return new IteratorChain<>(new ArrayIterator<>(iterators));
    }

    /**
     * Apply a side-effect to whenever next() is called.
     */
    public static <E> Iterator<E> onNext(Iterator<E> iterator, Consumer<? super E> consumer) {
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }
            @Override
            public E next() {
                final E next = iterator.next();
                consumer.accept(next);
                return next;
            }
        };
    }

    static class ArrayIterator<E> implements Iterator<E> {

        final E[] arr;
        AtomicInteger index = new AtomicInteger();

        public ArrayIterator(E[] arr) {
            this.arr = arr;
        }

        @Override
        public boolean hasNext() {
            return index.get() < arr.length;
        }

        @Override
        public E next() {
            return arr[index.getAndIncrement()];
        }

    }

    static class IteratorChain<E> implements Iterator<E> {

        final Iterator<Iterator<? extends E>> iterators;
        Iterator<? extends E> current;

        IteratorChain(Iterator<Iterator<? extends E>> iterators) {
            this.iterators = iterators;
            this.current = Collections.emptyIterator();
            hasNextIterator(); // find first non-empty iterator
        }

        @Override
        public boolean hasNext() {
            return current.hasNext() || hasNextIterator();
        }

        private boolean hasNextIterator() {
            while (!current.hasNext() && iterators.hasNext())
                current = iterators.next();
            return current.hasNext();
        }

        @Override
        public E next() {
            return current.next();
        }

        @Override
        public void remove() {
            current.remove();
        }

    }


    static class ListLoadingIterable<E> implements LazyLoadingIterable<E> {

        final List<E> memory;
        final Iterator<E> unread;

        ListLoadingIterable(Iterator<E> unread) {
            this.memory = new ArrayList<>();
            this.unread = onNext(unread, memory::add);
        }

        @Override
        public Iterator<E> iterator() {
            return concat(memory.subList(0, memory.size()).iterator(), unread);
        }

        @Override
        public void load() {
            unread.forEachRemaining(skip());
        }
    }

}
