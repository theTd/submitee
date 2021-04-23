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
    if (!submitee_safe.distinctLoadedScripts) submitee_safe.distinctLoadedScripts = {};
    if (!submitee_safe.loadedScripts) submitee_safe.loadedScripts = {}
    return new Promise(function (resolve) {
        submitee_safe.loadScript(url, distinct, resolve);
    });
}

/**
 *
 * @param {string[]} scripts
 */
submitee_safe.loadAllScript = function (scripts) {
    if (!submitee_safe.distinctLoadedScripts) submitee_safe.distinctLoadedScripts = {};
    if (!submitee_safe.loadedScripts) submitee_safe.loadedScripts = {}
    return new Promise(async resolve => {
        let cur = 0;
        for (let url of scripts) {
            cur++;
            if (submitee_safe.loadedScripts[url]) continue;
            console.log(`loading script ${url} (${cur}/${scripts.length})`);
            await submitee_safe.loadScriptPromise(url, null);
        }
        resolve();
    })
}
