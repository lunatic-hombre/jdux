package jdux;

import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    public static void write(Object obj, Appendable out) {
        DEFAULT_WRITER.write(node(obj), out);
    }

    public static JsonNode parse(String str) {
        return DEFAULT_PARSER.parse(TextInput.wrap(str));
    }

    public static JsonNode parse(Reader reader) {
        return DEFAULT_PARSER.parse(TextInput.wrap(reader));
    }

    public static JsonDB fileDB(Path path) {
        return new JsonStreamingDB(
            unchecked(() -> TextInput.wrap(Files.newBufferedReader(path)), IORuntimeException::new),
            unchecked(() -> {
                Path tempFile = Files.createTempFile(nextString(), "json");
                String ref = tempFile.toString();
                return new JsonStreamingDB.WriterReference(ref, Files.newBufferedWriter(tempFile));
            }, IORuntimeException::new),
            ref -> {
                try {
                    Files.move(Paths.get(ref), path, REPLACE_EXISTING);
                } catch (Exception e) {
                    throw new IORuntimeException(e);
                }
            },
            new JsonParser(false),
            new JsonWriter()
        );
    }

    public static void setPretty() {
        DEFAULT_WRITER.setPretty(true);
    }

    private static String nextString() {
        byte[] intBytes = ByteBuffer.allocate(4).putInt(RANDOM.nextInt()).array();
        return Base64.getEncoder().encodeToString(intBytes).replaceAll("\\W", "0");
    }

}
