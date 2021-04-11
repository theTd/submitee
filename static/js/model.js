class AttributeMap {
    constructor(root) {
        this.root = root || {};
    }

    get(path) {
        let paths = path.split(".");
        let node = this.root;
        for (let i = 0; i < paths.length; i++) {
            let nextPath = paths[i];
            if (node.hasOwnProperty(nextPath)) {
                node = node[nextPath];
            } else {
                return null;
            }
        }
        return node;
    }

    set(path, value) {
        let paths = path.split(",");
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

function getQueryVariable(name, queryString) {
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
            url: "../batch-get",
            method: "POST",
            contentType: "application/json",
            data: JSON.stringify({
                scheme: "STemplate",
                latest: latest,
                filter: filter
            }),
            success: function (data) {
                let all = Array();
                for (let val of data) {
                    all.push(new STemplate(val['attributes']));
                }
                resolve(all);
            },
            error: function (error) {
                reject(error.responseText);
            }
        });
    });
}

async function fetchSingleTemplateInfo(uuid) {
    return new Promise((resolve, reject) => {
        $.ajax({
            url: "../info/" + uuid,
            method: "GET",
            success: function (data) {
                resolve(new STemplate(data["attributes"]));
            },
            error: function (error) {
                reject(error.statusCode);
            }
        });
    });
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
