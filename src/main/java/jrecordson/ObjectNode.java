package jrecordson;

import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Comparator.comparingLong;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static jrecordson.Shorthands.unchecked;

public interface ObjectNode extends JsonNode {

    @SuppressWarnings("unchecked")
    @Override
    default <E> E asA(Class<E> recordType) {
        if (!recordType.isRecord())
            throw new JsonReflectException("Expected record type but was " + recordType.getName());
        Map<String, JsonNode> nodeMap = stream().collect(toMap(LabelledNode::label, identity()));
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
    Iterator<? extends LabelledNode> iterator();

    @Override
    Stream<? extends LabelledNode> stream();

    default JsonNode get(String key) {
        return stream()
            .filter(n -> n.label().equals(key))
            .map(LabelledNode::unlabelled)
            .findFirst().orElseGet(NullNode::new);
    }

}
