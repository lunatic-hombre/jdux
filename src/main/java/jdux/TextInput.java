package jdux;

import java.io.StringReader;
import java.nio.CharBuffer;
import java.util.function.IntPredicate;

import static java.lang.Character.toLowerCase;

/**
 * Simplifies reading in text data.  Allows for peeking and skipping.
 *
 * Note: it is recommended to wrap Readers and close upon end of use so that CharBuffers may be reused.
 */
public interface TextInput extends AutoCloseable {

    Pool<CharBuffer> BUFFER_POOL = Pool.create(() -> CharBuffer.allocate(1024));

    static TextInput wrap(String str) {
        return wrap(new StringReader(str));
    }

    static TextInput wrap(Readable in) {
        final CharBuffer buffer = BUFFER_POOL.get();
        return new BasicTextInput(in, buffer, () -> BUFFER_POOL.put(buffer));
    }
    static TextInput wrap(Readable in, long position) {
        final CharBuffer buffer = BUFFER_POOL.get();
        return new BasicTextInput(in, buffer, () -> BUFFER_POOL.put(buffer), position);
    }

    int read();
    String read(int nchars);
    default String readRemaining() {
        final StringBuilder sb = new StringBuilder();
        while (hasNext())
            sb.append((char) read());
        return sb.toString();
    }
    default String readLine() {
        return readUntilSkipping('\n');
    }
    default String readUntil(IntPredicate predicate) {
        return readWhile(predicate.negate());
    }
    default String readUntil(char until) {
        return readWhile(c -> c != until);
    }
    default String readUntilSkipping(char until) {
        final String s = readWhile(c -> c != until);
        if (hasNext())
            skip();
        return s;
    }
    default String readUntilWithEscape(int until, int escape) {
        final StringBuilder sb = new StringBuilder();
        while (hasNext() && peek() != until)
            sb.append((char)(peek() == escape && hasNext() ? skip().read() : read()));
        return sb.toString();
    }
    default String readWhile(IntPredicate p) {
        final StringBuilder sb = new StringBuilder();
        while (hasNext() && p.test(peek()))
            sb.append((char) read());
        return sb.toString();
    }
    default String readUntil(CharSequence str) {
        final StringBuilder sb = new StringBuilder();
        while (hasNext()) {
            if (str.charAt(0) == peek() && str.equals(peek(str.length())))
                break;
            sb.append((char) read());
        }
        return sb.toString();
    }

    int peek();
    String peek(int length);
    String peekWhile(IntPredicate p);

    default TextInput skipIgnoreCase(char ch) {
        return skipIgnoreCase(new String(new char[]{ch}));
    }
    default TextInput skipIgnoreCase(String str) {
        int i=0;
        while (hasNext() && i < str.length()) {
            if (toLowerCase(str.charAt(i++)) != toLowerCase(peek()))
                throw new IllegalStateException("Expected \""+str.substring(i-1)+"\" but was "+((char) peek())
                        +" for input \""+this.toString()+"\".");
            read();
        }
        return this;
    }
    default TextInput skipWhitespace() {
        return skipWhile(Character::isWhitespace);
    }
    default TextInput skipUntil(int ch) {
        return skipWhile(c -> c != ch);
    }
    default TextInput skipUntil(CharSequence str) {
        while (hasNext()) {
            skipUntil(str.charAt(0));
            if (peek(str.length()).equals(str))
                return this;
        }
        throw new IllegalStateException("Expected \""+str+"\" but reached end of input.");
    }
    default TextInput skipWhile(IntPredicate p) {
        while (hasNext() && p.test(peek()))
            read();
        return this;
    }
    default TextInput skip(char ch) {
        if (hasNext() && read() != ch)
            throw new IllegalStateException("Expected \""+ch+"\".");
        return this;
    }
    default TextInput skip(int count) {
        for (int i=0; i < count; i++)
            read();
        return this;
    }
    default TextInput skip() {
        read();
        return this;
    }

    @Override
    void close();

    long index();

    TextInput index(long index);

    boolean hasNext();
}
