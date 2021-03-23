package org.starrel.submitee.attribute;

import com.google.gson.JsonObject;

public class AttributeMapImpl<TContext extends AttributeHolder<?>> extends AttributeSpecImpl<Void> implements AttributeMap<TContext> {
    private final TContext holder;

    public AttributeMapImpl(TContext holder) {
        super(null, "", Void.class);
        this.holder = holder;
        setSource(new JsonTreeSource<>(Void.class));
    }

    @Override
    public TContext getHolder() {
        return holder;
    }

    @SuppressWarnings("unchecked")
    @Override
    public JsonObject serialize() {
        return ((JsonTreeSource<Void>) owningSource).getJsonRoot().getAsJsonObject();
    }
}
