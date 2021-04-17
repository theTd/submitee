if (!window.hasOwnProperty("submitee_safe")) {
    submitee_safe = {}
}

submitee_safe.loadScript = function (url, distinct, callback) {
    if (!submitee_safe.distinctLoadedScripts) submitee_safe.distinctLoadedScripts = {};
    if (!submitee_safe.loadedScripts) submitee_safe.loadedScripts = {}

    let element = document.createElement("script");
    element.src = url;
    element.addEventListener("load", callback);

    if (distinct) {
        let loadedId = submitee_safe.distinctLoadedScripts[distinct];
        if (loadedId) {
            let s = document.getElementById(loadedId);
            s.parentNode.removeChild(s);
        }
        element.id = makeid(6);
        submitee_safe.distinctLoadedScripts[distinct] = element.id;
    } else {
        if (submitee_safe.loadedScripts[url]) {
            callback();
            return;
        }
        submitee_safe.loadedScripts[url] = '1';
    }
    document.querySelector("body").appendChild(element);
}

submitee_safe.loadScriptPromise = function (url, distinct) {
    return new Promise(function (resolve) {
        submitee_safe.loadScript(url, distinct, resolve);
    });
}

/**
 *
 * @param {string[]} scripts
 */
submitee_safe.loadAllScript = async function (scripts) {
    return new Promise(async resolve => {
        for (let url of scripts) {
            await submitee_safe.loadScriptPromise(url, null);
        }
        resolve();
    })
}
