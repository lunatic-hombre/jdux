package jdux;

import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public interface JsonSubject {

    void update(UnaryOperator<JsonNode> update);

    void subscribe(Consumer<JsonNode> consumer);

}
