class TemplateFilterbarController extends FilterbarController {
    constructor(templates, templateVersionMap) {
        super();
        this.templates = templates;
        this.templateVersionMap = templateVersionMap;
        this.selected = getQueryValue("tid");
    }

    generateInputHtml(value) {
        let options = '';
        for (let templateId of Object.keys(this.templateVersionMap)) {
            let arr = this.templateVersionMap[templateId];
            let selected = this.selected === templateId ? "selected" : "";
            options += `<option value="${templateId}" ${selected}>${templateId}:*</option>`;
            for (let t of arr) {
                let selected = this.selected === t.uniqueId ? "selected" : "";
                options += `<option value="${t.uniqueId}" ${selected}>┗${t.templateId}:${t.version} (${t.name})</option>`;
            }
        }
        return `<select>${options}</select>`;
    }

    parseInputValue(jqForm) {
        return jqForm.find("option:selected").val();
    }

    getLocaleValue(value) {
        if (submitee.uuidPattern.test(value)) {
            let t = this.templates.filter(t => t.uniqueId === value)[0];
            return `${t.templateId}:${t.version} (${t.name})`;
        } else {
            return `${value}:<span style="color: red; font-size: 1rem">*</span>`;
        }
    }

    focus(jqForm) {
        jqForm.find("select")[0].focus();
    }
}