package org.starrel.submitee.auth;

import org.starrel.submitee.SubmiteeServer;
import org.starrel.submitee.model.AbstractUser;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class AnonymousUser extends AbstractUser {

    public AnonymousUser(String userId) {
        super(AnonymousUserRealm.TYPE_ID, userId);
        getAttributeMap().setAutoSaveAttribute(false);
    }

    @Override
    public boolean isAnonymous() {
        return true;
    }

    private final static String[] IGNORED_ATTRIBUTES = new String[]
            {"create-time", "last-seen", "preferred-language"};
    private final static Set<String> SET_IGNORED_ATTRIBUTES;

    static {
        SET_IGNORED_ATTRIBUTES = Set.copyOf(Arrays.stream(IGNORED_ATTRIBUTES).collect(Collectors.toList()));
    }

    @Override
    public void attributeUpdated(String path) {
        if (path == null) return;
        if (path.isEmpty() || !SET_IGNORED_ATTRIBUTES.contains(path)) {
            getAttributeMap().saveAttribute(SubmiteeServer.getInstance().getMongoDatabase());
        }
    }
}
