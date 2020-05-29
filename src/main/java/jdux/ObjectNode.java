package jdux;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toMap;
import static jdux.Shorthands.unchecked;
import static jdux.Streams.merge;

public interface ObjectNode extends JsonNode {

    record ChildNodeMap(Map<String, JsonNode> childMap) {
        ChildNodeMap(Stream<? extends LabelledNode> children) {
            this(children.collect(toMap(LabelledNode::label, LabelledNode::unlabelled)));
        }
        int numberOfMatchingFields(Constructor<?> ctor) {
            return (int) stream(ctor.getParameters())
                .map(Parameter::getName)
                .filter(childMap::containsKey)
                .count();
        }
        Object callConstructor(Constructor<?> ctor) {
            try {
                Object[] args = stream(ctor.getParameters())
                    .map(param -> childMap.getOrDefault(param.getName(), new NullNode()).asA(param.getParameterizedType()))
                    .toArray();
                return ctor.newInstance(args);
            } catch (ReflectiveOperationException e) {
                throw new JsonReflectException(e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    default <E> E asA(Class<E> type) {
        if (!type.isRecord())
            throw new NonRecordTypeException(type);
        ChildNodeMap childNodeMap = new ChildNodeMap(children());
        return (E) stream(type.getConstructors())
            .max(comparingInt(childNodeMap::numberOfMatchingFields))
            .map(childNodeMap::callConstructor)
            .orElseThrow(NoSuitableConstructorException::new);
    }

    @Override
    default Object asA(Type type) {
        if (type instanceof Class<?> c)
            return asA(c);
        throw new JsonReflectException("Only classes supported for now"); // TODO
    }

    @Override
    default Iterator<? extends LabelledNode> childrenIter() {
        return children().iterator();
    }

    @Override
    Stream<? extends LabelledNode> children();

    /**
     * Combine the given object into this one, overriding common properties.
     */
    default ObjectNode put(ObjectNode other) {
        return () -> merge(children(), other.children(), LabelledNode::label);
    }

    default ObjectNode put(String label, JsonNode node) {
        var labelledNode = new LabelledNodeDecorator(label, node);
        return () -> merge(children(), Stream.of(labelledNode), LabelledNode::label);
    }

    @Override
    default boolean isLeaf() {
        return false;
    }

    default JsonNode get(String key) {
        return children()
            .filter(n -> n.label().equals(key))
            .map(LabelledNode::unlabelled)
            .findFirst().orElseGet(NullNode::new);
    }

    class NonRecordTypeException extends JsonReflectException {
        public NonRecordTypeException(Class<?> type) {
            super("Expected record type but was " + type.getSimpleName());
        }
    }

}
