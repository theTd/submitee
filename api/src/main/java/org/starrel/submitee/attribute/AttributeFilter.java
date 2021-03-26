package org.starrel.submitee.attribute;

public interface AttributeFilter<TValue> {

    default void onSet(TValue value) throws FilterException {
    }

    default void onSet(String path, Object value) throws FilterException {
    }

    default void onDelete(String path) throws FilterException {
    }

    class FilterException extends RuntimeException {
        public FilterException(String message) {
            super(message);
        }
    }
}
