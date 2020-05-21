package jdux;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JsonPathTest {

    @Test
    public void parseOne() {
        JsonSelector selector = JsonPath.parse("foo");
        assertEquals(1, selector.length());
        String key = "foo";
        JsonSelectType type = JsonSelectType.CHILD;
        assertElemMatches(selector, type, key);
    }

    @Test
    public void parseChain() {
        JsonSelector selector = JsonPath.parse("foo.bar..doo.gar");
        assertEquals(4, selector.length());
        assertElemMatches(selector, JsonSelectType.CHILD, "foo");
        assertElemMatches(selector = selector.next(), JsonSelectType.CHILD, "bar");
        assertElemMatches(selector = selector.next(), JsonSelectType.DESCENDANT, "doo");
        assertElemMatches(selector.next(), JsonSelectType.CHILD, "gar");
    }

    private void assertElemMatches(JsonSelector elem, JsonSelectType type, String key) {
        assertEquals(key, elem.key());
        assertEquals(type, elem.type());
    }

}
