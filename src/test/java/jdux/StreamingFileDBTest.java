package jdux;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class StreamingFileDBTest extends AbstractStreamingJsonDBTest {

    @Override
    protected JsonDB getDB() {
        try {
            Path temp = Files.createTempFile("test", "json");
            return JDux.fileDB(temp);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
