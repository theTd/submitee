package org.starrel.submitee.model;

import org.starrel.submitee.ClassifiedException;

public interface FieldController {

    String getTypeId();

    void validate(SField field) throws ClassifiedException;

    default String export(SField field) {
        throw new UnsupportedOperationException();
    }
}
