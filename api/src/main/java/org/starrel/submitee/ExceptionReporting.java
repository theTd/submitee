package org.starrel.submitee;

import java.util.logging.Level;
import java.util.logging.Logger;

public interface ExceptionReporting {
    static void report(Throwable throwable) {
        try {
            SServer.getInstance().reportException(throwable);
        } catch (Throwable e) {
            Logger.getLogger(ExceptionReporting.class.getName()).log(Level.SEVERE, "failed reporting exception", e);
            Logger.getLogger(ExceptionReporting.class.getName()).log(Level.SEVERE, "printing stacktrace of reported exception", throwable);
        }
    }

    static void report(String activity, Throwable throwable) {
        try {
            SServer.getInstance().reportException(activity, throwable);
        } catch (Throwable e) {
            Logger.getLogger(ExceptionReporting.class.getName()).log(Level.SEVERE, "failed reporting exception", e);
            Logger.getLogger(ExceptionReporting.class.getName()).log(Level.SEVERE,
                    "printing stacktrace of reported exception, activity: " + activity, throwable);
        }
    }

    static void report(String event) {
        try {
            SServer.getInstance().reportException(event);
        } catch (Throwable e) {
            Logger.getLogger(ExceptionReporting.class.getName()).log(Level.SEVERE, "failed reporting exception", e);
            Logger.getLogger(ExceptionReporting.class.getName()).log(Level.SEVERE, "reported exception is event:" + event);
        }
    }
}
