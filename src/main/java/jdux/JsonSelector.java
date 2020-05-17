package jdux;

public interface JsonSelector extends JsonSelectorSegment {

    default int length() {
        int count = 1;
        for (var current = this; current.hasNext(); current = current.next())
            count++;
        return count;
    }

    boolean contains(JsonSelector other);

    boolean hasNext();

    JsonSelector next();

    // TODO dumb method for interface
    String toString(String child, String ancestor);

}
