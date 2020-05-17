package jdux;

import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.stream.Stream;

public interface JsonNode {

    default String jsonString() {
        StringBuilder out = new StringBuilder();
        JDux.write(this, out);
        return out.toString();
    }

    /**
     * Convert to the given type.
     * @param type the desired type; must be a record if object node,
     *             collection / array if array node, or primitive / string if value node
     * @return the converted value
     */
    <E> E asA(Class<E> type);

    /**
     * Looser form of asA(Class) for generic type support.
     */
    Object asA(Type type);

    /**
     * Iterator over this node's children.
     * @return the node's children; empty if non-object / array node
     */
    default Iterator<? extends JsonNode> childrenIter() {
        return children().iterator();
    }

    /**
     * Stream over this node's children.
     * @return the node's children; empty if non-object / array node
     */
    Stream<? extends JsonNode> children();

    /**
     * Convenience method to perform "asA()" operation over this node's children.
     */
    default <E> Stream<E> children(Class<E> recordType) {
        return children().map(n -> n.asA(recordType));
    }

    interface LabelledNode extends JsonNode {
        String label();
        JsonNode unlabelled();
    }

    @SuppressWarnings("unchecked")
    class ValueNode<T> implements JsonNode {
        T value;
        ValueNode(T value) {
            this.value = value;
        }
        @Override
        public <E> E asA(Class<E> recordType) {
            if (recordType.isInstance(value))
                return recordType.cast(value);
            // TODO conversion
            throw new JsonReflectException("Cannot convert " + value + " to " + recordType.getName());
        }
        @Override
        public Object asA(Type type) {
            return value; // TODO
        }

        @Override
        public Stream<? extends JsonNode> children() {
            return Stream.empty();
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }
    @SuppressWarnings("unchecked")
    class StringNode extends ValueNode<String> {
        StringNode(String value) {
            super(value);
        }
        @Override
        public <E> E asA(Class<E> type) {
            if (type != String.class)
                throw new UnsupportedOperationException("Cannot parse string value");
            return (E) value; // TODO
        }
        @Override
        public String toString() {
            return '"' + value + '"';
        }
    }
    class NullNode implements JsonNode {
        @Override
        public <E> E asA(Class<E> recordType) {
            return null;
        }
        @Override
        public Object asA(Type type) {
            return null;
        }
        @Override
        public Stream<? extends JsonNode> children() {
            return Stream.empty();
        }
        @Override
        public String toString() {
            return "null";
        }
    }

}
