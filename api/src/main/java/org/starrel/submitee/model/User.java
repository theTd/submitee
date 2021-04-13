package org.starrel.submitee.model;

import org.bson.conversions.Bson;
import org.starrel.submitee.attribute.AttributeHolder;

import java.util.List;
import java.util.concurrent.ExecutionException;

public interface User extends AttributeHolder<User> {
    String ATTRIBUTE_COLLECTION_NAME = "users";

    default boolean isAnonymous() {
        return false;
    }

    @Override
    default String getAttributePersistKey() {
        return getDescriptor().toString();
    }

    String getTypeId();

    String getId();

    List<? extends Submission> getSubmissions(Bson query) throws ExecutionException;

    Submission createSubmission(STemplate template);

    String getPreferredLanguage();

    void setPreferredLanguage(String language);

    UserDescriptor getDescriptor();
}
