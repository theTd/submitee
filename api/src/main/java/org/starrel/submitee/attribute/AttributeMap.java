package org.starrel.submitee.attribute;

import com.google.gson.JsonObject;
import com.mongodb.client.MongoDatabase;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Predicate;

public interface AttributeMap<TContext extends AttributeHolder<?>> extends AttributeSpec<Void> {

    static boolean includePath(String path, String match) {
        if (path.isEmpty()) return true;

        Iterator<String> pathIterator = Arrays.stream(path.split("\\.")).iterator();
        Iterator<String> matchIterator = Arrays.stream(match.split("\\.")).iterator();
        String node;
        while (matchIterator.hasNext()) {
            node = matchIterator.next();
            if (!pathIterator.hasNext()) {
                if (!pathIterator.next().equals(node)) {
                    return false;
                }
            }
        }
        return true;
    }

    TContext getHolder();

    JsonObject toJsonTree(Predicate<String> pathFilter);

    JsonObject toJsonTree();

    boolean getAutoSaveAttribute();

    void setAutoSaveAttribute(boolean autoSaveAttribute);

    void saveAttribute(MongoDatabase mongoDatabase);

    void readAttribute(MongoDatabase mongoDatabase);

}
