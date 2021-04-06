class TextFieldController extends FieldController {
    constructor() {
        super("text");
        this.displayName = "文本";
    }

    /**
     *
     * @param {SField} field
     * @returns {Function}
     */
    generateResolveFunction(field) {
        let inputId = 'text-input-for-' + field.name;
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
        let inputId = 'text-input-for-' + field.name;
        let placeholder = field.attributeMap.get("placeholder") || "";

        return `<label for="${inputId}">${field.name}</label><input type="text" placeholder="${placeholder}" id="${inputId}"/>`;
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
                c();
            }
        }
    }
}

fieldControllers['text'] = new TextFieldController();
