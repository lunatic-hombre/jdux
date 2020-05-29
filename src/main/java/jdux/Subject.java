package jdux;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public interface Subject<E> {

    void update(UnaryOperator<E> update);

    void subscribe(Consumer<E> consumer);

    E get();

    /**
     * Short-hand for immediately getting value to pass to consumer, then subscribing.
     */
    default void sync(Consumer<E> consumer) {
        consumer.accept(get());
        subscribe(consumer);
    }

    default <F> Subject<F> map(Function<E, F> to, Function<F, E> from) {
        Subject<E> base = this;
        return new Subject<>() {
            @Override
            public void update(UnaryOperator<F> update) {
                base.update(e -> from.apply(update.apply(to.apply(e))));
            }
            @Override
            public void subscribe(Consumer<F> consumer) {
                base.subscribe(e -> consumer.accept(to.apply(e)));
            }
            @Override
            public F get() {
                return to.apply(base.get());
            }
        };
    }

}
