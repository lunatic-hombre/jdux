package jdux;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public abstract class AbstractStreamingJsonDBTest {

    static final String SAMPLE_JSON = """
        {
          "user": {
            "name": "Steve",
            "age": 34
          },
          "friends": [
            {
              "name": "Joe",
              "age": 41
            },
            {
              "name": "Eddie",
              "age": 36
            }
          ]
        }""";

    private final JsonWriter writer = new JsonWriter().setPretty(true);

    protected JsonDB db;
    protected List<JsonNode> updates;

    @Before
    public void setUp() {
        db = getDB().root(JDux.parse(SAMPLE_JSON));
        updates = new ArrayList<>();
    }

    protected abstract JsonDB getDB();

    @Test
    public void changeUser() {
        TestUserRecord bob = new TestUserRecord("Bob Loblaw", 43);
        db.subscribe("user", updates::add);
        db.update("user", bob);
        String expected = """
            {
              "user": {
                "name": "Bob Loblaw",
                "age": 43
              },
              "friends": [
                {
                  "name": "Joe",
                  "age": 41
                },
                {
                  "name": "Eddie",
                  "age": 36
                }
              ]
            }""";
        assertEquals(expected, writer.toString(db.root()));
        assertEquals(1, updates.size());
        assertEquals(bob, updates.get(0).asA(TestUserRecord.class));
    }

    @Test
    public void changeAllNames() {
        db.subscribe("user.name", updates::add);
        db.subscribe("friends..name", updates::add);
        db.update("..name", "Spartacus");
        String expected = """
            {
              "user": {
                "name": "Spartacus",
                "age": 34
              },
              "friends": [
                {
                  "name": "Spartacus",
                  "age": 41
                },
                {
                  "name": "Spartacus",
                  "age": 36
                }
              ]
            }""";
        assertEquals(expected, writer.toString(db.root()));
        assertEquals(3, updates.size());
    }

    @Test
    public void changeSpecificName() {
        db.subscribe("..name", updates::add);
        db.update("user.name", "Spartacus");
        String expected = """
            {
              "user": {
                "name": "Spartacus",
                "age": 34
              },
              "friends": [
                {
                  "name": "Joe",
                  "age": 41
                },
                {
                  "name": "Eddie",
                  "age": 36
                }
              ]
            }""";
        assertEquals(expected, writer.toString(db.root()));
        assertEquals(1, updates.size());
    }

    @Test
    public void select() {
        assertEquals("\"Steve\",\"Joe\",\"Eddie\"",
            db.select("..name").map(JsonNode::jsonString)
                .collect(Collectors.joining(",")));
    }

    record TestUserRecord(String name, int age) {}

}
