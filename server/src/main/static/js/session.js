submitee.session = {};

submitee.closeSession = function () {
    $.ajax({
        url: "../session/close",
        method: "POST",
        contentType: "application/json",
        success: function (response) {
            submitee.session = {};
            if (submitee.sessionHeaderId) {
                create_toast("提示", "已完成登出");
                createSessionHeader(submitee.sessionHeaderId);
            }
        },
        error: function (xhr) {
            toast_ajax_error(xhr)
        }
    });
}

function fetchSessionInfo() {
    return new Promise((resolve, reject) => {
        $.ajax({
            url: "../session",
            method: "GET",
            success: function (response) {
                submitee.session.realm = response["realm"];
                submitee.session.id = response["id"];
                submitee.session.profile = response["profile"];
                submitee.grecaptchaSitekey = response["grecaptcha-sitekey"];
                resolve(response);
            },
            error: reject
        });
    })
}

function promiseSessionInfo() {
    if (submitee.pendingSession) {
        return submitee.pendingSession;
    }
    return submitee.pendingSession = new Promise((resolve, reject) => {
        fetchSessionInfo().then(resolve, reject).finally(() => {
            delete submitee.pendingSession;
        })
    });
}

/**
 *
 * @param {string} containerId
 */
function createSessionHeader(containerId) {
    submitee.sessionHeaderId = containerId;
    let container = $("#" + containerId);
    let run = () => {
        $.ajax({
            url: "session-header.html",
            method: 'GET',
            success: function (response) {
                container.html(response);
            },
            error: toast_ajax_error
        });
    }

    if (!submitee.session.realm) {
        promiseSessionInfo().then(run);
    } else {
        run();
    }
}

function setAuthTarget(authTarget) {
    submitee.authTarget = authTarget;
}

function sendToAuthPage() {
    if (submitee.authTarget) {
        let url = new URL(window.location.href);
        let redirect = url.pathname + url.search;
        window.location.href = `auth.html?d=${btoa(redirect)}&t=${submitee.authTarget}`;
    }
}
