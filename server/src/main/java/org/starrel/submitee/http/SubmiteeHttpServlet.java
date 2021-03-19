package org.starrel.submitee.http;

import org.starrel.submitee.SubmiteeServer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

public class SubmiteeHttpServlet extends HttpServlet {

    private SubmiteeServer server;

    @Override
    public void init() throws ServletException {
        server = (SubmiteeServer) getServletContext().getAttribute("server");
    }

    public SubmiteeServer getServer() {
        return server;
    }
}
