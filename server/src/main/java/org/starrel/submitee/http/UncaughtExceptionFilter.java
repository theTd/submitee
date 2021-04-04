package org.starrel.submitee.http;

import org.starrel.submitee.ExceptionReporting;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpFilter;

public class UncaughtExceptionFilter extends HttpFilter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) {
        try {
            chain.doFilter(req, res);
        } catch (Exception e) {
            ExceptionReporting.report(UncaughtExceptionFilter.class, "uncaught exception", e);
        }
    }
}
