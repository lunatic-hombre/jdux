JDux
====

Easy JSON state management for modern Java.

Features include:
- JSON parsing.
- JSON reflection.
- JSON writing.
- JSON state management.

## Usage

Here's an example test that parses and maps some JSON to a record.

```java
import jdux.JDux;

import org.junit.Test;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

public class JsonNodeTest {
    
    record UserRecord(int id, String name, List<RoleRecord> roles) {}
    record RoleRecord(int id, String name) {}
    
    private final String inputJson = """
        {
          "id": 123,
          "name":"Bob Loblaw"
          "roles": [
            {
              "id": 1,
              "name": "Manager"
            }
          ]
        }
    """;

    private final UserRecord userRecord = new UserRecord(123, "Bob Loblaw", singletonList(new RoleRecord(1, "Manager")));

    @Test
    public void parseAndMapToRecord() {
        var node = JDux.parse(inputJson);
        var user = node.asA(UserRecord.class);
        assertEquals(userRecord, user);
    }

}
```
