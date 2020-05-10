package jrecordson;

import java.util.Iterator;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

final class Streams {

    public Streams() {
        throw new AssertionError("This is a static utility class.");
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

    public static <T> Predicate<T> after(Predicate<T> condition) {
        return new Predicate<T>() {
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
