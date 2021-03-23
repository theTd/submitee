package org.starrel.submitee.model;

import com.google.gson.JsonElement;

public interface SField<TValue> {
    String getName();

    /**
     * @return plain text or html
     */
    Object getDescription();

    TValue parse(JsonElement json);

    /**
     * @return basic value or bson document
     */
    JsonElement write(TValue value);

    /**
     * @return style sheet
     */
    Object getStyle(TValue selectedValue);

    /**
     * @return html
     */
    Object getForm(TValue selectedValue);
}
