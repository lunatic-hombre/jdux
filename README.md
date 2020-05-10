JRecordSON
===========

This project houses a basic JSON parser / formatter for use with 
the new Java `record` types.

Features include:
- Parsing JSON lazily for more efficient streaming
- Reflection to record types and collections

## Usage

Here's an example test that parses and maps some JSON to a record.

```java
import jrecordson.JsonNode;

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
        var node = JsonNode.parse(inputJson);
        var user = node.asA(UserRecord.class);
        assertEquals(userRecord, user);
    }

}
```
