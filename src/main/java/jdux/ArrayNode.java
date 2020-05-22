package jdux;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.stream.Collector;

import static java.util.stream.Collectors.toUnmodifiableList;
import static java.util.stream.Collectors.toUnmodifiableSet;

public interface ArrayNode extends JsonNode {

    JsonNode get(int index);

    @SuppressWarnings("unchecked")
    @Override
    default <E> E asA(Class<E> type) {
        if (type.isArray()) {
            return (E) children()
                .map(n -> n.asA(type.getComponentType()))
                .toArray(len -> (Object[]) Array.newInstance(type.getComponentType(), len));
        }
        throw new JsonReflectException("Expected array or collection for ArrayNode conversion but was " + type.getName());
    }

    @Override
    default Object asA(Type type) {
        if (type instanceof Class<?> c)
            return asA(c);
        if (type instanceof ParameterizedType pt && pt.getRawType() instanceof Class<?> c && Collection.class.isAssignableFrom(c)) {
            Type typeBound = pt.getActualTypeArguments()[0];
            Collector<Object, ?, ? extends Collection<Object>> collector = switch (pt.getRawType().getTypeName()) {
                case "java.util.List", "java.util.Collection" -> toUnmodifiableList();
                case "java.util.Set" -> toUnmodifiableSet();
                default -> throw new JsonReflectException("Unrecognized collection type " + pt.getTypeName());
            };
            return children()
                .map(n -> n.asA(typeBound))
                .collect(collector);
        }
        throw new JsonReflectException("Expected array or collection for ArrayNode conversion but was " + type.getTypeName());
    }

    @Override
    default boolean isLeaf() {
        return false;
    }

}
