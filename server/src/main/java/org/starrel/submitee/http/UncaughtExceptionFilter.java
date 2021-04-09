package org.starrel.submitee.http;

import org.starrel.submitee.ExceptionReporting;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpFilter;

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
