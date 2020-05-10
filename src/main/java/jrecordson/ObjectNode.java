package jrecordson;

import java.util.Iterator;
import java.util.stream.Stream;

public interface ObjectNode extends JsonNode {

    @Override
    Iterator<? extends LabelledNode> iterator();

    @Override
    Stream<? extends LabelledNode> stream();

    JsonNode get(String key);

}
