package jdux;

import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

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
     * Query the JSON tree for the given path.
     * @param path in form of a.b..c where {a,b,c} are field names in the JSON tree
     * @return     A stream of matching nodes.
     */
    Stream<JsonNode> select(String path);

    /**
     * Chain method to isolate a subset of nodes.
     * @param path in form of a.b..c where {a,b,c} are field names in the JSON tree
     * @return     A subject for updates / subscriptions on a subset of nodes.
     */
    JsonSubject subject(String path);

    /**
     * Get the root of the JSON tree.
     * @return the root of the JSON tree.
     */
    JsonNode root();

    /**
     * Set the root of the JSON tree.
     */
    JsonDB root(JsonNode newRoot);

}
