package jdux;

public class JsonParseException extends RuntimeException {

    protected TextInput text;

    JsonParseException(String msg) {
        super(msg);
    }
    JsonParseException(String msg, TextInput text) {
        super(msg);
        this.text = text;
    }
    JsonParseException(Throwable cause, TextInput text) {
        super(cause);
        this.text = text;
    }
    JsonParseException setText(TextInput text) {
        this.text = text;
        return this;
    }
    @Override
    public String getMessage() {
        return (super.getMessage() == null ? "" : super.getMessage()) + " at index "+text.index()+", \"..." + text.readLine() + "\"";
    }

}
