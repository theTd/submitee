package org.starrel.submitee.http;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.starrel.submitee.EventLogService;
import org.starrel.submitee.ExceptionReporting;
import org.starrel.submitee.SubmiteeServer;
import org.starrel.submitee.model.User;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class EventsServlet extends SubmiteeHttpServlet {
    {
        setBaseUri("/events");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = getSession(req).getUser();
        if (!user.isSuperuser()) {
            responseAccessDenied(req, resp);
            return;
        }

        String level = req.getParameter("level");
        String entity = req.getParameter("entity");
        String activity = req.getParameter("activity");
        String detail = req.getParameter("detail");
        String startStr = req.getParameter("start");
        String limitStr = req.getParameter("limit");

        long start = startStr == null || startStr.isEmpty() ? -1 : Long.parseLong(startStr);
        int limit = limitStr == null || limitStr.isEmpty() ? -1 : Integer.parseInt(limitStr);

        if (entity != null) {
            entity = entity.replace("%", "[%]").replace("*", "%");
            entity = "%" + entity + "%";
        }
        if (activity != null) {
            activity = activity.replace("%", "[%]").replace("*", "%");
            activity = "%" + activity + "%";
        }
        if (detail != null) {
            detail = detail.replace("%", "[%]").replace("*", "%");
            detail = "%" + detail + "%";
        }

        CompletableFuture<List<EventLogService.EventCollapseContext>> query =
                SubmiteeServer.getInstance().getEventLogService().query(start, limit, level, entity, activity, detail);

        AsyncContext asyncContext = req.startAsync();
        asyncContext.start(() -> {
            try {
                resp.setStatus(HttpStatus.OK_200);
                resp.setContentType("application/json");
                SubmiteeServer.GSON.toJson(query.get(), resp.getWriter());
            } catch (Exception e) {
                ExceptionReporting.report(EventsServlet.class, "querying events", e);
                responseInternalError(req, resp);
            } finally {
                asyncContext.complete();
            }
        });
    }
}
