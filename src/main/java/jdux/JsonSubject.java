package jdux;

public interface JsonSubject extends Subject<JsonNode> {

    default <E> Subject<E> map(Class<E> type) {
        return map(n -> n.asA(type), JDux::node);
    }

}
