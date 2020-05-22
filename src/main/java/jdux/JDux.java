package jdux;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
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

    /**
     * Use reflection to create a JSON node from any supported object.
     */
    public static JsonNode node(Object object) {
        return REFLECT.toNode(object);
    }

    /**
     * Write a node to given output using the default writer.
     */
    public static void write(JsonNode node, Appendable out) {
        DEFAULT_WRITER.write(node, out);
    }

    /**
     * Write an object as JSON to given output using the default writer.
     */
    public static void write(Object obj, Appendable out) {
        DEFAULT_WRITER.write(node(obj), out);
    }

    /**
     * Parse with default parser.
     */
    public static JsonNode parse(String str) {
        return DEFAULT_PARSER.parse(TextInput.wrap(str));
    }

    /**
     * Parse with default parser.
     */
    public static JsonNode parse(Reader reader) {
        return DEFAULT_PARSER.parse(TextInput.wrap(reader));
    }

    /**
     * Create in-memory JSON database.
     * @param size number of bytes to allocate
     */
    public static JsonDB memDB(int size) {
        ReadWriteBuffers buffers = new ReadWriteBuffers(size);
        return new StreamingJsonDB(
            buffers::textInput,
            buffers::writer,
            buffers::swap,
            new JsonParser(),
            new JsonWriter()
        );
    }

    /**
     * Create a file JSON database.
     * @param path path to your chosen JSON file
     */
    public static JsonDB fileDB(Path path) {
        JsonDBFiles files = new JsonDBFiles(path);
        return new StreamingJsonDB(
            unchecked(files::textInput, IORuntimeException::new),
            unchecked(files::writer, IORuntimeException::new),
            unchecked(files::swap, IORuntimeException::new),
            new JsonParser(),
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

    private static class JsonDBFiles {

        private final Path read, write;

        public JsonDBFiles(Path read) {
            this.read = read;
            try {
                this.write = Files.createTempFile(nextString(), "json");
            } catch (IOException e) {
                throw new IORuntimeException(e);
            }
        }

        TextInput textInput() throws IOException {
            return TextInput.wrap(Files.newByteChannel(read), Charset.defaultCharset()); // TODO charset
        }

        Writer writer() throws IOException {
            return Files.newBufferedWriter(write);
        }

        void swap() throws IOException {
            Files.move(write, read, REPLACE_EXISTING);
        }

    }

    private static class ReadWriteBuffers {

        private CharBuffer readBuffer, writerBuffer;

        ReadWriteBuffers(int size) {
            this.readBuffer = CharBuffer.allocate(size);
            this.writerBuffer = CharBuffer.allocate(size);
        }

        TextInput textInput() {
            return TextInput.wrap(readBuffer);
        }

        Appendable writer() {
            return writerBuffer;
        }

        void swap() {
            CharBuffer swap = readBuffer;
            readBuffer = writerBuffer;
            writerBuffer = swap.clear();
        }

    }

}
