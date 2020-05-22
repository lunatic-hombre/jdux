package jdux;

import java.util.function.Predicate;

public interface JsonSelectorSegment extends Predicate<JsonNode> {

    JsonSelectType type();

    String key();

    default boolean test(JsonNode node) {
        return node instanceof JsonNode.LabelledNode ln && ln.label().equals(key());
    }

}
