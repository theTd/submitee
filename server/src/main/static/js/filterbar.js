function createFilterBar(selector) {
    let element = document.querySelector(selector);
    element.classList.add("filter-container");
    return new FilterBarHandle(element);
}

class FilterBarHandle {
    constructor(element) {
        this.element = element;
        this.ctx = {};
    }

    setValue(key, value) {
        let ctx = this.ctx[key];
        if (!ctx) return;
        ctx.value = value;

        let valueDiv = $(ctx.node).find(".filter-element-value");
        if (!value) {
            if (ctx.setValueCallback) {
                ctx.node.classList.remove("d-none");
                ctx.startEdit();
                ctx.controller.focus(valueDiv);
            } else {
                ctx.node.classList.add("d-none");
                if (ctx.removeCallback) ctx.removeCallback();
            }
        } else {
            ctx.node.classList.remove("d-none");
            valueDiv.html(ctx.controller.getLocaleValue(value)/* || value*/);
            ctx.editing = false;
            if (ctx.setValueCallback) ctx.setValueCallback(value);
        }
    }

    init(props) {
        let key = props["key"];
        let displayKey = props["displayKey"] || key;
        let value = props["value"];
        let removeCallback = props["removeCallback"];
        let selectionFeeder = props["selectionFeeder"];
        let setValueCallback = props["setValueCallback"];
        let datePicker = props["datePicker"];

        if (this.ctx[key]) {
            console.warn("re init filterbar ctx: " + key);
        }

        let node = document.createElement("div");
        let ctx = {
            node: node,
            value: value,
            removeCallback: removeCallback,
            selectionFeeder: selectionFeeder,
            setValueCallback: setValueCallback,
            datePicker: datePicker,
            editing: false
        };
        this.ctx[key] = ctx;

        ctx.controller = selectionFeeder ? new SelectionFilterbarController(ctx) :
            datePicker ? new DatetimeFilterbarController(ctx) : new TextFilterbarController(ctx);

        let displayValue = value ? ctx.controller.getLocaleValue(value) : "";

        node.classList.add("filter-element");
        if (!value) node.classList.add("d-none");
        node.innerHTML = `
<span class="filter-element-title">${displayKey}</span>
<div class="filter-element-value">${displayValue}</div>
<button class="filter-close-button"></button>
`;
        node.querySelector("button").addEventListener("click", () => {
            delete this.ctx[key];
            node.parentNode.removeChild(node);
            if (removeCallback) removeCallback();
        });

        if (setValueCallback) {
            let finalCtx = this.ctx[key];
            let startEdit = function () {
                if (finalCtx.editing) return;
                finalCtx.editing = true;

                let val = finalCtx.value;
                let valueDiv = $(node).find(".filter-element-value");
                let controller = finalCtx.controller;

                valueDiv.html(`<form>${controller.generateInputHtml(val)}</form>`)
                    .find("form").on("submit focusout", function (evt) {
                    evt.preventDefault();
                    finalCtx.editing = false;
                    val = controller.parseInputValue(valueDiv);
                    finalCtx.value = val;
                    if (!val) {
                        node.classList.add("d-none");
                        if (removeCallback) removeCallback();
                    } else {
                        node.classList.remove("d-none");
                        valueDiv.html(controller.getLocaleValue(val) || val);
                        setValueCallback(val);
                    }
                });
                controller.focus(valueDiv);
            };
            ctx.startEdit = startEdit;

            $(node).find(".filter-element-value").on("click", startEdit);
        }

        this.element.appendChild(node);
    }

    get(key) {
        let ctx = this.ctx[key];
        return ctx ? ctx.value : null;
    }
}

class FilterbarController {
    constructor(ctx) {
        this.ctx = ctx;
    }

    generateInputHtml(value) {
        return `<span class="alert alert-danger">存在问题的控制器</span>`;
    }

    parseInputValue(jqForm) {
    }

    getLocaleValue(value) {
        return value;
    }

    focus(jqForm) {
    }
}

class TextFilterbarController extends FilterbarController {
    generateInputHtml(value) {
        value = value || "";
        return `<input type='text' value='${value}'/>`;
    }

    parseInputValue(jqForm) {
        return jqForm.find("input").val();
    }

    getLocaleValue(value) {
        let locale = '<span class="text-sm-center">'
        let split = value.split("*");
        for (let i = 0; i < split.length; i++) {
            locale += split[i];
            if (i !== split.length - 1) {
                locale += "<span class='text-danger' style='font-size: 1rem'>*</span>"
            }
        }
        return locale + "</span>";
    }

    focus(jqForm) {
        jqForm.find("input")[0].focus();
        console.log(jqForm.find("input")[0])
    }
}

class SelectionFilterbarController extends FilterbarController {
    generateInputHtml(value) {
        let options = "";
        let selectionFeeder = this.ctx.selectionFeeder;
        for (let selectionKey of Object.keys(selectionFeeder)) {
            let displayValue = selectionFeeder[selectionKey] || selectionKey;
            options += `<option value="${selectionKey}" ${value === selectionKey ? 'selected' : ''}>${displayValue}</option>`;
        }
        return `<select>${options}</select>`;
    }

    parseInputValue(jqForm) {
        return jqForm.find("option:selected").val();
    }

    getLocaleValue(value) {
        let selectionFeeder = this.ctx.selectionFeeder;
        return selectionFeeder[value] || value;
    }

    focus(jqForm) {
        jqForm.find("select")[0].focus();
    }
}

class DatetimeFilterbarController extends FilterbarController {
    generateInputHtml(value) {
        let time = (value ? new Date(value) : new Date());
        time = new Date(time.getTime() - time.getTimezoneOffset() * 60000);
        let format = time.toISOString().split(".")[0];
        console.log(format);
        return `<input type="datetime-local" value="${format}"/>`;
    }

    parseInputValue(jqForm) {
        let value = jqForm.find("input").val();
        return new Date(value).getTime();
    }

    getLocaleValue(value) {
        return submitee.relativeTimeLocale(value);
    }

    focus(jqForm) {
        jqForm.find("input")[0].focus();
    }
}
