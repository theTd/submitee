package org.starrel.submitee.http;

import org.starrel.submitee.model.Session;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AuthServlet extends SubmiteeHttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setStatus(200);
        resp.getWriter().println("auth servlet test");
        Session session = getSession(req);
        if (session.isAnonymous()) {
        }
        resp.getWriter().println(req.getSession().getId());
        resp.getWriter().close();
    }
}
