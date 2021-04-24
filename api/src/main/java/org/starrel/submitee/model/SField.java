package org.starrel.submitee.model;

import org.starrel.submitee.attribute.AttributeHolder;

public interface SField extends AttributeHolder<SField> {

    String getName();

    String getType();

    String getComment();
}
