package org.starrel.submitee;

import org.slf4j.LoggerFactory;

import java.util.logging.Level;

public interface ExceptionReporting {

    static void shrinkStackTrace(Throwable throwable) {
        StackTraceElement[] original = throwable.getStackTrace();
        int shrinkLength = 0;
        for (StackTraceElement stackTraceElement : original) {
            if (!stackTraceElement.getClassName().equals(ExceptionReporting.class.getName())) {
                shrinkLength++;
            } else {
                break;
            }
        }
        StackTraceElement[] shrink = new StackTraceElement[shrinkLength];
        System.arraycopy(original, 0, shrink, 0, shrinkLength);
        throwable.setStackTrace(shrink);
    }

    static void report(String entity, String activity, String detail) {
        try {
            SServer.getInstance().pushEvent(Level.SEVERE, entity, activity, detail);
        } catch (Throwable e) {
            shrinkStackTrace(e);
            LoggerFactory.getLogger(ExceptionReporting.class).error("failed reporting exception", e);
            LoggerFactory.getLogger(ExceptionReporting.class).error(String.format("reported: entity=%s, activity=%s, detail=%s", entity, activity, detail));
        }
    }

    static void report(String entity, String activity, Throwable stacktrace) {
        try {
            SServer.getInstance().pushEvent(Level.SEVERE, entity, activity, stacktrace);
        } catch (Throwable e) {
            shrinkStackTrace(e);
            LoggerFactory.getLogger(ExceptionReporting.class).error("failed reporting exception", e);
            LoggerFactory.getLogger(ExceptionReporting.class).error(String.format("reported: entity=%s, activity=%s, stacktrace=", entity, activity), stacktrace);
        }
    }

    static void report(String entity, String activity, String detail, Throwable stacktrace) {
        try {
            SServer.getInstance().pushEvent(Level.SEVERE, entity, activity, detail, stacktrace);
        } catch (Throwable e) {
            shrinkStackTrace(e);
            LoggerFactory.getLogger(ExceptionReporting.class).error("failed reporting exception", e);
            LoggerFactory.getLogger(ExceptionReporting.class).error(String.format("reported: entity=%s, activity=%s, detail=%s stacktrace=", entity, activity, detail), stacktrace);
        }
    }

    static void report(Class<?> reporter, String activity, String detail) {
        report(reporter.getName(), activity, detail);
    }

    static void report(Class<?> reporter, String activity, Throwable stacktrace) {
        report(reporter.getName(), activity, stacktrace);
    }

    static void report(Class<?> reporter, String activity, String detail, Throwable stacktrace) {
        report(reporter.getName(), activity, detail, stacktrace);
    }
}
