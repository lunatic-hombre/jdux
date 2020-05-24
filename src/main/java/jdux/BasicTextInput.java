package jdux;

import java.io.IOException;
import java.nio.CharBuffer;
import java.util.function.IntPredicate;

class BasicTextInput implements TextInput {

    private final Readable reader;
    private final CharBuffer buffer;
    private final Runnable onClose;

    private long charsRead;
    private boolean closed;

    public BasicTextInput(Readable reader, CharBuffer buffer, Runnable onClose) {
        this(reader, buffer, onClose, 0L);
    }

    public BasicTextInput(Readable reader, CharBuffer buffer, Runnable onClose, long position) {
        this.reader = reader;
        this.buffer = buffer;
        this.onClose = onClose;
        this.charsRead = position;
        buffer.limit(0);
    }

    @Override
    public int peek() {
        return buffer.get(buffer.position());
    }

    @Override
    public String peek(int n) {
        if (!hasRemaining(n))
            return peekWhile(c -> true);
        return buffer.duplicate().limit(buffer.position() + n).toString();
    }

    @Override
    public String peekWhile(IntPredicate p) {
        buffer.mark();
        try {
            final StringBuilder sb = new StringBuilder();
            while (buffer.hasRemaining() && p.test(peek()))
                sb.append(buffer.get());
            return sb.toString();
        } finally {
            buffer.reset();
        }
    }

    @Override
    public int read() {
        return buffer.get();
    }

    @Override
    public boolean hasNext() {
        return buffer.hasRemaining() || readBuffer();
    }

    @Override
    public String read(int nchars) {
        if (!hasRemaining(nchars))
            throw new IllegalArgumentException("Could not read "+nchars+", reached end of input.");
        try {
            return buffer.subSequence(0, nchars).toString();
        } finally {
            advance(nchars);
        }
    }

    private void advance(int newPosition) {
        buffer.position(buffer.position() + newPosition);
    }

    private boolean hasRemaining(int n) {
        return n < buffer.capacity() && (buffer.position() + n <= buffer.limit() || readBuffer());
    }

    private boolean readBuffer() {
        try {
            buffer.compact();
            final int charsRead = reader.read(buffer);
            if (charsRead == -1) {
                buffer.limit(buffer.position());
                buffer.position(0);
                return false;
            }
            this.charsRead += charsRead;
            buffer.flip();
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long index() {
        return charsRead - (buffer.limit() - buffer.position());
    }

    @Override
    public TextInput index(long index) {
        throw new UnsupportedOperationException("Source for text input is not navigable!");
    }

    @Override
    public String toString() {
        return "..." + buffer.toString();
    }

    @Override
    public void close() {
        if (this.closed)
            return;
        try {
            TextInput.BUFFER_POOL.put(buffer);
            onClose.run();
            if (reader instanceof AutoCloseable ac)
                ac.close();
            this.closed = true;
        } catch (Exception e) {
            throw new IORuntimeException(e);
        }
    }

}
