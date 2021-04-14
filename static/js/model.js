submitee = {};

class AttributeMap {
    constructor(root) {
        this.root = root || {};
    }

    get(path, def) {
        let paths = path.split(".");
        let node = this.root;
        for (let i = 0; i < paths.length; i++) {
            let nextPath = paths[i];
            if (node.hasOwnProperty(nextPath)) {
                node = node[nextPath];
            } else {
                return def;
            }
        }
        return node;
    }

    set(path, value) {
        let paths = path.split(".");
        let node = this.root;
        for (let i = 0; i < paths.length; i++) {
            let nextPath = paths[i];
            if (i === paths.length - 1) {
                // last node
                node[nextPath] = value;
            }
            if (node.hasOwnProperty(nextPath)) {
                node = node[nextPath];
            } else {
                node[nextPath] = {};
                node = node[nextPath];
            }
        }
    }
}

class SField {
    /**
     *
     * @param {STemplate} owner
     * @param attributes
     */
    constructor(owner, attributes) {
        this.owner = owner;
        this.attributeMap = attributes ? new AttributeMap(attributes) : new AttributeMap();
    }

    get type() {
        return this.attributeMap.get("type");
    }

    set type(val) {
        this.attributeMap.set("type", val);
    }

    getTypeDisplayName() {
        return getFieldTypeDisplayName(this.type);
    }

    get name() {
        return this.attributeMap.get("name");
    }

    set name(val) {
        this.attributeMap.set("name", val);
    }

    get comment() {
        return this.attributeMap.get("comment") || "";
    }

    set comment(val) {
        this.attributeMap.set("comment", val);
    }

    get required() {
        return this.attributeMap.get("required") || false;
    }

    set required(val) {
        this.attributeMap.set("required", !!val);
    }

    async sync() {
        return this.owner.sync();
    }
}

class STemplate {
    constructor(attributes) {
        this.attributeMap = new AttributeMap(attributes);

        let t = this;

        let fieldsSection = this.attributeMap.get("fields");
        let fields = Array();
        if (fieldsSection) {
            fieldsSection.forEach(function (val) {
                let f = new SField(t, val);
                fields.push(f);
            })
        }

        this.fields = fields;
    }

    get uniqueId() {
        return this.attributeMap.get("uuid");
    }

    get templateId() {
        return this.attributeMap.get("template-id");
    }

    get version() {
        return this.attributeMap.get("version");
    }

    get name() {
        return this.attributeMap.get("name");
    }

    set name(val) {
        this.attributeMap.set("name", val);
    }

    get comment() {
        return this.attributeMap.get("comment");
    }

    set comment(val) {
        this.attributeMap.set("comment", val);
    }

    get desc() {
        return this.attributeMap.get("desc");
    }

    set desc(val) {
        this.attributeMap.set("desc", val);
    }

    get status() {
        return this.published ? "<span class='text-success'>发布</span>" : "<span class='text-info'>编辑</span>"
    }

    get published() {
        return this.attributeMap.get("published");
    }

    /**
     *
     * @param {SField} field
     */
    addField(field) {
        let names = Array();
        this.fields.forEach(val => {
            names.push(val.name);
        });

        if (names.includes(field.name)) {
            throw new Error("name conflict");
        }
        this.fields.push(field);
        return this.sync();
    }

    removeField(field) {
        this.fields = this.fields.filter(value => value !== field);
        return this.sync();
    }

    getFieldByName(name) {
        for (let value of this.fields) {
            if (value.name === name) return value;
        }
    }

    moveField(field, index) {
        let originalIndex = this.findFieldIndex(field);
        if (originalIndex === index) return new Promise(resolve => resolve());

        if (index > originalIndex) index--;

        this.fields = this.fields.filter(value => value !== field);
        this.fields.splice(index, 0, field);
        return this.sync();
    }

    findFieldIndex(field) {
        for (let i = 0; i < this.fields.length; i++) {
            if (this.fields[i] === field) {
                return i;
            }
        }
    }

    updateAttributeMap() {
        let fieldsSection = Array();
        this.fields.forEach(function (val) {
            fieldsSection.push(val.attributeMap.root);
        });
        this.attributeMap.set("fields", fieldsSection);
    }

    async sync() {
        this.updateAttributeMap();
        return new Promise((resolve, reject) => {
            console.log(JSON.stringify({
                "target": this.uniqueId,
                "content": this.attributeMap.root
            }));

            $.ajax({
                url: "../paste",
                method: "POST",
                contentType: "application/json",
                data: JSON.stringify({
                    "target": this.uniqueId,
                    "content": this.attributeMap.root
                }),
                success: function (data) {
                    resolve();
                },
                error: function (error) {
                    reject(getMessageFromAjaxError(error));
                }
            })
        })
    }
}

function getQueryValue(name, queryString) {
    queryString = queryString || window.location.search;

    let query = queryString.substring(1);
    let vars = query.split('&');
    for (let i = 0; i < vars.length; i++) {
        const pair = vars[i].split('=');
        if (decodeURIComponent(pair[0]) === name) {
            return decodeURIComponent(pair[1]);
        }
    }
    return null;
}

async function fetchTemplateInfo(filter, latest) {
    return new Promise((resolve, reject) => {
        $.ajax({
            url: "../batch-get/template",
            method: "POST",
            contentType: "application/json",
            data: JSON.stringify({
                latest: false,
                filter: filter
            }),
            success: function (data) {
                let all = Array();
                for (let val of data) {
                    all.push(new STemplate(val['body']));
                }
                resolve(all);
            },
            error: function (xhr) {
                reject(xhr);
            }
        });
    });
}

/**
 *
 * @param uuid
 * @returns {Promise<STemplate>}
 */
async function fetchSingleTemplateInfo(uuid) {
    return new Promise((resolve, reject) => {
        $.ajax({
            url: "../info/" + uuid,
            method: "GET",
            success: function (data) {
                resolve(new STemplate(data["body"]));
            },
            error: function (error) {
                reject(error);
            }
        });
    });
}

/**
 *
 * @param uuid
 * @param callback
 * @param errorCallback
 */
function setCurrentTemplate(uuid, callback, errorCallback) {
    fetchSingleTemplateInfo(uuid).then(template => {
        submitee.currentTemplate = template;
        callback(template);
    }, errorCallback);
}

fieldControllers = {};

class FieldController {
    constructor(fieldType) {
        this.fieldType = fieldType;
        this.displayName = fieldType;
    }

    resolveSubmission(field) {

    }

    async submissionInit() {
    }

    generateSubmissionHtml(field) {
        return `<div class="alert alert-warning">存在问题的字段: ${field.name}</div>`
    }

    generateConfigurationHtml(field) {

    }

    applyConfiguration(field) {

    }

    /**
     * @returns {string} any exception
     * @param {SField} field
     */
    validateConfiguration(field) {
    }

    getContainerId(field) {
        return "field-container-for-" + field.name;
    }
}

function getFieldTypeDisplayName(type) {
    let c = fieldControllers[type];
    return c ? c.displayName : type;
}

/**
 * https://stackoverflow.com/questions/1349404/generate-random-string-characters-in-javascript
 * @param length
 * @returns {string}
 */
function makeid(length) {
    var result = [];
    var characters = 'abcdefghijklmnopqrstuvwxyz';
    var charactersLength = characters.length;
    for (var i = 0; i < length; i++) {
        result.push(characters.charAt(Math.floor(Math.random() *
            charactersLength)));
    }
    return result.join('');
}

function findParentAttributeByElement(element, attribute) {
    let node = element;
    let name;
    while (node) {
        if (node.hasAttribute(attribute)) {
            name = node.getAttribute(attribute);
            break;
        }
        node = node.parentNode;
    }
    return name;
}

submitee.loadScript = function (url, distinct, callback) {
    if (!submitee.distinctLoadedScripts) submitee.distinctLoadedScripts = {};
    if (!submitee.loadedScripts) submitee.loadedScripts = {}

    let element = document.createElement("script");
    element.src = url;
    element.addEventListener("load", callback);

    if (distinct) {
        let loadedId = submitee.distinctLoadedScripts[distinct];
        if (loadedId) {
            let s = document.getElementById(loadedId);
            s.parentNode.removeChild(s);
        }
        element.id = makeid(6);
        submitee.distinctLoadedScripts[distinct] = element.id;
    } else {
        if (submitee.loadedScripts[url]) {
            callback();
            return;
        }
        submitee.loadedScripts[url] = '1';
    }
    document.querySelector("body").appendChild(element);
}

submitee.loadScriptPromise = function (url, distinct) {
    return new Promise(function (resolve) {
        submitee.loadScript(url, distinct, resolve);
    });
}

/**
 *
 * @param {string[]} scripts
 */
submitee.loadAllScript = async function (scripts) {
    return new Promise(async resolve => {
        for (let url of scripts) {
            await submitee.loadScriptPromise(url, null);
        }
        resolve();
    })
}

function beforeUnloadCheck() {
    if (submitee.beforeUnloadChecker) {
        return submitee.beforeUnloadChecker();
    } else {
        return undefined;
    }
}

class ScheduleBroker {
    constructor(initialState) {
        this.nextTask = null;
        this.nextState = null;

        this.pendingState = null;
        this.state = initialState;
    }

    submitState(task, state) {
        if (this.pendingState === state) return;
        this.nextTask = task;
        this.nextState = state;

        this._nextTask();
    }

    setState(state) {
        if (this.pendingState === state) {
            this.state = state;
            this.pendingState = null;
            this._nextTask();
        }
    }

    _nextTask() {
        if (this.pendingState || !this.nextTask) return
        if (this.state !== this.nextState) {
            let task = this.nextTask;
            this.pendingState = this.nextState;
            this.nextTask = null;
            this.nextState = null;
            setTimeout(task);
        }
    }
}

submitee.mailPattern = /^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9-]+(?:\.[a-zA-Z0-9-]+)*$/;
