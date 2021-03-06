package org.starrel.submitee;

import org.eclipse.jetty.http.HttpStatus;

import java.util.Locale;
import java.util.Objects;

public class ClassifiedException extends Exception {
    private final ClassifiedError classifiedError;
    private Object[] messageParts = null;

    public ClassifiedException(ClassifiedError classifiedError) {
        super(classifiedError.getMessageKey().format());
        this.classifiedError = classifiedError;
    }

    public ClassifiedException(ClassifiedError classifiedError, Object... messageParts) {
        super(classifiedError.getMessageKey().format(((String) null), messageParts));
        this.classifiedError = classifiedError;
        this.messageParts = messageParts;
    }

    public ClassifiedException(String classify, int httpStatus) {
        this(ClassifiedError.create(classify, httpStatus, I18N.fromKey(String.format("errors.%s", classify.toLowerCase(Locale.ROOT)))));
    }

    public ClassifiedException(String classify, int httpStatus, Object... messageParts) {
        this(ClassifiedError.create(classify, httpStatus, I18N.fromKey(String.format("errors.%s", classify.toLowerCase(Locale.ROOT)))), messageParts);
    }

    public ClassifiedException(String classify) {
        this(classify, HttpStatus.INTERNAL_SERVER_ERROR_500);
    }

    public ClassifiedError getClassifiedError() {
        return classifiedError;
    }

    public Object[] getMessageParts() {
        return messageParts;
    }

    public String getDistinguishName() {
        return classifiedError.getDistinguishName();
    }
}
