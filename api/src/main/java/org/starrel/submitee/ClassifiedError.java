package org.starrel.submitee;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.SneakyThrows;

public class ClassifiedError {
    private final String distinguishName;
    private final int httpStatus;
    private final I18N.I18NKey messageKey;
    private final Class<?>[] messagePartTypes;

    private ClassifiedError(String distinguishName, int httpStatus, I18N.I18NKey messageKey, Class<?>... messagePartTypes) {
        this.distinguishName = distinguishName;
        this.httpStatus = httpStatus;
        this.messageKey = messageKey;
        this.messagePartTypes = messagePartTypes;
    }

    private final static Cache<String, ClassifiedError> CACHE = CacheBuilder.newBuilder().build();

    @SneakyThrows
    public static ClassifiedError create(String distinguishName, int httpStatus, I18N.I18NKey messageKey, Class<?>... messagePartTypes) {
        return CACHE.get(distinguishName, () -> new ClassifiedError(distinguishName, httpStatus, messageKey, messagePartTypes));
    }

    public String getDistinguishName() {
        return distinguishName;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public I18N.I18NKey getMessageKey() {
        return messageKey;
    }

    public Class<?>[] getMessagePartTypes() {
        return messagePartTypes;
    }

}
