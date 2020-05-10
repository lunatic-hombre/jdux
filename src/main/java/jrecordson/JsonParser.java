package jrecordson;

import java.lang.reflect.Array;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static java.lang.Character.isDigit;
import static java.lang.Character.isLetter;
import static java.util.Comparator.comparingLong;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.*;
import static jrecordson.Iterables.recalling;
import static jrecordson.Shorthands.*;

class JsonParser {

    static final JsonParser DEFAULT = new JsonParser();
    static final JsonNode.NullNode NULL_NODE = new JsonNode.NullNode();

    private static final int
        MAX_INT_DIGITS = String.valueOf(Integer.MAX_VALUE).length(),
        MAX_LONG_DIGITS = String.valueOf(Long.MAX_VALUE).length();

    JsonNode parseNode(TextInput text) {
        try {
            if (!text.hasNext())
                return new JsonNode.NullNode();
            final int next = text.skipWhitespace().peek();
            return switch (next) {
                case '"', '\'' -> stringNode(text, (char) next);
                case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-' -> numberNode(text);
                case 'f', 't' -> booleanNode(text.read(next == 'f' ? 5 : 4));
                case 'n' -> new JsonNode.NullNode();
                case '[' -> new LazyLoadArrayNode(text);
                case '{' -> new LazyLoadObjectNode(text);
                default -> throw new JsonParseException("Expected json node/value, but was character " + ((char) next), text);
            };
        } catch (JsonParseException e) {
            throw e.setText(text);
        } catch (RuntimeException e) {
            throw new JsonParseException(e, text);
        }
    }

    private JsonNode stringNode(TextInput text, char quote) {
        text.skip(quote);
        final StringBuilder sb = new StringBuilder();
        while (text.hasNext() && text.peek() != quote) {
            int c = text.read();
            if (c == '\\' && text.hasNext()) {
                c = text.read();
                switch (c) {
                    case '"', '\'', '\\', '/' -> sb.append((char) c);
                    case 'b' -> sb.deleteCharAt(sb.length() - 1);
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> sb.appendCodePoint(Integer.parseInt(text.read(4), 16));
                    default -> sb.append('\\').append((char) c);
                }
            } else {
                sb.append((char) c);
            }
        }
        text.skip(quote);
        return new JsonNode.StringNode(sb.toString());
    }

    private JsonNode numberNode(TextInput text) {
        final String stringValue = text.readWhile(this::isNumeric);
        final Number number;
        if (stringValue.matches("-?\\d+")) {
            if (stringValue.length() < MAX_INT_DIGITS)
                number = Integer.parseInt(stringValue);
            else if (stringValue.length() < MAX_LONG_DIGITS)
                number = Long.parseLong(stringValue);
            else
                number = new BigInteger(stringValue);
        } else if (stringValue.matches("-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?")) {
            if (stringValue.length() < MAX_LONG_DIGITS)
                number = Double.parseDouble(stringValue);
            else
                number = new BigDecimal(stringValue);
        } else {
            throw new JsonParseException("Expected number value but was \"" + stringValue + "\".");
        }
        return new JsonNode.ValueNode<>(number);
    }

    boolean isNumeric(int ch) {
        return isDigit(ch) || ch == 'e' || ch == 'E' || ch == '.' || ch == '-' || ch == '+';
    }

    private JsonNode booleanNode(String stringValue) {
        if (stringValue.equals("true"))
            return new JsonNode.ValueNode<>(true);
        else if (stringValue.equals("false"))
            return new JsonNode.ValueNode<>(false);
        throw new JsonParseException("Expected boolean value but was \"" + stringValue + "\".");
    }

    private abstract static class LazyLoadNode<N extends JsonNode> implements JsonNode, LazyLoading {
        LazyLoadingIterable<N> children;

        LazyLoadNode(JsonChildNodeTextIterator<N> unread) {
            this.children = recalling(unread);
        }

        @Override
        public Stream<N> stream() {
            return Streams.toStream(children);
        }

        @Override
        public void load() {
            children.load();
        }
    }

    private class LazyLoadArrayNode extends LazyLoadNode<JsonNode> {
        public LazyLoadArrayNode(TextInput text) {
            super(new ArrayNodeTextIterator(text));
        }

        @SuppressWarnings("unchecked")
        @Override
        public <E> E asA(Class<E> type) {
            if (type.isArray()) {
                return (E) stream()
                    .map(n -> n.asA(type.getComponentType()))
                    .toArray(len -> (Object[]) Array.newInstance(type.getComponentType(), len));
            }
            throw new JsonReflectException("Expected array or collection for ArrayNode conversion but was " + type.getName());
        }

        @Override
        public Object asA(Type type) {
            if (type instanceof Class<?> c)
                return asA(c);
            if (type instanceof ParameterizedType pt && pt.getRawType() instanceof Class<?> c && Collection.class.isAssignableFrom(c)) {
                Type typeBound = pt.getActualTypeArguments()[0];
                Collector<Object, ?, ? extends Collection<Object>> collector = switch (pt.getRawType().getTypeName()) {
                    case "java.util.List", "java.util.Collection" -> toUnmodifiableList();
                    case "java.util.Set" -> toUnmodifiableSet();
                    default -> throw new JsonReflectException("Unrecognized collection type " + pt.getTypeName());
                };
                return stream()
                    .map(n -> n.asA(typeBound))
                    .collect(collector);
            }
            throw new JsonReflectException("Expected array or collection for ArrayNode conversion but was " + type.getTypeName());
        }

        @Override
        public String toString() {
            return '[' + stream().map(Object::toString).collect(joining(",")) + ']';
        }
    }

    private class LazyLoadObjectNode extends LazyLoadNode<JsonNode.LabelledNode> implements JsonNode.ObjectNode {
        public LazyLoadObjectNode(TextInput text) {
            super(new ObjectNodeTextIterator(text));
        }

        @SuppressWarnings("unchecked")
        @Override
        public <E> E asA(Class<E> recordType) {
            if (!recordType.isRecord())
                throw new JsonReflectException("Expected record type but was " + recordType.getName());
            Map<String, JsonNode> nodeMap = stream().collect(toMap(LabelledNode::label, identity()));
            return (E) Arrays.stream(recordType.getConstructors())
                .sorted(comparingLong(ctor -> Arrays.stream(ctor.getParameters()).map(Parameter::getName).filter(nodeMap::containsKey).count()))
                .map(unchecked(ctor -> ctor.newInstance(Arrays.stream(ctor.getParameters()).map(param -> nodeMap.getOrDefault(param.getName(), NULL_NODE).asA(param.getParameterizedType())).toArray()), JsonReflectException::new))
                .findFirst().orElseThrow(() -> new JsonReflectException("No suitable constructor for object"));
        }

        @Override
        public Object asA(Type type) {
            if (type instanceof Class<?> c)
                return asA(c);
            throw new JsonReflectException("Only classes supported for now"); // TODO
        }

        @Override
        public String toString() {
            return "{" + stream().map(Object::toString).collect(joining(",")) + "}";
        }
    }

    /**
     * Lazily loads children from text input.
     *
     * Extended for use in object node and array node.
     */
    private static abstract class JsonChildNodeTextIterator<N extends JsonNode> implements Iterator<N> {

        protected final TextInput text;
        private final char startChar, endChar;
        private boolean done = false;
        private N previous = null;

        JsonChildNodeTextIterator(TextInput text, char startChar, char endChar) {
            this.text = text;
            this.startChar = startChar;
            this.endChar = endChar;
        }

        @Override
        public boolean hasNext() {
            if (done || !text.hasNext())
                return false;
            advanceFromPrevious();
            return !((done = text.skipWhitespace().peek() == endChar) && text.skip(endChar) != null);
        }

        void advanceFromPrevious() {
            if (previous == null) {
                if (text.peek() == startChar)
                    text.skip();
            } else if (previous instanceof LazyLoading lazyLoading) {
                lazyLoading.load();
            }
        }

        void skipComma() {
            final int peek = text.peek();
            if (peek == ',')
                text.skip();
        }

        N setPrevious(N node) {
            return this.previous = node;
        }

    }

    private class ObjectNodeTextIterator extends JsonChildNodeTextIterator<JsonNode.LabelledNode> {

        ObjectNodeTextIterator(TextInput text) {
            super(text, '{', '}');
        }

        @Override
        public JsonNode.LabelledNode next() {
            skipComma();
            final int peek = text.skipWhitespace().peek();
            final String nodeName;
            if (peek == '\'' || peek == '"')
                nodeName = then(text.skip().readUntil((char) peek), text::skip);
            else if (isLetter(peek))
                nodeName = text.readWhile(Character::isLetterOrDigit);
            else
                throw new JsonParseException("Expected letter or quote but was " + ((char) peek), text);
            text.skipWhitespace().skipIgnoreCase(':').skipWhitespace();
            JsonNode base = parseNode(text);
            return setPrevious(new LabelledNodeDecorator(nodeName, base));
        }
    }

    private class ArrayNodeTextIterator extends JsonChildNodeTextIterator<JsonNode> {

        ArrayNodeTextIterator(TextInput text) {
            super(text, '[', ']');
        }

        @Override
        public JsonNode next() {
            skipComma();
            text.skipWhitespace();
            return setPrevious(parseNode(text));
        }

    }

    private static class LabelledNodeDecorator implements JsonNode.LabelledNode, LazyLoading {

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
        public <E> E asA(Class<E> recordType) {
            return base.asA(recordType);
        }

        @Override
        public Object asA(Type type) {
            return base.asA(type);
        }

        @Override
        public Stream<? extends JsonNode> stream() {
            return base.stream();
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
}
