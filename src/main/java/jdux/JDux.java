package jdux;

import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Random;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static jdux.Shorthands.unchecked;

public final class JDux {

    private static final Random RANDOM = new Random();
    private static final NodeReflection REFLECT = new NodeReflection();
    private static final JsonParser DEFAULT_PARSER = new JsonParser();
    private static final JsonWriter DEFAULT_WRITER = new JsonWriter();

    private JDux() {}

    public static JsonNode node(Object object) {
        return REFLECT.toNode(object);
    }

    public static void write(JsonNode node, Appendable out) {
        DEFAULT_WRITER.write(node, out);
    }

    public static JsonNode parse(String str) {
        return DEFAULT_PARSER.parse(TextInput.wrap(str));
    }

    public static JsonNode parse(Reader reader) {
        return DEFAULT_PARSER.parse(TextInput.wrap(reader));
    }

    public static JsonDB file(Path path) {
        return new JsonStreamingDB(
            unchecked(() -> TextInput.wrap(Files.newBufferedReader(path)), IORuntimeException::new),
            unchecked(() -> {
                String ref = nextString();
                return new JsonStreamingDB.WriterReference(ref, Files.newBufferedWriter(temp(path, ref)));
            }, IORuntimeException::new),
            ref -> {
                try {
                    Files.move(temp(path, ref), path, REPLACE_EXISTING);
                } catch (Exception e) {
                    throw new IORuntimeException(e);
                }
            },
            new JsonParser(),
            new JsonWriter()
        );
    }

    public static void setPretty() {
        DEFAULT_WRITER.setPretty(true);
    }

    private static String nextString() {
        return Base64.getEncoder().encodeToString(ByteBuffer.allocate(4).putInt(RANDOM.nextInt()).array());
    }

    private static Path temp(Path path, String ref) {
        return path.resolveSibling(ref + "_" + path.getFileName());
    }

}
