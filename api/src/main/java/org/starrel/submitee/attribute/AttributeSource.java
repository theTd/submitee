package org.starrel.submitee.attribute;

import java.util.List;

public interface AttributeSource {

    <TValue> TValue getAttribute(String path, Class<TValue> type);

    void setAttribute(String path, Object value);

    List<String> listKeys(String path);

    void delete(String path);
}
