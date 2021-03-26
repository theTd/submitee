package org.starrel.submitee.attribute;

public interface AttributeFilter<TValue> {

    void onSet(TValue value) throws FilterException;

    void onSet(String path, Object value) throws FilterException;

    void onDelete(String path) throws FilterException;

    class FilterException extends Exception {
        public FilterException(String message) {
            super(message);
        }
    }
}
