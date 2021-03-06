package org.starrel.submitee.attribute;

public interface AttributeHolder<THolder extends AttributeHolder<?>> {

    /**
     * @return null if no case persist
     */
    String getAttributePersistKey();

    AttributeMap<? extends THolder> getAttributeMap();

    default String getAttributeScheme() {
        return null;
    }

    default <TValue> TValue getAttribute(String path, Class<TValue> type) {
        return getAttributeMap().get(path, type);
    }

    default <TValue> TValue getAttribute(String path, Class<TValue> type, TValue defaultValue) {
        return getAttributeMap().get(path, type, defaultValue);
    }

    default void setAttribute(String path, Object value) {
        getAttributeMap().set(path, value);
    }

    /**
     * @return x
     * @deprecated switch to acl
     */
    @Deprecated
    default boolean isPublicAccessible() {
        return false;
    }

    void attributeUpdated(String path);
}
