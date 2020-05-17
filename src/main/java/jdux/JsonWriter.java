package jdux;

import java.io.IOException;

public class JsonWriter {

    private boolean pretty = false;
    private String indent = "  ";

    public JsonWriter() {}

    public JsonWriter(boolean pretty, String indent) {
        this.pretty = pretty;
        this.indent = indent;
    }

    public JsonWriter setPretty(boolean pretty) {
        this.pretty = pretty;
        return this;
    }

    public JsonWriter setIndent(String indent) {
        this.indent = indent;
        return this;
    }

    public String toString(JsonNode node) {
        StringBuilder sb = new StringBuilder();
        write(node, sb);
        return sb.toString();
    }

    public void write(JsonNode node, Appendable out) {
        write(0, node, out);
    }

    private void write(int depth, JsonNode node, Appendable out) {
        if (node instanceof ArrayNode an)
            writeArray(depth, an, out);
        else if (node instanceof ObjectNode on)
            writeObject(depth, on, out);
        else if (node instanceof JsonNode.LabelledNode ln)
            writeLabelled(depth, ln, out);
        else
            writeValue(node, out);
    }

    private void writeArray(int depth, ArrayNode an, Appendable out) {
        writeCollection(depth, an, out, '[', ']');
    }

    private void writeObject(int depth, ObjectNode on, Appendable out) {
        writeCollection(depth, on, out, '{', '}');
    }

    private void writeLabelled(int depth, JsonNode.LabelledNode ln, Appendable out) {
        try {
            out.append('"').append(ln.label()).append('"').append(':');
            if (pretty)
                out.append(' ');
            write(depth, ln.unlabelled(), out);
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    private void writeValue(JsonNode node, Appendable out) {
        try {
            out.append(node.toString());
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    private void writeCollection(int depth, JsonNode node, Appendable out, char open, char close) {
        try {
            out.append(open);
            var children = node.childrenIter();
            if (!children.hasNext()) {
                out.append(close);
                return;
            }
            depth++;
            spacing(depth, out).write(depth, children.next(), out);
            while (children.hasNext())
                spacing(depth, out.append(',')).write(depth, children.next(), out);
            spacing(depth - 1, out);
            out.append(close);
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    private JsonWriter spacing(int depth, Appendable out) throws IOException {
        if (pretty) {
            out.append(System.lineSeparator());
            for (int i = 0; i < depth; i++)
                out.append(indent);
        }
        return this;
    }

}
