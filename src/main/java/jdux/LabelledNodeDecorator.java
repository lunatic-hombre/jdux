package jdux;

import java.lang.reflect.Type;
import java.util.stream.Stream;

class LabelledNodeDecorator implements JsonNode.LabelledNode, LazyLoading {

    private final String nodeName;
    private final JsonNode base;

    public LabelledNodeDecorator(String nodeName, JsonNode base) {
        this.nodeName = nodeName;
        this.base = base;
    }

    @Override
    public String label() {
        return nodeName;
    }

    @Override
    public JsonNode unlabelled() {
        return base;
    }

    @Override
    public <E> E asA(Class<E> recordType) {
        return base.asA(recordType);
    }

    @Override
    public Object asA(Type type) {
        return base.asA(type);
    }

    @Override
    public Stream<? extends JsonNode> children() {
        return base.children();
    }

    @Override
    public String toString() {
        return "\"" + nodeName + "\":" + base.toString();
    }

    @Override
    public void load() {
        if (base instanceof LazyLoading lazyLoading)
            lazyLoading.load();
    }
}
