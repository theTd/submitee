package org.starrel.submitee;

import org.slf4j.LoggerFactory;

import java.util.logging.Level;
import java.util.logging.Logger;

public interface ExceptionReporting {

    static void report(String entity, String activity, String detail) {
        try {
            SServer.getInstance().reportException(entity, activity, detail);
        } catch (Throwable e) {
            LoggerFactory.getLogger(ExceptionReporting.class).error("failed reporting exception", e);
            LoggerFactory.getLogger(ExceptionReporting.class).error(String.format("reported: entity=%s, activity=%s, detail=%s", entity, activity, detail));
        }
    }

    static void report(String entity, String activity, Throwable stacktrace) {
        try {
            SServer.getInstance().reportException(entity, activity, stacktrace);
        } catch (Throwable e) {
            LoggerFactory.getLogger(ExceptionReporting.class).error("failed reporting exception", e);
            LoggerFactory.getLogger(ExceptionReporting.class).error(String.format("reported: entity=%s, activity=%s, stacktrace=", entity, activity), stacktrace);
        }
    }

    static void report(Class<?> reporter, String activity, String detail) {
        report(reporter.getName(), activity, detail);
    }

    static void report(Class<?> reporter, String activity, Throwable stacktrace) {
        report(reporter.getName(), activity, stacktrace);
    }
}
