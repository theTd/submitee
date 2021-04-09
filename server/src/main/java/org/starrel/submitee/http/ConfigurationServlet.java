package org.starrel.submitee.http;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import org.eclipse.jetty.http.HttpStatus;
import org.starrel.submitee.ExceptionReporting;
import org.starrel.submitee.I18N;
import org.starrel.submitee.SubmiteeServer;
import org.starrel.submitee.blob.BlobStorage;
import org.starrel.submitee.blob.BlobStorageProvider;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConfigurationServlet extends AbstractJsonServlet {

    {
        setBaseUri("/configuration");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Map<String, Object> configurationMap = new LinkedHashMap<>();

        List<String> blobStorageProviders = SubmiteeServer.getInstance().getBlobStorageController().getProviders().stream()
                .map(BlobStorageProvider::getTypeId).collect(Collectors.toList());
        Map<String, String> providerNameMap = new LinkedHashMap<>();
        for (String provider : blobStorageProviders) {
            providerNameMap.put(provider, I18N.fromKey("blob_storage.provider." + provider).format(req));
        }
        configurationMap.put("blob_storage_providers", providerNameMap);

        List<JsonObject> blobStorages = new ArrayList<>();
        SubmiteeServer.getInstance().getBlobStorageController().getStorages().forEach(blobStorage -> {
            blobStorages.add(blobStorage.getAttributeMap().toJsonTree());
        });
        configurationMap.put("blob_storages", blobStorages);

        resp.setStatus(HttpStatus.OK_200);
        resp.setContentType("application/json");
        resp.getWriter().println(SubmiteeServer.GSON.toJson(configurationMap));
        resp.getWriter().close();
    }

    @Override
    protected void request(HttpServletRequest req, HttpServletResponse resp, JsonObject body) throws ServletException, IOException {
        String[] uriParts = parseUri(req.getRequestURI());
        if (uriParts.length < 1) {
            ExceptionReporting.report(ConfigurationServlet.class, "parsing configuration method",
                    "unexpected uri:" + req.getRequestURI());
            responseBadRequest(req, resp);
            return;
        }

        switch (uriParts[0]) {
            case "create-blob-storage": {
                String provider = body.has("provider") ? body.get("provider").getAsString() : null;
                String name = body.has("name") ? body.get("name").getAsString() : null;
                if (name == null || name.isEmpty()) {
                    responseBadRequest(req, resp, I18N.General.MISSING_PARAMETER,
                            I18N.fromKey("blob_storage.parameter.name"));
                    return;
                }
                if (provider == null || provider.isEmpty()) {
                    responseBadRequest(req, resp, I18N.General.MISSING_PARAMETER,
                            I18N.fromKey("blob_storage.parameter.provider"));
                    return;
                }

                try {
                    SubmiteeServer.getInstance().getBlobStorageController().createStorage(provider, name);
                } catch (Exception e) {
                    ExceptionReporting.report(ConfigurationServlet.class, "creating blob storage", e);
                    responseInternalError(req, resp);
                    return;
                }
                resp.setStatus(HttpStatus.OK_200);
                break;
            }
            case "setup-blob-storage": {
                String provider = body.get("provider").getAsString();
                String name = body.get("name").getAsString();
                List<? extends BlobStorage> match = SubmiteeServer.getInstance().getBlobStorageController().getStorages().stream()
                        .filter(s -> s.getName().equals(name) && s.getTypeId().equals(provider)).collect(Collectors.toList());
                if (match.isEmpty()) {
                    responseNotFound(req, resp);
                    return;
                }
                match.iterator().next().getAttributeMap().set("", body.get("body").getAsJsonObject());
                resp.setStatus(HttpStatus.OK_200);
                break;
            }
            default: {
                ExceptionReporting.report(ConfigurationServlet.class, "unknown configuration method", uriParts[0]);
                responseBadRequest(req, resp);
            }
        }
    }
}
