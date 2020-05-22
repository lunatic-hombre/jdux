package jdux;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

final class Streams {

    public Streams() {
        throw new AssertionError("This is a static utility class.");
    }

    public static <T> Stream<T> toStream(Queue<T> queue) {
        return Stream.generate(queue::remove).takeWhile(n -> !queue.isEmpty());
    }

    public static <T> Stream<T> toStream(Iterable<? extends T> iterable) {
        return StreamSupport.stream((Spliterator<T>) iterable.spliterator(), false);
    }

    public static <T> Stream<T> toStream(Iterator<? extends T> iterator) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
    }

    public static <T> Stream<T> toStream(Optional<? extends T> optional) {
        return optional.isPresent() ? Stream.of(optional.get()) : Stream.empty();
    }

    public static <T, U> Stream<T> merge(Stream<? extends T> trunk, Stream<? extends T> branch, Function<? super T, U> mapping) {
        return Stream.concat(branch, trunk).filter(distinctByKey(mapping));
    }

    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    public static <T> Predicate<T> after(Predicate<T> condition) {
        return new Predicate<>() {
            boolean found = false;
            @Override
            public boolean test(T t) {
                try {
                    return found;
                } finally {
                    found = condition.test(t);
                }
            }
        };
    }

}
