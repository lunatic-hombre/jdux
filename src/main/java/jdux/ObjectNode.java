package jdux;

import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Comparator.comparingLong;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static jdux.Shorthands.unchecked;
import static jdux.Streams.merge;

public interface ObjectNode extends JsonNode {

    @SuppressWarnings("unchecked")
    @Override
    default <E> E asA(Class<E> recordType) {
        if (!recordType.isRecord())
            throw new JsonReflectException("Expected record type but was " + recordType.getName());
        Map<String, JsonNode> nodeMap = children().collect(toMap(LabelledNode::label, identity()));
        return (E) Arrays.stream(recordType.getConstructors())
            .sorted(comparingLong(ctor -> Arrays.stream(ctor.getParameters()).map(Parameter::getName).filter(nodeMap::containsKey).count()))
            .map(unchecked(ctor -> ctor.newInstance(Arrays.stream(ctor.getParameters()).map(param -> nodeMap.getOrDefault(param.getName(), new NullNode()).asA(param.getParameterizedType())).toArray()), JsonReflectException::new))
            .findFirst().orElseThrow(() -> new JsonReflectException("No suitable constructor for object"));
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

    default JsonNode get(String key) {
        return children()
            .filter(n -> n.label().equals(key))
            .map(LabelledNode::unlabelled)
            .findFirst().orElseGet(NullNode::new);
    }

}
