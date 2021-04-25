/**
 * https://stackoverflow.com/questions/1349404/generate-random-string-characters-in-javascript
 * @param length
 * @returns {string}
 */
function makeid(length) {
    let result = [];
    // noinspection SpellCheckingInspection
    let characters = 'abcdefghijklmnopqrstuvwxyz';
    let charactersLength = characters.length;
    for (let i = 0; i < length; i++) {
        result.push(characters.charAt(Math.floor(Math.random() *
            charactersLength)));
    }
    return result.join('');
}

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
 * @param {string[]} urlArray
 */
submitee_safe.loadAllScript = function (urlArray) {
    if (!submitee_safe.distinctLoadedScripts) submitee_safe.distinctLoadedScripts = {};
    if (!submitee_safe.loadedScripts) submitee_safe.loadedScripts = {}
    return new Promise(async resolve => {
        let cur = 0;
        for (let url of urlArray) {
            cur++;
            if (submitee_safe.loadedScripts[url]) {
                console.log(`skipping loaded script ${url} (${cur}/${urlArray.length})`);
                continue;
            }
            console.log(`loading script ${url} (${cur}/${urlArray.length})`);
            await submitee_safe.loadScriptPromise(url, null);
        }
        resolve();
    })
}

submitee_safe.loadStylesheet = function (url, callback) {
    if (!submitee_safe.loadedStylesheets) submitee_safe.loadedStylesheets = {}

    let element = document.createElement("link");
    element.rel = "stylesheet";
    element.href = url;
    element.addEventListener("load", callback);
    if (submitee_safe.loadedStylesheets[url]) {
        callback();
        return;
    }
    submitee_safe.loadedStylesheets[url] = '1';
    document.querySelector("head").appendChild(element);
}

submitee_safe.loadStylesheetPromise = function (url) {
    if (!submitee_safe.loadedStylesheets) submitee_safe.loadedStylesheets = {}
    return new Promise(function (resolve) {
        submitee_safe.loadStylesheet(url, resolve);
    });
}

submitee_safe.loadAllStylesheet = function (urlArray) {
    if (!submitee_safe.loadedStylesheets) submitee_safe.loadedStylesheets = {}
    return new Promise(async resolve => {
        let cur = 0;
        for (let url of urlArray) {
            cur++;
            if (submitee_safe.loadedStylesheets[url]) {
                console.log(`skipping loaded stylesheet ${url} (${cur}/${urlArray.length})`);
                continue;
            }
            console.log(`loading stylesheet ${url} (${cur}/${urlArray.length})`);
            await submitee_safe.loadStylesheetPromise(url, null);
        }
        resolve();
    })
}

submitee_safe.appendStyleSheet = function (content, distinct) {
    if (!submitee_safe.appendedStylesheets) submitee_safe.appendedStylesheets = {};
    if (distinct) {
        let id = submitee_safe.appendedStylesheets[distinct];
        if (id) document.getElementById(id).remove();
    }
    let id = makeid(6);
    let element = document.createElement("style");
    element.id = id;
    element.innerHTML = content;
    document.querySelector("head").appendChild(element);
}