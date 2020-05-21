package jdux;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class NodeReflection {

    JsonNode toNode(Object obj) {
        if (obj == null)
            return new JsonNode.NullNode();
        if (obj instanceof String s)
            return new JsonNode.StringNode(s);
        if (obj instanceof TemporalAccessor ta)
            return new JsonNode.StringNode(DateTimeFormatter.ISO_INSTANT.format(ta));
        Class<?> type = obj.getClass();
        if (type.isPrimitive() || Primitives.isWrapperType(type))
            return new JsonNode.ValueNode<>(obj);
        if (type.isArray())
            return new ListNode(Arrays.stream((Object[]) obj)
                .map(this::toNode)
                .collect(toList()));
        if (obj instanceof Collection<?> collection)
            return new ListNode(collection.stream()
                .map(this::toNode)
                .collect(toList()));
        if (type.isRecord())
            return new RecordNode(obj);
        throw new JsonReflectException("Unsupported type " + obj);
    }

    static class ListNode implements ArrayNode {

        private final List<JsonNode> nodes;

        public ListNode(List<JsonNode> nodes) {
            this.nodes = nodes;
        }

        @Override
        public JsonNode get(int index) {
            return nodes.get(index);
        }

        @Override
        public Iterator<? extends JsonNode> childrenIter() {
            return nodes.iterator();
        }

        @Override
        public Stream<? extends JsonNode> children() {
            return nodes.stream();
        }

        @Override
        public String toString() {
            return jsonString();
        }
    }

    private class RecordNode implements ObjectNode {

        private final Object obj;

        public RecordNode(Object obj) {
            this.obj = obj;
        }

        @Override
        public Stream<? extends LabelledNode> children() {
            return Stream.of(obj.getClass().getDeclaredFields())
                .filter(f -> !Modifier.isStatic(f.getModifiers()))
                .map(this::asLabeledNode);
        }

        private LabelledNode asLabeledNode(Field field) {
            try {
                return new LabelledNodeDecorator(
                    field.getName(),
                    toNode(obj.getClass().getMethod(field.getName()).invoke(obj))
                );
            } catch (ReflectiveOperationException e) {
                throw new JsonReflectException(e);
            }
        }

        @Override
        public String toString() {
            return jsonString();
        }

    }
}
