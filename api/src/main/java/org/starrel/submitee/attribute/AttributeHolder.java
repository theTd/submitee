package org.starrel.submitee.attribute;

public interface AttributeHolder<THolder extends AttributeHolder<?>> {

    /**
     * @return null if no case persist
     */
    String getAttributePersistKey();

    AttributeMap<? extends THolder> getAttributeMap();

    default <TValue> TValue getAttribute(String path, Class<TValue> type) {
        return getAttributeMap().get(path, type);
    }

    default void setAttribute(String path, Object value) throws AttributeFilter.FilterException {
        getAttributeMap().set(path, value);
    }

    void attributeUpdated(String path);
}
