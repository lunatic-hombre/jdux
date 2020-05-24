package jdux;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.CharBuffer;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;

import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

public final class JDux {

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
        return new StreamingJsonDB(new ReadWriteBuffers(size));
    }

    /**
     * Create a file JSON database.
     * @param path path to your chosen JSON file
     */
    public static JsonDB fileDB(Path path) {
        return new StreamingJsonDB(new FileChannels(path));
    }

    public static void setPretty() {
        DEFAULT_WRITER.setPretty(true);
    }

    private static class JsonDBFiles {

        private final Path file;

        public JsonDBFiles(Path file) {
            this.file = file;
        }

        TextInput textInput() throws IOException {
            return TextInput.wrap(Files.newBufferedReader(file)); // TODO charset
        }

        Writer writer() throws IOException {
            return Files.newBufferedWriter(file);
        }

    }

    private static class FileChannels implements StreamingJsonDB.StreamOptions<Writer> {

        private final SeekableByteChannel read, write;

        public FileChannels(Path file) {
            try {
                this.read = Files.newByteChannel(file, new HashSet<>(Collections.singleton(READ)));
                this.write = Files.newByteChannel(file, new HashSet<>(Collections.singleton(WRITE)));
            } catch (IOException e) {
                throw new IORuntimeException(e);
            }
        }

        @Override
        public TextInput input() {
            return TextInput.wrap(Channels.newReader(read, StandardCharsets.US_ASCII));
        }

        @Override
        public Writer output() {
            return Channels.newWriter(write, StandardCharsets.US_ASCII);
        }

        @Override
        public void after(Writer output) {
            try {
                output.flush();
            } catch (IOException e) {
                throw new IORuntimeException(e);
            }
        }
    }
    private static class ReadWriteBuffers implements StreamingJsonDB.StreamOptions<CharBuffer> {

        private final CharBuffer read, write;

        ReadWriteBuffers(int size) {
            this.write = CharBuffer.allocate(size);
            this.read = write.asReadOnlyBuffer();
        }

        @Override
        public TextInput input() {
            read.position(0);
            return TextInput.wrap(read.asReadOnlyBuffer());
        }

        @Override
        public CharBuffer output() {
            write.position(0);
            return write;
        }

        @Override
        public void after(CharBuffer output) {} // do nothing
    }

}
