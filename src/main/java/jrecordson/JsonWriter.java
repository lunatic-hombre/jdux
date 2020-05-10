package jrecordson;

import java.io.IOException;

public class JsonWriter {

    static final JsonWriter DEFAULT_WRITER = new JsonWriter();

    private boolean pretty = false;
    private String indent = "  ";

    public JsonWriter() {}

    public JsonWriter(boolean pretty, String indent) {
        this.pretty = pretty;
        this.indent = indent;
    }

    public void setPretty(boolean pretty) {
        this.pretty = pretty;
    }

    public void setIndent(String indent) {
        this.indent = indent;
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
        else if (node instanceof JsonNode.ValueNode<?> vn)
            writeValue(vn, out);
        else
            throw new JsonReflectException("Unknown node type " + node);
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

    private void writeValue(JsonNode.ValueNode<?> node, Appendable out) {
        try {
            out.append(node.toString());
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    private void writeCollection(int depth, JsonNode node, Appendable out, char open, char close) {
        try {
            out.append(open);
            var children = node.iterator();
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
