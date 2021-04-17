package org.starrel.submitee.http;

import com.google.gson.JsonObject;
import jakarta.servlet.AsyncContext;
import org.eclipse.jetty.http.HttpStatus;
import org.starrel.submitee.*;
import org.starrel.submitee.blob.BlobStorage;
import org.starrel.submitee.blob.BlobStorageProvider;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.starrel.submitee.model.User;
import org.starrel.submitee.model.UserRealm;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class ConfigurationServlet extends AbstractJsonServlet {

    {
        setBaseUri("/configuration");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = getSession(req).getUser();
        if (!user.isSuperuser()) {
            responseAccessDenied(req, resp);
            return;
        }

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
                blobStorageErrors.put(storage.getName(), I18N.fromKey(String.format("blob_storage.provider.%s.error.%s",
                        storage.getTypeId(), e.getDistinguishName().toLowerCase(Locale.ROOT))).format(req));
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

        // region smtp settings
        configurationMap.put("smtp", SubmiteeServer.getInstance().getAttributeMap().of("smtp").toJsonTree());
        // endregion

        configurationMap.put("grecaptcha-sitekey", SubmiteeServer.getInstance().getAttribute("grecaptcha-sitekey", String.class));
        configurationMap.put("grecaptcha-secretkey", SubmiteeServer.getInstance().getAttribute("grecaptcha-secretkey", String.class));

        // region user realms
        Map<String, String> userRealms = new LinkedHashMap<>();
        for (UserRealm realm : SubmiteeServer.getInstance().getUserRealms()) {
            userRealms.put(realm.getTypeId(), I18N.fromKey(String.format("user_realm.%s.title", realm.getTypeId())).format(req));
        }
        // endregion
        configurationMap.put("user-realms", userRealms);

        resp.setStatus(HttpStatus.OK_200);
        resp.setContentType("application/json");
        resp.getWriter().println(SubmiteeServer.GSON.toJson(configurationMap));
        resp.getWriter().close();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp, JsonObject body) throws ServletException, IOException {
        User user = getSession(req).getUser();
        if (!user.isSuperuser()) {
            responseAccessDenied(req, resp);
            return;
        }

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
                    responseClassifiedError(req, resp, ClassifiedErrors.MISSING_PARAMETER,
                            I18N.fromKey("blob_storage.parameter.name").format(req));
                    return;
                }
                if (provider == null || provider.isEmpty()) {
                    responseClassifiedError(req, resp, ClassifiedErrors.MISSING_PARAMETER,
                            I18N.fromKey("blob_storage.parameter.provider").format(req));
                    return;
                }

                try {
                    SubmiteeServer.getInstance().getBlobStorageController().createStorage(provider, name);
                    resp.setStatus(HttpStatus.OK_200);
                } catch (ClassifiedException e) {
                    responseClassifiedError(req, resp, e.getClassifiedError());
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
                SubmiteeServer.getInstance().pushEvent(Level.INFO, ConfigurationServlet.class,
                        "setup blob storage", String.format("target=%s, config=%s", setup.getTypeId() + ":" + setup.getName(),
                                SubmiteeServer.GSON.toJson(body.get("config"))));

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

                SubmiteeServer.getInstance().pushEvent(Level.INFO, ConfigurationServlet.class,
                        "register toggle settings updated", String.format("enabled=%b, message=%s", enabled, disableMessage));
                resp.setStatus(HttpStatus.OK_200);
                break;
            }
            case "smtp-settings": {
                SubmiteeServer.getInstance().getAttributeMap().setAll("smtp", body);
                SubmiteeServer.getInstance().getAttributeMap().save();

                SubmiteeServer.getInstance().pushEvent(Level.INFO, ConfigurationServlet.class,
                        "smtp settings updated", "settings=" + SubmiteeServer.GSON.toJson(body));
                resp.setStatus(HttpStatus.OK_200);
                break;
            }
            case "send-test-mail": {
                AsyncContext asyncContext = req.startAsync();
                asyncContext.start(() -> {
                    try {
                        Util.sendNotificationEmail(body.get("addr").getAsString(),
                                "SUBMITEE测试邮件", "这是一封测试邮件，如果能够收到此邮件，则邮件发送配置有效", null).get();

                        SubmiteeServer.getInstance().pushEvent(Level.INFO, ConfigurationServlet.class,
                                "send test email", "target=" + body.get("addr"));
                        resp.setStatus(HttpStatus.OK_200);
                    } catch (InterruptedException ignored) {
                    } catch (ExecutionException e) {
                        ExceptionReporting.report(ConfigurationServlet.class, "sending mail", e);
                        responseInternalError(req, resp);
                    } finally {
                        asyncContext.complete();
                    }
                });
                break;
            }
            case "grecaptcha": {
                SubmiteeServer.getInstance().setAttribute("grecaptcha-sitekey", body.get("sitekey").getAsString());
                SubmiteeServer.getInstance().setAttribute("grecaptcha-secretkey", body.get("secretkey").getAsString());

                SubmiteeServer.getInstance().pushEvent(Level.INFO, ConfigurationServlet.class,
                        "grecaptcha settings updated", String.format("sitekey=%s, secretkey=%s",
                                body.get("sitekey").getAsString(),
                                body.get("secretkey").getAsString()));
                resp.setStatus(HttpStatus.OK_200);
                break;
            }
            case "test-grecaptcha": {
                String token = body.get("token").getAsString();
                AsyncContext asyncContext = req.startAsync();
                asyncContext.start(() -> {
                    try {
                        Util.grecaptchaVerify(token, Util.getRemoteAddr(req),
                                SubmiteeServer.getInstance().getAttribute("grecaptcha-secretkey", String.class));
                        resp.setStatus(HttpStatus.OK_200);
                    } catch (ClassifiedException e) {
                        ExceptionReporting.report(ConfigurationServlet.class, "testing grecaptcha", e);
                        responseClassifiedException(req, resp, e);
                    } finally {
                        asyncContext.complete();
                    }
                });
                break;
            }
            default: {
                ExceptionReporting.report(ConfigurationServlet.class, "unknown configuration method", uriParts[0]);
                responseBadRequest(req, resp);
            }
        }
    }
}
