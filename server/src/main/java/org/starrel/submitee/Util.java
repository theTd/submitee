package org.starrel.submitee;

import jakarta.servlet.http.HttpServletRequest;

public class Util {
    public static String getRemoteAddr(HttpServletRequest req){
        String addr = null;
        String header = req.getHeader("X-Forwarded-For");
        if (header != null) {
            int idx = header.indexOf(",");
            addr = idx == -1 ? header : header.substring(0, idx);
        }
        if (addr == null) {
            addr = req.getRemoteAddr();
        }
        return addr;
    }
}
