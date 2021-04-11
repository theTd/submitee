package org.starrel.submitee;

public class ClassifiedException extends Exception {
    private final String classify;

    public ClassifiedException(String classify, String message) {
        super(message);
        this.classify = classify;
    }

    public ClassifiedException(Throwable cause, String classify, Object... messageParts) {
        super(I18N.fromKey(classify).format(((String) null), messageParts), cause);
        this.classify = classify;
    }

    public String getClassify() {
        return classify;
    }
}
