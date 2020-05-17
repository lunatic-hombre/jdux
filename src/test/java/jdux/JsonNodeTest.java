package jdux;

import org.junit.BeforeClass;
import org.junit.Test;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

public class JsonNodeTest {

    private final String inputJson = """
            {
              "id": 123,
              "name": "Bob Loblaw",
              "roles": [
                {
                  "id": 1,
                  "name": "Manager"
                }
              ]
            }""";
    private final UserRecord userRecord = new UserRecord(
        123,
        "Bob Loblaw",
        singletonList(new RoleRecord(1, "Manager"))
    );

    @BeforeClass
    public static void setUp() {
        JDux.setPretty();
    }

    @Test
    public void serializationOfUser() {
        var node = JDux.parse(inputJson);
        var unpretty = "{" +
            "\"id\":123,\"name\":\"Bob Loblaw\"," +
            "\"roles\":[{\"id\":1,\"name\":\"Manager\"}]" +
        "}";
        assertEquals(unpretty, node.toString());
    }

    @Test
    public void parseAndMapToRecord() {
        var node = JDux.parse(inputJson);
        var user = node.asA(UserRecord.class);
        assertEquals(userRecord, user);
    }

    @Test
    public void prettyOutput() {
        var node = JDux.parse(inputJson);
        assertEquals(inputJson, node.jsonString());
    }

    @Test
    public void reflection() {
        var node = JDux.node(userRecord);
        assertEquals(inputJson, node.jsonString());
    }

}
