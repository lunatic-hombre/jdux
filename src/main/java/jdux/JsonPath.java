package jdux;

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static jdux.JsonSelectType.CHILD;

public class JsonPath {

    private static final Pattern PATH_ELEM = Pattern.compile("(?<type>^|\\.\\.?)(?<key>\\w+)");

    static JsonSelector parse(String str) {
        if (str == null || str.isBlank())
            return null; // TODO?
        Matcher matcher = PATH_ELEM.matcher(str);
        if (!matcher.find())
            throw new JsonPathParseException("Invalid path " + str);
        JsonPathElem head = readNextSegment(0, matcher), current = head;
        int index = matcher.end();
        while (matcher.find(index)) {
            current = current.next = readNextSegment(index, matcher);
            index = matcher.end();
        }
        return head;
    }

    private static JsonPathElem readNextSegment(int index, Matcher matcher) {
        if (matcher.start() != index)
            throw new JsonPathParseException("Unexpected characters");
        JsonSelectType type = matcher.group("type").length() > 1
            ? JsonSelectType.DESCENDANT
            : CHILD;
        return new JsonPathElem(type, matcher.group("key"));
    }

    private static class JsonPathElem implements JsonSelector, JsonSelectorSegment {

        final JsonSelectType type;
        final String key;
        JsonPathElem next;

        JsonPathElem(JsonSelectType type, String key) {
            this.type = type;
            this.key = key;
        }

        @Override
        public JsonSelectorSegment root() {
            return this;
        }

        @Override
        public JsonSelector next() {
            return next;
        }

        @Override
        public JsonSelectType type() {
            return type;
        }

        @Override
        public String key() {
            return key;
        }

        @Override
        public Iterator<JsonSelectorSegment> iterator() {
            return new Iterator<>() {
                JsonPathElem elem = JsonPathElem.this;
                @Override
                public boolean hasNext() {
                    return elem != null;
                }
                @Override
                public JsonSelectorSegment next() {
                    try {
                        return elem;
                    } finally {
                        elem = elem.next;
                    }
                }
            };
        }

        @Override
        public boolean contains(JsonSelector other) {
            return other.toString("", "!")
                .matches(toString("", ".*"));
        }

        @Override
        public String toString(String child, String ancestor) {
            StringBuilder sb = new StringBuilder();
            sb.append(type == CHILD ? key : ancestor + key);
            for (JsonPathElem e = next; e != null; e = e.next)
                sb.append(e.type == CHILD ? child : ancestor).append(e.key);
            return sb.toString();
        }

        @Override
        public String toString() {
            return toString(".", "..");
        }

    }

}
