package org.starrel.submitee.model;

import org.bson.conversions.Bson;
import org.starrel.submitee.attribute.AttributeHolder;

import java.util.List;

public interface User extends AttributeHolder<User> {

    boolean isAnonymous();

    String getTypeId();

    String getId();

    List<Submission> getSubmissions(Bson query);
}
