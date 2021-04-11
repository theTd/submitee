package org.starrel.submitee.http;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.starrel.submitee.I18N;
import org.starrel.submitee.SubmiteeServer;
import org.starrel.submitee.Util;
import org.starrel.submitee.model.User;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class ConnectionThrottleFilter extends HttpFilter {
    private final static long THRESHOLD = 50;
    private final static int THRESHOLD_PERIOD = 10000;

    private final Cache<String, TimeList> cache = CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES).build();

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
        String addr = Util.getRemoteAddr(req);
        req.setAttribute("REMOTE_ADDR", addr);

        TimeList timeList;
        try {
            timeList = cache.get(addr, TimeList::new);
        } catch (ExecutionException ignored) {
            return;
        }
        if (timeList.checkViolation()) {
            res.setStatus(HttpStatus.TOO_MANY_REQUESTS_429);
            res.getWriter().println(I18N.Http.TOO_MANY_REQUEST.format(req));
            res.getWriter().close();
            return;
        }
        chain.doFilter(req, res);
    }

    private static class TimeList {
        LinkedList<Long> timeList = new LinkedList<>();

        synchronized boolean checkViolation() {
            long now = System.currentTimeMillis();
            long edge = now - THRESHOLD_PERIOD;

            Iterator<Long> iterator = timeList.descendingIterator();
            int hit = 0;
            while (iterator.hasNext()) {
                if (iterator.next() > edge) {
                    if (++hit >= THRESHOLD) {
                        return true;
                    }
                } else {
                    break;
                }
            }
            timeList.add(now);
            while (timeList.size() > THRESHOLD) {
                timeList.removeFirst();
            }
            return false;
        }
    }
}
