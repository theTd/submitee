package org.starrel.submitee.attribute;

public interface AttributeHolder<THolder extends AttributeHolder<?>> {

    /**
     * @return null if no case persist
     */
    String getAttributePersistKey();

    AttributeMap<THolder> getAttributeMap();

    void attributeUpdated(String path);
}
