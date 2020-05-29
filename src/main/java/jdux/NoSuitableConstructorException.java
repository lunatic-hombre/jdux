package jdux;

public class NoSuitableConstructorException extends JsonReflectException {
    public NoSuitableConstructorException() {
        super("No suitable constructor found!");
    }
}
