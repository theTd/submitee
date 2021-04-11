package org.starrel.submitee;

public class ClassifiedException extends Exception {
    private final String classify;
    private final Object[] messageParts;

    public ClassifiedException(String classify, Object... messageParts) {
        super(I18N.fromKey(classify).format(((String) null), messageParts));
        this.classify = classify;
        this.messageParts = messageParts;
    }

    public ClassifiedException(Throwable cause, String classify, Object... messageParts) {
        super(I18N.fromKey(classify).format(((String) null), messageParts), cause);
        this.classify = classify;
        this.messageParts = messageParts;
    }

    public String getClassify() {
        return classify;
    }

    public Object[] getMessageParts() {
        return messageParts;
    }
}
