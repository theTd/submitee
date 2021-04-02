class PropertyMap {
    constructor() {
        this.root = {};
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
        if (!this.propertyMap) this.propertyMap = new PropertyMap();
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
    constructor(uniqueId, propertyMap) {
        this.uniqueId = uniqueId;
        this.propertyMap = propertyMap;
        this.fields = {};
        if (!this.propertyMap) this.propertyMap = new PropertyMap();
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
