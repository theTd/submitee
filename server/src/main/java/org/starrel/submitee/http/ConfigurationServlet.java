package org.starrel.submitee.http;

import com.google.gson.JsonObject;
import org.eclipse.jetty.http.HttpStatus;
import org.starrel.submitee.ClassifiedException;
import org.starrel.submitee.ExceptionReporting;
import org.starrel.submitee.I18N;
import org.starrel.submitee.SubmiteeServer;
import org.starrel.submitee.blob.BlobStorage;
import org.starrel.submitee.blob.BlobStorageProvider;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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

        // region providers
        List<String> blobStorageProviders = SubmiteeServer.getInstance().getBlobStorageController().getProviders().stream()
                .map(BlobStorageProvider::getTypeId).collect(Collectors.toList());
        Map<String, String> providerNameMap = new LinkedHashMap<>();
        for (String provider : blobStorageProviders) {
            providerNameMap.put(provider, I18N.fromKey("blob_storage.provider." + provider).format(req));
        }
        configurationMap.put("blob_storage_providers", providerNameMap);
        // endregion

        // region storages
        Map<String, Object> blobStorages = new LinkedHashMap<>();
        SubmiteeServer.getInstance().getBlobStorageController().getStorages().forEach(blobStorage ->
                blobStorages.put(blobStorage.getName(), blobStorage.getAttributeMap().toJsonTree()));
        configurationMap.put("blob_storages", blobStorages);
        // endregion

        // region storage config translation
        Map<String, Map<String, String>> blobStorageConfigTranslations = new LinkedHashMap<>();
        SubmiteeServer.getInstance().getBlobStorageController().getStorages().forEach(blobStorage -> {
            Map<String, String> translationMap = new LinkedHashMap<>();
            List<String> configKeys = blobStorage.getAttributeMap().getKeys("config");
            for (String configKey : configKeys) {
                translationMap.put(configKey, I18N.fromKey(String.format("blob_storage.provider.%s.config.%s",
                        blobStorage.getTypeId(), configKey)).format(req));
            }
            blobStorageConfigTranslations.put(blobStorage.getName(), translationMap);
        });
        configurationMap.put("blob_storage_config_translations", blobStorageConfigTranslations);
        // endregion

        // region storage errors
        Map<String, String> blobStorageErrors = new LinkedHashMap<>();
        for (BlobStorage storage : SubmiteeServer.getInstance().getBlobStorageController().getStorages()) {
            try {
                storage.validateConfiguration();
            } catch (ClassifiedException e) {
                blobStorageErrors.put(storage.getName(),
                        I18N.fromKey(String.format("blob_storage.provider.%s.error.%s",
                                storage.getTypeId(), e.getClassify())).format(req));
            } catch (Exception e) {
                blobStorageErrors.put(storage.getName(), e.getMessage());
            }
        }
        configurationMap.put("blob_storage_errors", blobStorageErrors);
        // endregion

        // region register toggle
        configurationMap.put("register-enabled",
                SubmiteeServer.getInstance().getAttribute("register-enabled", Boolean.class, true));
        configurationMap.put("register-disable-message",
                SubmiteeServer.getInstance().getAttribute("register-disable-message", String.class));
        // endregion

        resp.setStatus(HttpStatus.OK_200);
        resp.setContentType("application/json");
        resp.getWriter().println(SubmiteeServer.GSON.toJson(configurationMap));
        resp.getWriter().close();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp, JsonObject body) throws ServletException, IOException {
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
                    resp.setStatus(HttpStatus.OK_200);
                } catch (ClassifiedException e) {
                    responseBadRequest(req, resp, I18N.General.NAME_CONFLICT,
                            I18N.fromKey("blob_storage"), name);
                    return;
                } catch (Exception e) {
                    ExceptionReporting.report(ConfigurationServlet.class, "creating blob storage", e);
                    responseInternalError(req, resp);
                    return;
                }
                break;
            }
            case "setup-blob-storage": {
                String name = body.get("name").getAsString();
                BlobStorage setup = SubmiteeServer.getInstance().getBlobStorageController().getStorage(name);
                if (setup == null) {
                    responseNotFound(req, resp);
                    return;
                }
                setup.getAttributeMap().set("config", body.get("config"));

                resp.setStatus(HttpStatus.OK_200);
                try {
                    setup.validateConfiguration();
                } catch (Exception e) {
                    ExceptionReporting.report(ConfigurationServlet.class, "validating configuration", e);
                }
                break;
            }
            case "register-toggle": {
                boolean enabled = body.get("register-enabled").getAsBoolean();
                String disableMessage = body.get("register-disable-message").getAsString();
                SubmiteeServer.getInstance().setAttribute("register-enabled", enabled);
                SubmiteeServer.getInstance().setAttribute("register-disable-message", disableMessage);
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
