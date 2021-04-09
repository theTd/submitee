package org.starrel.submitee.http;

import com.google.gson.JsonObject;
import org.starrel.submitee.attribute.AttributeMap;
import org.starrel.submitee.attribute.AttributeMapImpl;
import org.starrel.submitee.attribute.AttributeSpec;
import org.starrel.submitee.model.SField;
import org.starrel.submitee.model.STemplateImpl;

public class SFieldImpl implements SField {
    private final STemplateImpl template;
    private final AttributeMap<SFieldImpl> attributeMap;

    private final AttributeSpec<String> name;
    private final AttributeSpec<String> type;

    public SFieldImpl(STemplateImpl template, JsonObject attributeBody) {
        this.template = template;
        this.attributeMap = new AttributeMapImpl<>(this, null);
        this.attributeMap.set("", attributeBody);

        this.name = this.attributeMap.of("name", String.class);
        this.type = this.attributeMap.of("type", String.class);
    }

    @Override
    public String getAttributePersistKey() {
        return null;
    }

    @Override
    public AttributeMap<SFieldImpl> getAttributeMap() {
        return attributeMap;
    }

    @Override
    public String getName() {
        return name.get();
    }

    @Override
    public String getType() {
        return type.get();
    }

    @Override
    public void attributeUpdated(String path) {
        // TODO: 2021-04-08-0008 parent
    }
}
