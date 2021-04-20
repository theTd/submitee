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

class Submission {
    constructor(attributes) {
        this.attributeMap = new AttributeMap(attributes);
    }

    get uniqueId() {
        return this.attributeMap.get("unique-id");
    }

    get templateUUID() {
        return this.attributeMap.get("template-uuid");
    }

    get submitUser() {
        if (this.attributeMap.get("submit-user.realm-type") === "anonymous") return "匿名用户";
        return this.attributeMap.get("submit-user.realm-type") + ":" + this.attributeMap.get("submit-user.user-id");
    }

    get submitTime() {
        return this.attributeMap.get("submit-time");
    }

    get submitTimeRaw() {
        return this.attributeMap.get("submit-time");
    }

    get debug() {
        return this.attributeMap.get("debug");
    }

    get content() {
        return this.attributeMap.get("body");
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

    get literalState() {
        if (this.archived) {
            return "archived";
        } else if (this.published) {
            return "published";
        }
        return "editing";
    }

    get status() {
        if (this.archived) {
            return `<span class="template-status-archived">归档</span>`;
        }
        return this.published ?
            "<span class='template-status-published'>发布</span>" :
            "<span class='template-status-editing'>编辑</span>"
    }

    get published() {
        return this.attributeMap.get("published");
    }

    get archived() {
        return this.attributeMap.get("archived");
    }

    get submissionPageLink() {
        return submitee.getSubmissionPageLink(this.uniqueId);
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

async function fetchTemplateSize(filter, latest) {
    return new Promise((resolve, reject) => {
        $.ajax({
            url: "../batch-get/template/size",
            method: "POST",
            contentType: "application/json",
            data: JSON.stringify({
                latest: latest,
                filter: filter
            }),
            success: function (response) {
                resolve(parseInt(response));
            },
            error: function (xhr) {
                reject(xhr);
            }
        });
    });
}

async function fetchTemplateInfo(filter, latest, start, length, abbrev) {
    return new Promise((resolve, reject) => {
        $.ajax({
            url: "../batch-get/template",
            method: "POST",
            contentType: "application/json",
            data: JSON.stringify({
                latest: latest,
                filter: filter,
                start: start,
                length: length,
                abbrev: abbrev
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
            success: function (response) {
                resolve(new STemplate(response["body"]));
            },
            error: reject
        });
    });
}

async function fetchListedTemplateInfo(uuidList) {
    return new Promise((resolve, reject) => {
        $.ajax({
            url: "../batch-get/template",
            method: "POST",
            contentType: "application/json",
            data: JSON.stringify({list: uuidList}),
            success: function (response) {
                let map = {};
                for (let r of response) {
                    let body = r["body"];
                    map[body["uuid"]] = new STemplate(body);
                }
                resolve(map);
            },
            error: reject
        })
    })
}

function fetchSubmissionSize(filter) {
    return new Promise((resolve, reject) => {
        $.ajax({
            url: "../batch-get/submission/size",
            method: "POST",
            contentType: "application/json",
            data: JSON.stringify({filter: filter}),
            success: function (response) {
                resolve(parseInt(response))
            },
            error: reject
        });
    });
}

async function fetchSubmissionInfo(filter, start, length) {
    return new Promise((resolve, reject) => {
        $.ajax({
            url: "../batch-get/submission",
            method: "POST",
            contentType: "application/json",
            data: JSON.stringify({
                filter: filter,
                start: start,
                length: length
            }),
            success: function (data) {
                let all = Array();
                for (let val of data) {
                    all.push(new Submission(val['body']));
                }
                resolve(all);
            },
            error: reject
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

submitee.fieldControllers = {};

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

    generateReportHtml(field, value) {
        return new Promise(resolve => {
            if (!value) {
                resolve(`<span style="color: gray">&lt;空&gt;</span>`);
            }
            resolve(value)
        });
    }
}

submitee.initializedControllers = Array();

/**
 *
 * @param {Set} set
 */
function requireControllers(set) {
    let promises = Array();
    for (let type of set) {
        if (!submitee.initializedControllers.includes(type)) {
            submitee.initializedControllers.push(type);
            let controller = submitee.fieldControllers[type];
            promises.push(controller.submissionInit());
        }
    }
    return Promise.all(promises);
}


function getFieldTypeDisplayName(type) {
    let c = submitee.fieldControllers[type];
    return c ? c.displayName : type;
}

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

function findParentAttributeByElement(element, attribute) {
    let node = element;
    let name;
    while (node) {
        if (!node.hasAttribute) {
            node = node.parentNode;
            continue;
        }
        if (node.hasAttribute(attribute)) {
            name = node.getAttribute(attribute);
            break;
        }
        node = node.parentNode;
    }
    return name;
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

submitee.getFileMeta = async function (key) {
    return new Promise((resolve, reject) => {
        $.ajax({
            url: "../get-file/" + key + "/metadata",
            method: "GET",
            success: function (response) {
                resolve(response);
            },
            error: reject
        })
    })
}

submitee.getPage = function (url) {
    return new Promise(resolve => {
        $.ajax({
            url: url,
            method: "GET",
            success: resolve
        });
    });
}

submitee.getSubmissionPageLink = function (templateUniqueId) {
    let url = new URL(window.location.href);
    url.pathname = "/static/submission.html";
    url.search = "?target=" + templateUniqueId;
    return url.toString();
}

submitee.relativeTimeLocale = function (time) {
    let date = new Date(parseInt(time));
    let now = new Date();
    let result = '';

    // region year
    let yearOff = now.getFullYear() - date.getFullYear();
    if (Math.abs(yearOff) > 1) {
        return date.toLocaleString();
    } else {
        result += yearOff === 0 ? "" : (yearOff > 0 ? "去年" : "明年");
    }
    // endregion

    // region month
    let monthOff = now.getMonth() - date.getMonth();
    if (yearOff !== 0 || Math.abs(monthOff) > 1) {
        result += date.getMonth() + "月";
    } else {
        result += monthOff === 0 ? "" : (monthOff > 0 ? "上个月" : "下个月");
    }
    // endregion

    // region day
    let dayOff = now.getDate() - date.getDate();
    if (monthOff !== 0 || Math.abs(dayOff) > 1) {
        result += date.getDate() + "日";
    } else {
        result += dayOff === 0 ? "" : (dayOff > 0 ? "昨天" : "明天");
    }
    // endregion

    let pad = function (number) {
        if (number < 10) return '0' + number;
        return number;
    }

    // if (Math.abs(dayOff) > 1) return result;
    result += ` ${date.getHours()}:${pad(date.getMinutes())}`;
    if (date.getDate() === now.getDate() && date.getSeconds() !== 0) {
        result += ":" + pad(date.getSeconds());
    }
    return result;
}

submitee.escapeRegex = function (value) {
    return value.replace(/[\-\[\]{}()*+?.,\\^$|#\s]/g, "\\$&");
}

submitee.mailPattern = /^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9-]+(?:\.[a-zA-Z0-9-]+)*$/;
submitee.uuidPattern = /\b[0-9a-f]{8}\b-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-\b[0-9a-f]{12}\b/;
