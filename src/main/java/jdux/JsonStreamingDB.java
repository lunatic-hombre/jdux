package jdux;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static jdux.Iterables.filter;
import static jdux.Iterables.filterMap;
import static jdux.JsonPickType.DESCENDANT;
import static jdux.Shorthands.then;

class JsonStreamingDB implements JsonDB {

    record JsonUpdateSubscriber(JsonSelector selection, Consumer<JsonNode> onUpdate) {
        boolean isDescendant() {
            return selection.type() == DESCENDANT;
        }
        boolean keyMatches(String key) {
            return selection.key().equalsIgnoreCase(key);
        }
        boolean accepts(String key) {
            return isDescendant() || keyMatches(key);
        }
        JsonUpdateSubscriber next(String key) {
            return keyMatches(key)
                ? new JsonUpdateSubscriber(selection.next(), onUpdate)
                : this;
        }
    }

    public static record WriterReference(String ref, Writer out) {}

    private final Supplier<TextInput> source;
    private final Supplier<WriterReference> sink;
    private final Consumer<String> swap;
    private final JsonParser parser;
    private final JsonWriter writer;
    private final Collection<JsonUpdateSubscriber> allSubscribers;

    JsonStreamingDB(Supplier<TextInput> source,
                    Supplier<WriterReference> sink,
                    Consumer<String> swap,
                    JsonParser parser,
                    JsonWriter writer) {
        this.source = source;
        this.sink = sink;
        this.swap = swap;
        this.parser = parser;
        this.writer = writer;
        this.allSubscribers = new ArrayList<>();
    }

    @Override
    public void update(String query, UnaryOperator<JsonNode> update) {
        var path = JsonPath.parse(query);
        var superSetSubscribers = allSubscribers.stream().filter(s -> s.selection.contains(path)).collect(toList());
        if (!superSetSubscribers.isEmpty())
            update = then(update, result -> superSetSubscribers.forEach(s -> s.onUpdate.accept(result)));
        var subsetSubscribers = filter(allSubscribers, s -> path.contains(s.selection) && !superSetSubscribers.contains(s));

        var updatedNode = updateNode(root(), path, subsetSubscribers, update);

        // write to output stream and swap in result for next read
        WriterReference output = sink.get();
        try (var out = output.out()) {
            writer.write(updatedNode, out);
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
        swap.accept(output.ref());
    }

    @Override
    public JsonNode root() {
        return parser.parse(source.get());
    }

    @Override
    public void subscribe(String query, Consumer<JsonNode> consumer) {
        allSubscribers.add(new JsonUpdateSubscriber(JsonPath.parse(query), consumer));
    }

    JsonNode updateNode(JsonNode node,
                        JsonSelector pick,
                        Iterable<JsonUpdateSubscriber> subscribers,
                        UnaryOperator<JsonNode> update) {
        if (node instanceof JsonNode.LabelledNode ln)
            return new LabelledNodeDecorator(ln.label(), updateNode(ln.unlabelled(), pick, subscribers, update));
        else if (node instanceof ObjectNode on)
            return new JsonUpdateObjectNode(on, pick, subscribers, update);
        else if (node instanceof ArrayNode an)
            return new JsonUpdateArrayNode(an, pick, subscribers, update);
        return new JsonUpdateNode<>(node, pick, subscribers, update);
    }

    private static Iterable<JsonUpdateSubscriber> nextGen(Iterable<JsonUpdateSubscriber> subscribers,
                                                          String key) {
        return filterMap(subscribers, s -> s.accepts(key), s -> s.next(key));
    }

    private static UnaryOperator<JsonNode> applyLabel(UnaryOperator<JsonNode> base) {
        return n -> {
            var result = base.apply(n);
            if (n instanceof JsonNode.LabelledNode ln && !(result instanceof JsonNode.LabelledNode))
                result = new LabelledNodeDecorator(ln.label(), result);
            return result;
        };
    }

    class JsonUpdateArrayNode extends JsonUpdateNode<ArrayNode> implements ArrayNode {
        public JsonUpdateArrayNode(ArrayNode node, JsonSelector pick, Iterable<JsonUpdateSubscriber> subscribers, UnaryOperator<JsonNode> update) {
            super(node, pick, subscribers, update);
        }
        @Override
        public JsonNode get(int index) {
            return node.get(index);
        }
    }

    class JsonUpdateObjectNode extends JsonUpdateNode<ObjectNode> implements ObjectNode {
        public JsonUpdateObjectNode(ObjectNode node, JsonSelector pick, Iterable<JsonUpdateSubscriber> subscribers, UnaryOperator<JsonNode> update) {
            super(node, pick, subscribers, applyLabel(update));
        }
        @Override
        public Iterator<? extends LabelledNode> childrenIter() {
            return children().iterator();
        }
        @Override
        public Stream<? extends LabelledNode> children() {
            return node.children().map(this::map);
        }
        protected LabelledNode map(LabelledNode ln) {
            String key = ln.label();
            if (key.equals(select.key()))
                return (LabelledNode) (select.hasNext()
                                    ? updateNode(ln, select.next(), nextGen(subscribers, key), update)
                                    : doUpdate(ln));
            else if (select.type() == DESCENDANT)
                return (LabelledNode) updateNode(ln, select, nextGen(subscribers, key), update);
            else
                return ln;
        }
        @Override
        public ObjectNode put(ObjectNode other) {
            return node.put(other);
        }
        @Override
        public ObjectNode put(String label, JsonNode other) {
            return node.put(label, other);
        }
        @Override
        public JsonNode get(String key) {
            return node.get(key);
        }
    }

    class JsonUpdateNode<N extends JsonNode> implements JsonNode {

        final N node;
        final JsonSelector select;
        final Iterable<JsonUpdateSubscriber> subscribers;
        final UnaryOperator<JsonNode> update;

        public JsonUpdateNode(N node,
                              JsonSelector select,
                              Iterable<JsonUpdateSubscriber> subscribers,
                              UnaryOperator<JsonNode> update) {
            this.node = node;
            this.select = select;
            this.subscribers = subscribers;
            this.update = update;
        }

        @Override
        public <E> E asA(Class<E> type) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object asA(Type type) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Stream<? extends JsonNode> children() {
            return node.children().map(this::map);
        }

        protected JsonNode map(JsonNode n) {
            return select.type() == DESCENDANT
                ? updateNode(n, select, subscribers, update)
                : n;
        }

        @Override
        public String toString() {
            return node.jsonString();
        }

        protected JsonNode doUpdate(JsonNode n) {
            var result = update.apply(n);
            subscribers.forEach(s -> s.onUpdate.accept(result));
            return result;
        }

    }

}
