package jrecordson;

import java.io.Reader;
import java.lang.reflect.Type;
import java.util.stream.Stream;

public interface JsonNode {

    static JsonNode parse(String str) {
        return JsonParser.DEFAULT.parseNode(TextInput.wrap(str));
    }
    static JsonNode parse(Reader reader) {
        return JsonParser.DEFAULT.parseNode(TextInput.wrap(reader));
    }

    <E> E asA(Class<E> recordType);

    Object asA(Type type);

    Stream<? extends JsonNode> stream();

    default <E> Stream<E> streamAs(Class<E> recordType) {
        return stream().map(n -> n.asA(recordType));
    }

    interface LabelledNode extends JsonNode {
        String label();
    }

    interface ObjectNode extends JsonNode {
        @Override
        Stream<? extends LabelledNode> stream();
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
        public Stream<? extends JsonNode> stream() {
            throw new UnsupportedOperationException("Cannot iterate on value node");
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
        public Stream<? extends JsonNode> stream() {
            throw new UnsupportedOperationException("Cannot iterate on value node");
        }
        @Override
        public String toString() {
            return "null";
        }
    }

}
