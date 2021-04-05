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
    constructor(uniqueId, propertyMap) {
        this.uniqueId = uniqueId;
        this.propertyMap = propertyMap;
        if (!this.propertyMap) this.propertyMap = new AttributeMap();
    }

    get type() {
        return this.propertyMap.get("type");
    }

    set type(val) {
        this.propertyMap.set("type", val);
    }

    get name() {
        return this.propertyMap.get("name");
    }

    set name(val) {
        this.propertyMap.set("name", val);
    }

    get comment() {
        return this.propertyMap.get("comment");
    }

    set comment(val) {
        this.propertyMap.set("comment", val);
    }
}

class STemplate {
    constructor(uniqueId, attributes) {
        this.uniqueId = uniqueId;
        this.attributeMap = new AttributeMap(attributes);
        this.fields = {};
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

    /**
     *
     * @param {SField} field
     */
    addField(field) {
        this.fields[field.uniqueId] = field;
    }

    /**
     *
     * @param uniqueId
     * @return {SField}
     */
    getField(uniqueId) {
        return this.fields[uniqueId];
    }

    getAllFields() {
        let arr = Array();
        for (let key in this.fields) {
            if (this.fields.hasOwnProperty(key)) arr.push(this.fields[key]);
        }
        return arr;
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
                    all.push(new STemplate(val['uniqueId'], val['attributes']));
                }
                resolve(all);
            },
            error: function (error) {
                reject(error.responseText);
            }
        });
    });
}

fieldControllers = {};

class FieldController {
    constructor(fieldType) {
        this.fieldType = fieldType;
    }

    generateResolveFunction(field) {

    }

    generateHtml(field) {

    }

    validateResolveResult(field, resolveResult) {
    }
}

class TextFieldController extends FieldController {
    constructor() {
        super("text");
    }

    generateResolveFunction(field) {
        let inputId = 'text-input-for-' + field.uniqueId;
        return function () {
            return $("#" + inputId).val();
        }
    }

    /**
     *
     * @param {SField} field
     * @returns {string}
     */
    generateHtml(field) {
        let inputId = 'text-input-for-' + field.uniqueId;
        let placeholder = field.propertyMap.get("placeholder") || "";

        return `<label for="${inputId}">${field.name}</label><input type="text" placeholder="${placeholder}" id="${inputId}"/>`;
    }

    /**
     *
     * @param {SField} field
     * @param resolveResult
     */
    validateResolveResult(field, resolveResult) {
        let constraints = field.propertyMap.get("constraints");
        if (constraints) {

            for (let i = 0; i < constraints.length; i++) {
                let c = constraints[i];
                c();
            }
        }
    }
}

fieldControllers['text'] = new TextFieldController();
