package org.starrel.submitee;

import org.eclipse.jetty.http.HttpStatus;

import static org.starrel.submitee.ClassifiedError.create;

public abstract class ClassifiedErrors {
    public final static ClassifiedError INTERNAL_SERVER_ERROR = create(
            "INTERNAL_SERVER_ERROR", HttpStatus.INTERNAL_SERVER_ERROR_500, I18N.Http.INTERNAL_ERROR);

    public final static ClassifiedError NOT_FOUND = create(
            "NOT_FOUND", HttpStatus.NOT_FOUND_404, I18N.Http.NOT_FOUND);

    public final static ClassifiedError ACCESS_DENIED = create(
            "ACCESS_DENIED", HttpStatus.UNAUTHORIZED_401, I18N.Http.ACCESS_DENIED);

    public final static ClassifiedError BAD_REQUEST = create(
            "BAD_REQUEST", HttpStatus.BAD_REQUEST_400, I18N.Http.INVALID_INPUT);

    public final static ClassifiedError MISSING_PARAMETER = create(
            "MISSING_PARAMETER", HttpStatus.BAD_REQUEST_400, I18N.General.MISSING_PARAMETER,
            String.class);

    public final static ClassifiedError NAME_CONFLICT = create(
            "NAME_CONFLICT", HttpStatus.CONFLICT_409, I18N.General.NAME_CONFLICT,
            String.class, String.class);

    public final static ClassifiedError REQUIRE_CAPTCHA = create(
            "REQUIRE_CAPTCHA", HttpStatus.FORBIDDEN_403, I18N.General.REQUIRE_CAPTCHA);

    public final static ClassifiedError CAPTCHA_FAILURE = create(
            "CAPTCHA_FAILURE", HttpStatus.FORBIDDEN_403, I18N.General.CAPTCHA_FAILURE);

    public final static ClassifiedError USER_EXISTS_EMAIL = create(
            "USER_EXISTS_EMAIL", HttpStatus.FORBIDDEN_403, I18N.General.USER_EXISTS_EMAIL);

    public final static ClassifiedError USER_NOT_EXIST = create(
            "USER_NOT_EXIST", HttpStatus.NOT_FOUND_404, I18N.General.USER_NOT_EXIST);

    public final static ClassifiedError TOO_MANY_REQUEST = create(
            "TOO_MANY_REQUEST", HttpStatus.TOO_MANY_REQUESTS_429, I18N.Http.TOO_MANY_REQUEST);

    public final static ClassifiedError VERIFY_CODE_MISMATCH = create(
            "VERIFY_CODE_MISMATCH", HttpStatus.FORBIDDEN_403, I18N.General.VERIFY_CODE_MISMATCH);

    public final static ClassifiedError SUBMIT_TO_OLD_TEMPLATE = create(
            "SUBMIT_TO_OLD_TEMPLATE", HttpStatus.FORBIDDEN_403, I18N.General.SUBMIT_TO_OLD_TEMPLATE);
}
