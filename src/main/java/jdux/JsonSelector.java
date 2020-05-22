package jdux;

import static jdux.JsonSelectType.DESCENDANT;

public interface JsonSelector extends Iterable<JsonSelectorSegment> {

    default int length() {
        var count = 0;
        var iter = iterator();
        for (; iter.hasNext(); iter.next())
            count++;
        return count;
    }

    JsonSelectorSegment root();

    JsonSelector next();

    default boolean hasNext() {
        return next() != null;
    }

    default String key() {
        return root().key();
    }

    default JsonSelectType type() {
        return root().type();
    }

    boolean contains(JsonSelector other);

    default String toString(String child, String descendant) {
        var iter = iterator();
        if (!iter.hasNext())
            return "";
        var seg = iter.next();
        var sb = new StringBuilder(seg.type() == DESCENDANT ? descendant + seg.key() : seg.key());
        while (iter.hasNext())
            sb.append(seg.type() == DESCENDANT ? descendant + seg.key() : child + seg.key());
        return sb.toString();
    }
}
