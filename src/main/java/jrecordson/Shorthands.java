package jrecordson;

import java.util.function.Consumer;
import java.util.function.Function;

final class Shorthands {

    private Shorthands() {
        throw new AssertionError("This is a static utility class.");
    }

    public static <T> T then(T t, Runnable after) {
        try {
            return t;
        } finally {
            after.run();
        }
    }

    public static <T> T apply(T t, Consumer<? super T> consumer) {
        consumer.accept(t);
        return t;
    }

    public static <T> Consumer<T> skip() {
        return t -> {};
    }


    public static <I,O> Function<I,O> unchecked(CheckedFunction<I,O> f,
                                                Function<Throwable, ? extends RuntimeException> wrapEx) {
        return in -> {
            try {
                return f.apply(in);
            } catch (Exception e) {
                throw wrapEx.apply(e);
            }
        };
    }

    public interface CheckedRunnable {
        void run() throws Exception;
    }

    public interface CheckedFunction<I,O> {
        O apply(I in) throws Exception;
    }

}
