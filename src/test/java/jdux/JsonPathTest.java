package jdux;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JsonPathTest {

    @Test
    public void parseOne() {
        JsonSelector selector = JsonPath.parse("foo");
        assertEquals(1, selector.length());
        String key = "foo";
        JsonPickType type = JsonPickType.CHILD;
        assertElemMatches(selector, type, key);
    }

    @Test
    public void parseChain() {
        JsonSelector selector = JsonPath.parse("foo.bar..doo.gar");
        assertEquals(4, selector.length());
        assertElemMatches(selector, JsonPickType.CHILD, "foo");
        assertElemMatches(selector = selector.next(), JsonPickType.CHILD, "bar");
        assertElemMatches(selector = selector.next(), JsonPickType.DESCENDANT, "doo");
        assertElemMatches(selector.next(), JsonPickType.CHILD, "gar");
    }

    private void assertElemMatches(JsonSelector elem, JsonPickType type, String key) {
        assertEquals(key, elem.key());
        assertEquals(type, elem.type());
    }

}
