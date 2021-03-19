package org.starrel.submitee.attribute;

import org.bson.Document;

import java.util.List;

public interface AttributeController<TValue, TContext> {

    Class<TValue> getType();

    String getPath();

    List<String> getDependency();

    boolean isPersist();

    default TValue parse(Document bson) {
        return null;
    }

    default Document write(TValue value) {
        return null;
    }

    default TValue defaultValue(TContext context) {
        return null;
    }

    void validate(TValue value, TContext context) throws InvalidAttributeSignal;

}
