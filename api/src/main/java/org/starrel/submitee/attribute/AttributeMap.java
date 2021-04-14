package org.starrel.submitee.attribute;

import com.google.gson.JsonObject;

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

    boolean getAutoSaveAttribute();

    void setAutoSaveAttribute(boolean autoSaveAttribute);

    void save();

    void read();

}
