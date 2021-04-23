package org.starrel.submitee.http;

import com.github.stuxuhai.jpinyin.PinyinFormat;
import com.github.stuxuhai.jpinyin.PinyinHelper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.starrel.submitee.ExceptionReporting;
import org.starrel.submitee.JsonUtil;
import org.starrel.submitee.SubmiteeServer;

import java.io.IOException;
import java.util.*;

public class PinyinServlet extends AbstractJsonServlet {
    private final static Cache<String, List<String>> CACHE =
            CacheBuilder.newBuilder().maximumSize(1000).build();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp, JsonObject body) throws ServletException, IOException {
        JsonArray list = JsonUtil.parseArray(body, "list");
        if (list == null) {
            responseBadRequest(req, resp);
            return;
        }
        AsyncContext asyncContext = req.startAsync();
        asyncContext.start(() -> {
            try {
                JsonArray response = new JsonArray();
                for (JsonElement element : list) {
                    if (!element.isJsonPrimitive()) continue;
                    JsonPrimitive p = element.getAsJsonPrimitive();
                    if (!p.isString()) continue;
                    String str = p.getAsString();
                    List<String> pinyinList = CACHE.get(str, () -> getPossiblePinyinList(str));
                    JsonArray inst = new JsonArray();
                    pinyinList.forEach(inst::add);
                    response.add(inst);
                }
                resp.setStatus(HttpStatus.OK_200);
                resp.setContentType("application/json");
                resp.getWriter().println(SubmiteeServer.GSON.toJson(response));
            } catch (Exception e) {
                ExceptionReporting.report(PinyinServlet.class, "writing response", e);
                responseInternalError(req, resp);
            } finally {
                asyncContext.complete();
            }
        });
    }

    public static List<String> getPossiblePinyinList(String mixedString) {
        List<Object> list = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        for (char ch : mixedString.toCharArray()) {
            if (filterChar(ch)) {
                if (sb.length() != 0) {
                    list.add(sb.toString());
                    sb = new StringBuilder();
                }
                list.add(new PinyinList(PinyinHelper.convertToPinyinArray(ch, PinyinFormat.WITHOUT_TONE)));
            } else {
                sb.append(ch);
            }
        }

        if (sb.length() != 0) list.add(sb.toString());

        Set<String> possibleCombines = new HashSet<>();

        boolean shouldContinue;
        do {
            shouldContinue = false;
            // output
            possibleCombines.add(outputList(list));

            for (int i = 0; i < list.size(); i++) {
                if (!(list.get(i) instanceof PinyinList)) continue;
                PinyinList pinyinList = (PinyinList) list.get(i);

                if (pinyinList.isLast()) {
                    pinyinList.reset();
                    boolean nextNonLastFound = false;
                    for (int j = i + 1; j < list.size(); j++) {
                        if (!(list.get(j) instanceof PinyinList)) continue;
                        PinyinList nextList = (PinyinList) list.get(j);
                        if (nextList.isLast()) {
                            nextList.reset();
                        } else {
                            nextList.next();
                            nextNonLastFound = true;
                            break;
                        }
                    }
                    if (nextNonLastFound) shouldContinue = true;
                } else {
                    pinyinList.next();
                    shouldContinue = true;
                }
                break;
            }
        } while (shouldContinue);

        return new ArrayList<>(possibleCombines);
    }

    private static boolean filterChar(char ch) {
        return Character.toString(ch).matches("[\u2e80-\ufe4f]");
    }

    private static String outputList(List<Object> list) {
        StringBuilder sb = new StringBuilder();
        for (Object obj : list) {
            if (obj instanceof PinyinList) {
                sb.append(((PinyinList) obj).get());
            } else {
                sb.append(obj.toString());
            }
        }
        return sb.toString();
    }

    private static class PinyinList {
        private final List<String> pinyinList = new ArrayList<>();
        private int index = 0;

        public PinyinList(String[] l) {
            Collections.addAll(pinyinList, l);
        }

        public boolean isLast() {
            return index + 1 >= pinyinList.size();
        }

        public void next() {
            index++;
        }

        public void reset() {
            index = 0;
        }

        public String get() {
            return pinyinList.get(index);
        }
    }
}
