package jdux;

import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

final class Shorthands {

    private Shorthands() {
        throw new AssertionError("This is a static utility class.");
    }

    public static <T> UnaryOperator<T> then(UnaryOperator<T> op, Consumer<T> after) {
        return t -> {
            var r = op.apply(t);
            after.accept(r);
            return r;
        };
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

    public static <T> Supplier<T> unchecked(Callable<T> callable,
                                            Function<Throwable, ? extends RuntimeException> wrapEx) {
        return () -> {
            try {
                return callable.call();
            } catch (Exception e) {
                throw wrapEx.apply(e);
            }
        };
    }

    public static <T> Consumer<T> uncheckedConsumer(CheckedConsumer<T> consumer,
                                                    Function<Throwable, ? extends RuntimeException> wrapEx) {
        return t -> {
            try {
                consumer.accept(t);
            } catch (Exception e) {
                throw wrapEx.apply(e);
            }
        };
    }

    public static Runnable unchecked(CheckedRunnable runnable,
                                     Function<Throwable, ? extends RuntimeException> wrapEx) {
        return () -> {
            try {
                runnable.run();
            } catch (Exception e) {
                throw wrapEx.apply(e);
            }
        };
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

    public interface CheckedConsumer<T> {
        void accept(T t) throws Exception;
    }

    public interface CheckedRunnable {
        void run() throws Exception;
    }

    public interface CheckedFunction<I,O> {
        O apply(I in) throws Exception;
    }

}
