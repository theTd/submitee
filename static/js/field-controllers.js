class TextFieldController extends FieldController {
    constructor() {
        super("text");
        this.displayName = "文本";
    }

    resolveSubmission(field) {
        let containerId = this.getContainerId(field);
        let inputId = `${containerId}-${field.name}`;
        return $("#" + inputId).val();
    }

    /**
     *
     * @param {SField} field
     * @returns {string}
     */
    generateSubmissionHtml(field) {
        let containerId = this.getContainerId(field);
        let inputId = `${containerId}-${field.name}`;
        let placeholder = field.attributeMap.get("placeholder");
        if (!placeholder) placeholder = "";
        return `<input type="text" placeholder="${placeholder}" id="${inputId}"/>`;
    }

    /**
     *
     * @param {SField} field
     * @param resolveResult
     */
    validateResolveResult(field, resolveResult) {
        let constraints = field.attributeMap.get("constraints");
        if (constraints) {

            for (let i = 0; i < constraints.length; i++) {
                let c = constraints[i];
                new Function(c)();
            }
        }
    }
}

fieldControllers['text'] = new TextFieldController();

class RadioFieldController extends FieldController {
    constructor() {
        super("radio");
        this.displayName = "单选";
    }

    /**
     *
     * @param {SField} field
     * @returns {string}
     */
    resolveSubmission(field) {
        let containerId = super.getContainerId(field);
        return $(`#${containerId} input[name=${field.name}]:checked`).val();
    }

    /**
     *
     * @param {SField} field
     * @returns {string}
     */
    generateSubmissionHtml(field) {
        let values = field.attributeMap.get("values");

        if (!values) {
            return `<div class="alert alert-warning">存在问题的字段: ${field.name}</div>`;
        }

        let radioList = "";
        let containerId = this.getContainerId(field);
        values.forEach(function (val) {
            let radioId = `${containerId}-${val}`;
            radioList += `<input id="${radioId}" type="radio" name="${field.name}" value="${val}" /><label class="ml-1" for="${radioId}">${val}</label><div class="w-100"></div>`;
        })
        return radioList;
    }

    generateConfigurationHtml(field) {
        let id = "radio-conf-" + field.name;

        let values = field.attributeMap.get("values");
        let present = "";
        if (values) {
            values.forEach(function (val) {
                present += val;
                present += ",";
            })
        }

        return `
<div class="container">
<label for="${id}">可选项目： (以,分隔)</label>
<input id="${id}" type="text" id="radio-conf-${field.name}" value="${present}"/>
</div>
`;
    }

    applyConfiguration(field) {
        let id = "radio-conf-" + field.name;
        let valuesString = $("#" + id).val();
        let values = Array();
        valuesString.split(",").forEach(value => {
            if (value) values.push(value);
        });
        field.attributeMap.set("values", values);
    }
}

fieldControllers['radio'] = new RadioFieldController();

class CheckboxFieldController extends FieldController {
    constructor() {
        super("checkbox");
        this.displayName = "多选"
    }

    generateSubmissionHtml(field) {
        let values = field.attributeMap.get("values");

        if (!values) {
            return `<div class="alert alert-warning">存在问题的字段: ${field.name}</div>`;
        }

        let checkboxList = "";
        let containerId = this.getContainerId(field);
        values.forEach(function (val) {
            let checkboxId = `${containerId}-${val}`;
            checkboxList += `<input id="${checkboxId}" type="checkbox" name="${field.name}" value="${val}" /><label class="ml-1" for="${checkboxId}">${val}</label><div class="w-100"></div>`;
        })
        return checkboxList;
    }

    resolveSubmission(field) {
        let containerId = super.getContainerId(field);
        let checked = $(`#${containerId} input[name=${field.name}]:checked`);

        let array = Array();
        for (let val of checked) {
            array.push(val.value);
        }

        return array;
    }

    generateConfigurationHtml(field) {
        let id = "checkbox-conf-" + field.name;

        let values = field.attributeMap.get("values");
        let present = "";
        if (values) {
            values.forEach(function (val) {
                present += val;
                present += ",";
            })
        }

        return `
<div class="container">
<label for="${id}">可选项目： (以,分隔)</label>
<input id="${id}" type="text" id="checkbox-conf-${field.name}" value="${present}"/>
</div>
`;
    }

    applyConfiguration(field) {
        let id = "checkbox-conf-" + field.name;
        let valuesString = $("#" + id).val();
        let values = Array();
        valuesString.split(",").forEach(value => {
            if (value) values.push(value);
        });
        field.attributeMap.set("values", values);
    }
}

fieldControllers['checkbox'] = new CheckboxFieldController();

class RichTextFieldController extends FieldController {
    constructor() {
        super("rich-text");
        this.displayName = "富文本"
    }

    generateSubmissionHtml(field) {
        let containerId = this.getContainerId(field);
        let editorId = containerId + "-editor";

        setTimeout(function () {
            let element = document.getElementById(editorId);
            if (!element) return;
            let editor = new window.wangEditor(element);
            editor.config.menus = [
                'bold',
                'fontSize',
                'fontName',
                'italic',
                'underline',
                'strikeThrough',
                'lineHeight',
                'foreColor',
                'backColor',
                'list',
                'justify',
                'code',
            ]
            editor.create();
            element.editor = editor;
        }, 1);

        return `<div id="${editorId}"></div>`;
    }

    resolveSubmission(field) {
        let editorId = this.getContainerId(field) + "-editor";
        return document.getElementById(editorId).editor.txt.html();
    }
}

fieldControllers["rich-text"] = new RichTextFieldController();
