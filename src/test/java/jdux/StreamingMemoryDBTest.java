package jdux;

public class StreamingMemoryDBTest extends AbstractStreamingJsonDBTest {

    @Override
    protected JsonDB getDB() {
        return JDux.memDB(1024 * 1024);
    }

}
