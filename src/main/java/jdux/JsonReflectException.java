package jdux;

public class JsonReflectException extends RuntimeException {
    public JsonReflectException(Throwable cause) {
        super(cause);
    }
    public JsonReflectException(String message) {
        super(message);
    }
}
