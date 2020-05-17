package jdux;

import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public interface JsonDB {

    /**
     * Assign the nodes for the given path to the supplied value.
     * @param path  in form of a.b..c where {a,b,c} are field names in the JSON tree
     * @param value any object type that can be translated to a JSON node
     */
    default void update(String path, Object value) {
        update(path, JDux.node(value));
    }

    /**
     * Assign the nodes for the given path.
     * @param path  in form of a.b..c where {a,b,c} are field names in the JSON tree
     * @param node  any JSON node
     */
    default void update(String path, JsonNode node) {
        update(path, n -> node);
    }

    /**
     * Replace nodes using the given function.
     * @param path   in form of a.b..c where {a,b,c} are field names in the JSON tree
     * @param update operator to replace the matched nodes with the return value
     */
    void update(String path, UnaryOperator<JsonNode> update);

    /**
     * When nodes for the given path are modified, the consumer will be called.
     * @param path     in form of a.b..c where {a,b,c} are field names in the JSON tree
     * @param consumer accepts updated nodes
     */
    void subscribe(String path, Consumer<JsonNode> consumer);

    /**
     * Get the root of the JSON tree.
     * @return the root of the JSON tree.
     */
    JsonNode root();

}
