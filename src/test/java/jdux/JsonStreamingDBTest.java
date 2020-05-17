package jdux;

import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class JsonStreamingDBTest {

    private static final String JSON = """
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

    private final JsonWriter pretty = new JsonWriter().setPretty(true);

    private JsonDB db;
    private List<JsonNode> updates;

    @Before
    public void setUp() throws Exception {
        Path temp = Files.createTempFile("test", "json");
        JsonNode node = JDux.parse(JSON);
        try (var out = Files.newBufferedWriter(temp)) {
            pretty.write(node, out);
        }
        db = JDux.file(temp);
        updates = new ArrayList<>();
    }

    @Test
    public void changeUser() {
        TestUserRecord bob = new TestUserRecord("Bob", 43);
        db.subscribe("user", updates::add);
        db.update("user", bob);
        String expected = """
            {
              "user": {
                "name": "Bob",
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
        assertEquals(expected, pretty.toString(db.root()));
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
        assertEquals(expected, pretty.toString(db.root()));
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
        assertEquals(expected, pretty.toString(db.root()));
        assertEquals(1, updates.size());
    }

    record TestUserRecord(String name, int age) {}

}
