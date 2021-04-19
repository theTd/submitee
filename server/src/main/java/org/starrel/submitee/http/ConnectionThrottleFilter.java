package org.starrel.submitee.http;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.starrel.submitee.*;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class ConnectionThrottleFilter extends HttpFilter {
    private final static int THRESHOLD = 50;
    private final static long THRESHOLD_PERIOD = 10000;

    private final Cache<String, TimeThrottleList> cache = CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES).build();

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
        String addr = Util.getRemoteAddr(req);
        req.setAttribute("REMOTE_ADDR", addr);

        TimeThrottleList timeList;
        try {
            timeList = cache.get(addr, () -> new TimeThrottleList(THRESHOLD_PERIOD, THRESHOLD));
        } catch (ExecutionException ignored) {
            return;
        }
        if (!timeList.checkViolation()) {
            SubmiteeServer.getInstance().pushEvent(Level.WARNING,
                    ConnectionThrottleFilter.class, "rejecting connection throttle violation",
                    "addr=" + addr + ", uri=" + req.getRequestURI());
            SubmiteeHttpServlet.responseClassifiedError(req, res, ClassifiedErrors.TOO_MANY_REQUEST);
            return;
        }
        chain.doFilter(req, res);
    }

}
