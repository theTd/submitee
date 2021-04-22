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
        let controller = props["controller"];

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
            editing: false,
            controller: controller
        };
        this.ctx[key] = ctx;

        if (!controller) ctx.controller = selectionFeeder ? new SelectionFilterbarController() :
            datePicker ? new DatetimeFilterbarController() : new TextFilterbarController();
        ctx.controller.ctx = ctx;

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
        time = new Date(time.getTime() - (time.getTimezoneOffset() * 60000));
        let format = time.toISOString().split(".")[0];
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

function buildPagination(containerSelector, currentPage, maxPages, switchPageHandle) {
    let container = $(containerSelector);
    container.html(`
<ul class="pagination" style="margin: 0">
</ul>
<form style="display: flex;align-items: center; margin-left: 1rem">
    <input type="number" min="1" max="${maxPages}" value="${currentPage}"
           style="display: inline-block;width: 2.5rem; margin-right: 0.4rem"/>
    <button type="submit" class="btn btn-sm btn-outline-secondary"
            style="display: inline-block; font-size: 0.8rem">前往
    </button>
</form>
    `);
    container.find("form").on("submit", () => {
        switchPageHandle(parseInt(container.find("form").find("input[type=number]").val()));
        return false;
    });
    let pagination = container.find("ul");

    let prevPage = currentPage - 1;
    if (prevPage < 1) prevPage = null;
    let nextPage = currentPage + 1;
    if (nextPage > maxPages) nextPage = null;

    let prevButton = createPageItem("前页", prevPage, false, switchPageHandle);
    if (!prevPage) {
        $(prevButton).addClass("disabled");
    } else {
        $(document).on("keydown", (evt) => {
            if (evt.code === 'ArrowLeft') switchPageHandle(prevPage);
        });
    }

    pagination.append(prevButton);

    for (let i = 1; i <= maxPages; i++) {
        pagination.append(createPageItem(i, i, i === currentPage, switchPageHandle))
    }

    let nextButton = createPageItem("后页", nextPage, false, switchPageHandle);
    if (!nextPage) {
        $(nextButton).addClass("disabled");
    } else {
        $(document).on("keydown", (evt) => {
            if (evt.code === 'ArrowRight') switchPageHandle(nextPage);
        });
    }

    pagination.append(nextButton);
}

function createPageItem(text, page, active, switchPageHandle) {
    let node = document.createElement("li");
    $(node).addClass("page-item");
    $(node).html(`<button class="page-link">${text}</button>`);
    $(node).find("button").on("click", () => switchPageHandle(page));
    if (active) $(node).addClass("active");
    return node;
}

function _init_toast() {
    if ($("#template-toast")[0]) return;

    submitee.toastDistinct = {};

    let template = document.createElement("template");
    template.id = "template-toast";
    template.innerHTML = `
<div class="toast" style="direction: ltr">
    <div class="toast-header">
        <strong class="mr-auto"></strong>
        <button type="button" class="ml-2 mb-1 close" data-dismiss="toast" aria-label="Close">
            <span>&times;</span>
        </button>
    </div>
    <div class="toast-body">
    </div>
</div>
`;
    document.body.appendChild(template);

    let container = document.createElement("div");
    // container.id = "toast-container";
    container.classList.add("position-fixed");

    // container.classList.add("w-100");
    container.style.minHeight = '0';
    container.style.top = '5px';
    container.style.right = '5px';
    container.style.direction = "rtl";
    container.style.zIndex = '20000';

    let innerContainer = document.createElement("div");
    innerContainer.id = "toast-container";
    innerContainer.classList.add("position-absolute");
    innerContainer.style.width = "max-content";

    container.appendChild(innerContainer);
    document.body.appendChild(container);
}

function create_toast(title, content, delay, distinct) {
    _init_toast();
    if (!delay) delay = 2000;
    else if (!parseInt(delay)) {
        distinct = delay;
        delay = 2000;
    }


    let template = $("#template-toast")[0];
    let id = makeid(6);

    template.content.querySelector(".toast").id = id;
    template.content.querySelector("strong").textContent = title;
    template.content.querySelector(".toast-body").innerHTML = content;

    let node = document.importNode(template.content, true);
    let container = $("#toast-container")[0];

    if (distinct) {
        try {
            container.removeChild(container.querySelector("#" + submitee.toastDistinct[distinct]));
        } catch (e) {
        }
    }

    container.appendChild(node);

    if (distinct) {
        submitee.toastDistinct[distinct] = id;
    }

    setTimeout(() => {
        $("#" + id).toast({
            delay: delay
        }).toast('show');
    }, 1);
    setTimeout(() => {
        let rm = container.querySelector("#" + id)
        if (rm) container.removeChild(rm);
    }, delay + 1000);
}

/**
 *
 * @param {jqXHR} xhr
 */
function getMessageFromAjaxError(xhr) {
    let raw = xhr.getResponseHeader("SUBMITEE-ERROR-TITLE");
    if (raw) {
        return decodeURIComponent(escape(atob(raw))); // tricky skill decoding base64 to utf-8 string
    } else {
        return xhr.status === 0 ? "无法连接到服务器" : xhr.statusText;
    }
}

function getErrorClassifyFromAjaxError(xhr) {
    return xhr.getResponseHeader("SUBMITEE-ERROR-CLASSIFY");
}

/**
 *
 * @param {jqXHR} xhr
 */
function toast_ajax_error(xhr) {
    if (getErrorClassifyFromAjaxError(xhr) === "ACCESS_DENIED") {
        create_toast("重要提示", `<p>${getMessageFromAjaxError(xhr)}</p><p><a href="javascript:sendToAuthPage()">前往认证页</a></p>`, 10000)
    } else {
        create_toast("重要提示", `<p>${getMessageFromAjaxError(xhr)}</p>`, 10000)
    }
}

function _init_icon_tooltip() {
    if ($("#template-icon-tooltip")[0]) return;

    // region load css
    let link = document.createElement('link');
    link.rel = 'stylesheet';
    link.href = 'https://fonts.googleapis.com/icon?family=Material+Icons';
    let head = document.getElementsByTagName('head')[0];
    head.appendChild(link);

    let tooltipStyle = document.createElement("style");
    tooltipStyle.innerHTML = ".tooltip-inner p {margin:0 !important;}";
    document.body.appendChild(tooltipStyle);
    // endregion

    let template = document.createElement("template");
    template.id = "template-icon-tooltip";
    template.innerHTML = `
<button type="button" style="line-height: 0; background-color: transparent;margin: 0;padding: 0;border: 0">
<i data-toggle="popover" data-container="body" data-placement="left" 
type="button" data-html="true" class="material-icons" 
style="font-size:2rem;color:red">error</i>
</button>
`;
    document.body.appendChild(template);
}

function createErrorTooltip(html) {
    return createOutFlowIconTooltip("error", "2rem", "red", html, "top");
}

function createIconTooltip(icon, size, color, html, placement, callback) {
    _init_icon_tooltip();

    let elem = document.createElement("div");
    elem.innerHTML = html;

    let template = $("#template-icon-tooltip")[0];
    let i = template.content.querySelector("i");

    i.textContent = icon;
    i.style.fontSize = size;
    i.style.color = color;

    let button = template.content.querySelector("button");
    let id = makeid(6);
    button.id = id;

    let node = document.importNode(template.content, true);
    $(node).find("#" + id).tooltip({
        html: true,
        title: elem,
        placement: placement,
        content: function () {
            return html;
        }
    }).on("click", () => {
        if (callback) callback();
    });
    return node;
}

function createOutFlowIconTooltip(icon, size, color, message, placement, callback) {
    return createOutFlow(createIconTooltip(icon, size, color, message, placement, callback));
}

function _init_out_flow() {
    if ($("#template-out-flow")[0]) return;

    let template = document.createElement("template");
    template.id = "template-out-flow";
    template.innerHTML = `
<div class="d-inline position-relative no-gutters out-flow-container">
<div class="position-absolute d-inline">

</div>
</div>`;
    document.body.appendChild(template);
}

function createOutFlow(node) {
    _init_out_flow();

    let template = $("#template-out-flow")[0];
    let id = makeid(6);
    template.content.querySelector("div").id = id;
    setTimeout(() => {
        $("#" + id).find(".position-absolute")[0].appendChild(node);
    });
    return document.importNode(template.content, true);
}

function setTitle(title) {
    document.title = title + " - SUBMITEE";
}

setTitle(document.title);

function noSanitizePopover(selector, html, placement, title) {
    let o = $(selector);
    if (o.data("bs.popover")) {
        $("#" + o.data("bs.popover")["tip"].id).remove();
        o.data("bs.popover", null);
    }
    if (title) {
        o.popover({
            container: 'body',
            content: html,
            title: title,
            html: true,
            placement: placement,
            trigger: 'focus',
            sanitizeFn: (content) => content
        }).popover('show');
    } else {
        o.popover({
            container: 'body',
            content: html,
            html: true,
            placement: placement,
            trigger: 'focus',
            sanitizeFn: (content) => content
        }).popover('show');
    }
}

class ExtendedList {
    constructor(containerSelector) {
        this.container = $($(containerSelector)[0]);
        if (!this.container) throw new Error("invalid container");
        this.selections = Array();
    }

    show() {
        let outerContainer = document.createElement("div");
        this.outerContainer = outerContainer;

        $(outerContainer).addClass("d-flex");
        $(outerContainer).addClass("flex-row");

        let leftContainer = document.createElement("div");
        this.leftContainer = leftContainer;

        $(leftContainer).append(this.createNodeSearch());
        let selectionList = document.createElement("ul");
        this.selectionList = selectionList;
        $(selectionList).addClass("extended-list");
        $(selectionList).css("max-width", "15rem");
        // $(selectionList).append(this.createNodeSearch());
        for (let selection of this.selections) {
            $(selectionList).append(this.createNodeSelection(selection));
        }
        leftContainer.append(selectionList);

        if (this.allowCreate()) {
            $(leftContainer).append(this.createNodeCreateSelection());
        }

        outerContainer.appendChild(leftContainer);

        let contextContainer = document.createElement("div");
        $(contextContainer).addClass("extended-list-context-container");
        outerContainer.appendChild(contextContainer);
        this.contextContainer = contextContainer;

        this.container.popover({
            content: outerContainer,
            placement: 'top',
            html: true,
            sanitizeFn: (content) => content,
            focus: true,
            template: `
<div class="popover" role="tooltip">
<div class="arrow"></div>
<h3 class="popover-header"></h3>
<div class="popover-body" style="box-shadow: #8d8d8d 0 0 5px 1px;margin: 0; padding: 0"></div>
</div>`
        }).popover('show');

        let lst = this;
        let listener = function (evt) {
            if (!outerContainer.contains(evt.target)) {
                lst.close();
                document.removeEventListener("click", listener, true);
            }
        }
        setTimeout(() => document.addEventListener("click", listener, true));
    }

    addSelection(selection) {
        this.selections.push(selection);
        if (this.outerContainer) {
            $(this.selectionList).append(this.createNodeSelection(selection));
        }
    }

    createNodeSelection(selection) {
        let li = document.createElement("li");
        $(li).html(`<button style="width: 100%; height: 100%; text-align: unset">${this.localizeSelection(selection)}</button>`);
        $(li).on("click", () => this.onSelect(selection));
        $(li).addClass("extended-list-item");
        $(li).addClass("extended-list-item-selection");
        return li;
    }

    localizeSelection(selection) {
        return selection + "";
    }

    onSelect(selection) {
    }

    close() {
        this.container.popover('dispose');
    }

    allowCreate() {
        return false;
    }

    setContextContent(node) {
        $(this.contextContainer).empty();
        $(this.contextContainer).append(node);
    }

    createNodeCreateForm() {
        let node = document.createElement("li");
        $(node).addClass("extended-list-item");
        $(node).addClass("extended-list-create-widget");
        $(node).html(`
<form class="d-flex flex-row align-items-center ml-1">
<input type="text" size="1" style="border:1px solid black; border-radius: 2px; min-width: 4rem; width: 100%"/>
<button type="submit" class="btn d-flex m-1 ml-2 p-0 shadow-none">
<i class="material-icons text-success" style="font-size: 1rem">check</i>
</button>
<button type="button" class="btn d-flex m-1 p-0 shadow-none">
<i class="material-icons text-danger" style="font-size: 1rem">close</i>
</button>
</form>`);
        let lst = this;
        $(node).find("form").on("submit", async () => {
            let input = $(node).find("input").val();
            if (!input) return;
            try {
                let createdSelection = await lst.createSelection(input);
                if (createdSelection) {
                    this.leftContainer.removeChild(node);
                    lst.addSelection(createdSelection);
                    this.leftContainer.appendChild(this.createNodeCreateSelection());
                    $(this.container).popover("update");
                }
            } catch (e) {
                let input = $(node).find("input")[0];
                input.setCustomValidity(e.message);
                input.reportValidity();
            }
            return false;
        });
        $(node).find("form").find("button[type=button]").on("click", () => {
            this.leftContainer.removeChild(node);
            this.leftContainer.appendChild(this.createNodeCreateSelection());
            $(this.container).popover("update");
        });
        return node;
    }

    async createSelection(input) {
        return false;
    }

    createNodeCreateSelection() {
        let node = document.createElement("div");
        $(node).addClass("extended-list-item");
        // $(node).addClass("extended-list-item-create-button");
        $(node).css("height", "1.2rem");
        $(node).css("min-height", "unset")
        $(node).html(`
<button style="width: 100%; font-size: 0.8rem; display: flex; flex-direction: column; align-items: center;
 justify-items: center;">
 <i style="font-size: 0.8rem" class="material-icons text-success font-weight-bold">add</i>
 </button>`);
        $(node).find("button").on("click", () => {
            node.parentNode.removeChild(node);
            let widget = this.createNodeCreateForm();
            this.leftContainer.append(widget);
            $(widget).find("input")[0].focus();
            this.container.popover("update");
        });
        return node;
    }

    createNodeSearch() {
        let node = document.createElement("div");
        $(node).addClass("extended-list-item");
        $(node).addClass("extended-list-create-widget");
        $(node).addClass("d-flex").addClass("flex-row").addClass("align-items-center").addClass("ml-1");
        $(node).css("padding-top", "0.3rem");
        $(node).html(`
<input size="1" type="text" style="border:1px solid black; border-radius: 2px; min-width: 4rem; width: 100%"/>
<i class="material-icons ml-2" style="font-size: 1.2rem">search</i>`);
        $(node).find("input").on("keyup", () => {
            let pattern = $(node).find("input").val();

            for (let childNode of this.selectionList.childNodes) {
                if ($(childNode).hasClass("extended-list-item-selection")) {
                    let text = $(childNode).text();
                    if (text.indexOf(pattern) !== -1) {
                        $(childNode).removeClass("d-none");
                    } else {
                        $(childNode).addClass("d-none");
                    }
                    $(this.container).popover("update");
                }
            }
        });
        return node;
    }
}
