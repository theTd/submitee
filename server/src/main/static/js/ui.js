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

function fetchPinyinCompositeList(stringOrArray) {
    let arr = false;
    if (Array.isArray(stringOrArray)) {
        arr = true;
        let map = {};
        for (let element of stringOrArray) {
            if (typeof element !== 'string') throw Error("not string");
            map[element] = 1;
        }
        stringOrArray = Object.keys(map);
    } else if (typeof stringOrArray !== 'string') {
        throw Error("not string");
    }
    if (!arr) {
        stringOrArray = [stringOrArray];
    }
    return new Promise((resolve, reject) => {
        $.ajax({
            url: "../pinyin",
            method: "POST",
            contentType: "application/json",
            data: JSON.stringify({list: stringOrArray}),
            success: function (response) {
                if (!arr) resolve(response[0]);
                let map = {};
                let cur = 0;
                for (let element of stringOrArray) {
                    map[element] = response[cur++];
                }
                resolve(map);
            },
            error: reject
        })
    });
}

class ExtendedList {
    constructor() {
        this.container = null;
        this.selections = Array();
        this.pinyinList = {};
        this.nodeCreateSelection = this.createNodeCreateSelection();
    }

    initList() {
        let outerContainer = document.createElement("div");
        this.outerContainer = outerContainer;

        $(outerContainer).addClass("d-flex");
        $(outerContainer).addClass("flex-row");
        $(outerContainer).addClass("extended-list-container");

        let leftContainer = document.createElement("div");
        this.leftContainer = leftContainer;

        if (this.enableSearch()) {
            $(leftContainer).append(this.createNodeSearch());
        }
        let selectionList = document.createElement("ul");
        this.selectionList = selectionList;
        $(selectionList).addClass("extended-list");
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

        if (this.enableSearch()) {
            setTimeout(() => {
                $(this.leftContainer).find(".extended-list-search")[0].focus();
            }, 100);
            this.updatePinyinList();
        }
    }

    show(containerSelector, placement) {
        if (this.container) this.close();

        this.container = $(containerSelector);
        if (!this.container) throw Error("unknown container: " + containerSelector);

        if (!this.outerContainer) {
            this.initList();
        }

        this.container.popover({
            content: this.outerContainer,
            placement: placement || 'right',
            html: true,
            sanitizeFn: (content) => content,
            focus: true,
            template: `
<div class="popover" role="tooltip" style="max-width: unset">
<div class="arrow"></div>
<h3 class="popover-header"></h3>
<div class="popover-body" style="box-shadow: #8d8d8d 0 0 5px 1px;margin: 0; padding: 0"></div>
</div>`
        }).popover('show');

        let inst = this;
        let listener = function (evt) {
            if (!inst.outerContainer.contains(evt.target)) {
                inst.close();
                document.removeEventListener("mousedown", listener, true);
            }
        }
        setTimeout(() => {
            document.addEventListener("mousedown", listener, true)
        });
    }

    close() {
        if (this.container) {
            this.container.popover('dispose');
            delete this.container;
        }
    }

    addSelection(selection) {
        this.selections.push(selection);
        if (this.outerContainer) {
            $(this.selectionList).append(this.createNodeSelection(selection));
            this.updatePinyinList();
        }
    }

    replaceSelection(previousSelection, newSelection) {
        let prevElement = this.getSelectionElement(previousSelection);
        let newElement = this.createNodeSelection(newSelection);
        this.selectionList.replaceChild(newElement, prevElement);
        this.updatePinyinList();
    }

    getSelectionElementByKey(key) {
        return $(this.selectionList).find(`li[data-selection-key="${key}"]`)[0];
    }

    getSelectionElement(selection) {
        return this.getSelectionElementByKey(this.getSelectionKey(selection));
    }

    createNodeSelection(selection) {
        let li = document.createElement("li");
        let key = this.getSelectionKey(selection);
        $(li).html(`<button style="width: 100%; height: 100%; text-align: unset">${key}</button>`);
        $(li).on("click", () => this.onSelect(selection));
        $(li).attr("data-selection-key", key);
        $(li).addClass("extended-list-item");
        $(li).addClass("extended-list-item-selection");
        return li;
    }

    getSelectionKey(selection) {
        return selection;
    }

    onSelect(selection) {
    }

    allowCreate() {
        return false;
    }

    enableSearch() {
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
        let inst = this;
        $(node).find("input").on("change", () => {
            $(node).find("input")[0].setCustomValidity("");
        })
        $(node).find("form").on("submit", () => {
            let input = $(node).find("input").val();
            if (input) {
                try {
                    let create = inst.createSelection(input);
                    if (!create) throw Error("无法完成操作");

                    let callback = function (result) {
                        if (result) {
                            inst.leftContainer.removeChild(node);
                            inst.addSelection(result);
                            inst.leftContainer.appendChild(inst.nodeCreateSelection);
                            $(inst.container).popover("update");
                        }
                    }
                    if (create.then) {
                        create.then(result => callback(result), error => {
                            let input = $(node).find("input")[0];
                            input.setCustomValidity(error);
                            input.reportValidity();
                        });
                    } else {
                        callback(create);
                    }
                } catch (e) {
                    let input = $(node).find("input")[0];
                    input.setCustomValidity(e.message);
                    input.reportValidity();
                }
            } else {
                let input = $(node).find("input")[0];
                input.setCustomValidity("空命名");
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

    createSelection(input) {
        return false;
    }

    createNodeCreateSelection() {
        let node = document.createElement("div");
        $(node).addClass("extended-list-item");
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
            if (this.container) this.container.popover("update");
        });
        return node;
    }

    updatePinyinList() {
        let updateList = Array();
        for (let childNode of this.selectionList.childNodes) {
            let key = $(childNode).attr("data-selection-key");
            if (!key) continue;
            if (!this.pinyinList[key]) {
                updateList.push(key);
            }
        }

        if (updateList.length !== 0) {
            fetchPinyinCompositeList(updateList).then(response => {
                for (let key of Object.keys(response)) {
                    this.pinyinList[key] = response[key];
                }
                this.updateSearch();
            }, error => {
                console.warn("ignoring xhr failure: " + getMessageFromAjaxError(error));
            });
        }
    }

    updateSearch() {
        let search = $(this.leftContainer).find(".extended-list-search").val()
        for (let childNode of this.selectionList.childNodes) {
            if (!search) {
                $(childNode).removeClass("disabled");
                $(childNode).removeClass("highlight");
                $(childNode).css("order", "unset");
                continue;
            }

            let key = $(childNode).attr("data-selection-key");
            if (!key) {
                console.warn("search cancelled due to data-selection-key missing");
                return;
            }

            let pinyinList = this.pinyinList[key];
            let matchArray = Array();
            matchArray.push(key);
            if (pinyinList) {
                for (let p of pinyinList) matchArray.push(p);
            }
            let show = false;
            for (let match of matchArray) {
                if (match.indexOf(search) !== -1) {
                    show = true;
                    $(childNode).removeClass("disabled");
                    $(childNode).addClass("highlight");
                    $(childNode).css("order", "1");
                    $(childNode).find("button").removeAttr("tabindex");
                    break;
                }
            }
            if (!show) {
                $(childNode).addClass("disabled");
                $(childNode).removeClass("highlight");
                $(childNode).css("order", "10");
                $(childNode).find("button").attr("tabindex", "-1")
            }
            $(this.container).popover("update");
        }
    }

    createNodeSearch() {
        let node = document.createElement("div");
        $(node).addClass("extended-list-item");
        $(node).addClass("extended-list-create-widget");
        $(node).addClass("d-flex").addClass("flex-row").addClass("align-items-center").addClass("ml-1");
        $(node).css("padding-top", "0.3rem");
        $(node).html(`
<input class="extended-list-search" size="1" type="text" style="min-width: 4rem; width: 100%"/>
<i class="material-icons ml-2" style="font-size: 1.2rem">search</i>`);
        $(node).find("input").on("keyup", () => this.updateSearch());
        return node;
    }
}

class CascadedList extends ExtendedList {

    onSelect(selection) {
        if (this.haveCascadeList(selection)) {
            let context = this.getCascadeList(selection);
            context.initList();
            this.setActivating(this.getSelectionElementByKey(this.getSelectionKey(selection)));
            this.setContextContent(context.outerContainer);
        } else {
            this.setActivating(null);
            this.setContextContent(null)
            this.onFinalSelect(selection);
        }
    }

    setActivating(li) {
        if (this.activating) {
            $(this.activating).removeClass("active");
        }
        $(li).addClass("active");
        this.activating = li;
    }

    onFinalSelect(selection) {
    }

    haveCascadeList(selection) {
        return false;
    }

    getCascadeList(selection) {
    }
}

class EditableCascadedList extends CascadedList {
    allowCreate() {
        return true;
    }

    allowCreateCascadeList() {
        return false;
    }

    addSelection(selection) {
        if (!selection["name"]) {
            throw Error("no name: " + JSON.stringify(selection));
        } else if (this.getSelectionElementByKey(selection["name"])) {
            throw Error("name conflict");
        }
        super.addSelection(selection);
    }

    createSelection(input) {
        if (this.getSelectionElementByKey(input)) {
            throw Error("命名冲突");
        }
        let create = {name: input, color: "#000000"};
        this.onCreate(create);
        // noinspection JSValidateTypes
        return create;
    }

    getSelectionKey(selection) {
        return super.getSelectionKey(selection.name);
    }

    createEditContext(selection) {
        let node = document.createElement("form");
        $(node).addClass("container");
        $(node).addClass("")
        $(node).css("padding", "2rem");
        $(node).css("width", "10rem");
        let id = makeid(6);
        $(node).html(`
<div class="row flex-row align-items-center flex-nowrap">
<label for="${id}" style="width: 3rem;margin: 3px 0">颜色</label>
<input style="padding: 0;text-align: center;width: 100%" type="color" value="${selection.color || "#000000"}" id="${id}"/><br/>
</div>
<div class="row flex-row align-items-center flex-nowrap">
<label for="${id}1" style="width: 3rem;margin: 3px 0">名称</label>
<input style="padding: 0;text-align: center;width: 100%" type="text" size="1" value="${selection.name}" id="${id}1"/>
<br/>
</div>
<div class="row justify-content-center">
<button type="submit" class="btn btn-sm btn-outline-primary" style="
font-size: 0.8rem; height: 1.3rem; padding: 0; width: 100%; margin-top: 0.8rem;">应用</button>
</div>
<div class="row justify-content-center">
<button type="button" class="btn btn-sm btn-outline-danger" style="
font-size: 0.8rem; height: 1.3rem; padding: 0; width: 100%; margin-top: 0.8rem;">删除</button>
</div>
`);
        $(node).find("input[type=text]").on("change", () => {
            let input = $(node).find("input[type=text]")[0];
            input.setCustomValidity("");
        })
        $(node).on("submit", () => {
            let color = $(node).find("input[type=color]").val();
            let name = $(node).find("input[type=text]").val();
            if (!name) {
                let input = $(node).find("input[type=text]")[0];
                input.setCustomValidity("空命名");
                input.reportValidity();
                return false;
            }
            let newSelection = {};
            Object.assign(newSelection, selection);
            newSelection["name"] = name;
            newSelection["color"] = color;
            if (name !== selection.name && this.getSelectionElement(newSelection)) {
                let input = $(node).find("input[type=text]")[0];
                input.setCustomValidity("命名冲突");
                input.reportValidity();
                return false;
            }
            this.onEdit(selection, newSelection);
            this.setActivating(null);
            this.setContextContent(null);
            this.replaceSelection(selection, newSelection);
            this.selections.splice(this.selections.indexOf(selection), 1, newSelection);
            return false;
        });
        $(node).find("button[type=button]").on("click", (evt) => {
            this.selections.splice(this.selections.indexOf(selection), 1);
            this.selectionList.removeChild(this.getSelectionElementByKey(this.getSelectionKey(selection)));
            this.setActivating(null);
            this.setContextContent(null);
            this.onDelete(selection);
        })
        return node;
    }

    createNodeSelection(selection) {
        let li = document.createElement("li");
        $(li).css("position", "relative");
        let createCascadeListButton = this.haveCascadeList(selection) ? `
<button class="editable-cascaded-list-create-cascade-list-button editable-extended-list-button text-success" title="该项有子列表">
<i class="material-icons" style="font-size: 1.1rem;">playlist_play</i></button>`
            : this.allowCreateCascadeList() ? `
<button class="editable-cascaded-list-create-cascade-list-button editable-extended-list-button" title="创建子列表">
<i class="material-icons" style="font-size: 1.1rem">playlist_add</i></button>` : "";
        let sortButtons = this.allowSort() ? `
<button class="editable-cascaded-list-moveup-button editable-extended-list-button" title="向上移动"><i class="material-icons" style="font-size: 1.1rem">north</i></button>
<button class="editable-cascaded-list-movedown-button editable-extended-list-button" title="向下移动"><i class="material-icons" style="font-size: 1.1rem">south</i></button>` : "";
        let editButton = this.allowEdit() ? `
<button class="editable-cascaded-list-edit-button editable-extended-list-button" title="编辑"><i class="material-icons" style="font-size: 1.1rem">edit</i></button>` : "";
        $(li).html(`
<div style="width: 100%; height: 100%; text-align: unset; color: ${selection.color}; display: flex; flex-direction: row; justify-content: space-between">
<span style="width: 100%; text-align: center; padding: 0 0.7rem;">${selection.name}</span>
${sortButtons}${editButton}${createCascadeListButton}
</div>
`);
        if (this.allowEdit()) {
            $(li).find(".editable-cascaded-list-edit-button")[0].addEventListener("click", (evt) => {
                evt.stopPropagation();
                this.setActivating(li);
                $(li).addClass("active");
                this.setContextContent(this.createEditContext(selection));
            }, true);
        }

        if (this.allowSort()) {
            $(li).find(".editable-cascaded-list-moveup-button")[0].addEventListener("click", (evt) => {
                evt.stopPropagation();
                let prevIdx = this.selections.indexOf(selection);
                if (prevIdx === 0) {
                    // already top
                    return;
                }
                let newIdx = prevIdx - 1;
                let alter = this.selections[newIdx];
                if (!alter) throw Error("selection on idx " + newIdx + " not found");
                this.selections.splice(prevIdx, 1);
                this.selections.splice(newIdx, 0, selection);

                let alterKey = this.getSelectionKey(alter);
                let movedKey = this.getSelectionKey(selection);
                let element = this.selectionList.removeChild(this.getSelectionElementByKey(movedKey));
                let before = this.getSelectionElementByKey(alterKey);
                before.parentNode.insertBefore(element, before);
                this.onMoveUp(selection);
            }, true);
            $(li).find(".editable-cascaded-list-movedown-button")[0].addEventListener("click", (evt) => {
                evt.stopPropagation();
                let prevIdx = this.selections.indexOf(selection);
                if (prevIdx === this.selections.length - 1) {
                    // already bottom
                    return;
                }
                let newIdx = prevIdx + 1;
                this.selections.splice(prevIdx, 1);
                this.selections.splice(newIdx, 0, selection);

                let before;
                if (newIdx === this.selections.length - 1) {
                    // move to bottom
                } else {
                    let beforeSelection = this.selections[newIdx + 1];
                    before = this.getSelectionElementByKey(this.getSelectionKey(beforeSelection));
                }
                let movedKey = this.getSelectionKey(selection);
                let element = this.selectionList.removeChild(this.getSelectionElementByKey(movedKey));
                if (!before) {
                    // append to bottom
                    this.selectionList.appendChild(element);
                } else {
                    this.selectionList.insertBefore(element, before);
                }
                this.onMoveDown(selection);
            });
        }

        if (this.haveCascadeList(selection) || this.allowCreateCascadeList()) {
            $(li).find(".editable-cascaded-list-create-cascade-list-button")[0].addEventListener("click", (evt) => {
                evt.stopPropagation();
                if (this.haveCascadeList(selection)) {
                    this.onSelect(selection);
                } else {
                    let lst = this.getCascadeList(selection);
                    if (lst) {
                        this.selectionList.replaceChild(this.createNodeSelection(selection),
                            this.getSelectionElementByKey(this.getSelectionKey(selection)));
                        this.setActivating(this.getSelectionElementByKey(this.getSelectionKey(selection)));
                        lst.initList();
                        this.setContextContent(lst.outerContainer);
                    } else {
                        console.warn("no cascaded list created for selection " + selection["name"]);
                    }
                }
            }, true);
        }

        $(li).css("cursor", "pointer");
        $(li).on("click", () => this.onSelect(selection));
        $(li).addClass("extended-list-item");
        $(li).addClass("extended-list-item-selection");
        $(li).attr("data-selection-key", selection.name);
        return li;
    }

    allowSort() {
        return false;
    }

    allowEdit() {
        return false;
    }

    onSelect(selection) {
        if (this.haveCascadeList(selection)) {
            let lst = this.getCascadeList(selection);
            lst.initList();
            this.setContextContent(lst.outerContainer);
            this.setActivating(this.getSelectionElementByKey(this.getSelectionKey(selection)));
        } else {
            this.setActivating(null);
            this.setContextContent(null);
            this.onFinalSelect(selection);
        }
    }

    onMoveUp(selection) {
    }

    onMoveDown(selection) {
    }

    onEdit(selection, newSelection) {
    }

    onDelete(selection) {
    }

    onCreate(selection) {
    }
}

class TagSelector extends EditableCascadedList {
    constructor(tags, selectHook, exclude, canEdit) {
        super();
        this.tags = tags;
        this.selectHook = selectHook;
        this.exclude = Array.isArray(exclude) ? exclude : [];
        this.canEdit = !!canEdit;

        if (!canEdit) {
            for (let value of Object.values(this.tags)) {
                this.addSelection(value);
            }
        } else {
            for (let meta of Object.values(this.tags)) {
                if (this.exclude.indexOf(meta.id) !== -1) {
                    this.addSelection(meta);
                }
            }
        }
    }

    getSelectionKey(selection) {
        return selection.name;
    }

    onFinalSelect(selection) {
        this.setContextContent(null);
        if (this.selectHook) {
            this.selectHook(selection);
        }
    }

    allowEdit() {
        return false;
    }

    allowCreate() {
        return this.canEdit;
    }

    haveCascadeList(selection) {
        return false;
    }

    enableSearch() {
        return false;
    }

    createNodeCreateSelection() {
        let node = document.createElement("div");
        $(node).addClass("extended-list-item");
        $(node).css("height", "1.2rem");
        $(node).css("min-height", "unset")
        $(node).html(`
<button style="width: 100%; font-size: 0.8rem; display: flex; flex-direction: column; align-items: center;
 justify-items: center;">
 <i style="font-size: 0.8rem" class="material-icons text-success font-weight-bold">add</i>
 </button>`);

        let outerList = this;
        let fnBuildInsertList = function () {
            let availableTags = Array();
            for (let tagMeta of Object.values(outerList.tags)) {
                if (outerList.exclude.indexOf(tagMeta.id) === -1) {
                    availableTags.push(tagMeta);
                }
            }
            if (availableTags.length === 0) {
                outerList.setContextContent(null);
                return;
            }

            let insertList = new class extends TagSelector {
                constructor() {
                    super(availableTags);
                }

                onFinalSelect(selection) {
                    outerList.exclude.push(selection.id);
                    outerList.addSelection(selection);
                    fnBuildInsertList();
                }
            }
            insertList.initList();
            outerList.setContextContent(insertList.outerContainer);
        }
        $(node).find("button").on("click", fnBuildInsertList);
        return node;
    }

    createNodeSelection(selection) {
        let li = document.createElement("li");
        $(li).css("position", "relative");
        let removeButton = this.canEdit ? `
<button class="tag-selector-remove-button editable-extended-list-button" title="移除">
<i class="material-icons" style="font-size: 1.1rem">close</i></button>
        ` : "";
        $(li).html(`
<div style="width: 100%; height: 100%; text-align: unset; color: ${selection.color}; display: flex; flex-direction: row; justify-content: space-between">
<span style="width: 100%; text-align: center; padding: 0 0.7rem;">${selection.name}</span>
${removeButton}
</div>
`);
        if (this.canEdit) {
            $(li).find(".tag-selector-remove-button")[0].addEventListener("click", (evt) => {
                evt.stopPropagation();
                this.onRemoveTag(selection.id);
                this.setContextContent(null);
                this.getSelectionElementByKey(this.getSelectionKey(selection)).remove();
                this.exclude.splice(this.exclude.indexOf(selection.id), 1);
            })
        }

        $(li).css("cursor", "pointer");
        $(li).on("click", () => this.onFinalSelect(selection));
        $(li).addClass("extended-list-item");
        $(li).addClass("extended-list-item-selection");
        $(li).attr("data-selection-key", selection.name);

        if (!this.canEdit) {
            if (this.exclude.indexOf(selection.id) !== -1) {
                $(li).addClass("disabled");
                $(li).css("order", "10");
                $(li).attr("tab-index", -1);
            } else {
                $(li).css("order", "1");
                // $(sel).attr("tab-index", 0);
            }
        }
        return li;
    }

    onRemoveTag() {
    }
}

function createConfirmDialog(selectorOrElement, onConfirm, title) {
    let div = document.createElement("div");
    $(div).addClass("confirm-dialog");
    $(div).html(`<button class="btn btn-outline-danger">长按确认</button><div class="confirm-dialog-progressbar"></div>`);

    let prog = 0.0;
    let schedule;
    let progressbar = $(div).find(".confirm-dialog-progressbar");
    $(div).find("button")
        .on("mousedown", () => {
            // start timing
            schedule = setInterval(() => {
                if (prog > 1) {
                    clearInterval(schedule)
                    $(target).popover('dispose');
                    document.removeEventListener("click", listener);
                    onConfirm();
                    return;
                }
                prog += 0.1;
                progressbar.css("clip-path", `inset(0 ${new Intl.NumberFormat().format(100 * (1 - prog)) + "%"} 0 0)`);
            }, 100);
        })
        .on("mouseup blur", () => {
            prog = 0.0;
            progressbar.css("clip-path", `inset(0 100% 0 0)`);
            clearInterval(schedule);
        });

    let target = selectorOrElement;
    if (typeof target === 'string') {
        target = $(target)[0];
    }

    $(target).popover({
        content: div,
        placement: "top",
        title: title || "",
        html: true,
        template: `
<div class="popover" role="tooltip" style="max-width: unset">
<div class="arrow"></div>
<h3 class="popover-header"></h3>
<div class="popover-body" style="margin: 0; padding: 0"></div>
</div>`,
        sanitizeFn: (content) => content,
    }).popover('show');
    let listener = function (evt) {
        if (!div.contains(evt.target)) {
            $(target).popover('dispose');
            document.removeEventListener("click", listener);
        }
    };
    setTimeout(() => {
        document.addEventListener("click", listener);
    });
}

async function asyncCreateRegionCascadedList(selectCallback) {
    if (!submitee["region-model"]) {
        submitee["region-model"] = await new Promise(resolve => {
            $.ajax({
                url: "assets/region.json",
                success: function (response) {
                    let obj;
                    if (typeof response === 'string') {
                        obj = JSON.parse(response);
                    } else {
                        obj = response;
                    }
                    let model = {};
                    for (let id of Object.keys(obj).sort()) {
                        let tid = id.substr(4);
                        let sid = id.substr(2, 2);
                        let fid = id.substr(0, 2);
                        let display = obj[id];
                        if (tid === '00') {
                            if (sid === '00') {
                                // first class
                                model[id] = {id: id, display: display};
                            } else {
                                // second class
                                let o = model[fid + "0000"];
                                if (!o) continue;

                                let child = o["child"];
                                if (!child) {
                                    child = {};
                                    o["child"] = child;
                                }
                                child[id] = {id: id, display: display};
                            }
                        } else {
                            // third class
                            let lvl = model[fid + "0000"];
                            if (lvl["child"]) {
                                let s = lvl["child"][fid + sid + "00"];
                                if (s) lvl = s;
                            }

                            let child = lvl["child"];
                            if (!child) {
                                child = {};
                                lvl["child"] = child;
                            }
                            child[id] = {id: id, display: display};
                        }
                    }
                    resolve(model);
                }
            })
        })
    }

    let fnInitCascadedList = function (struct) {
        let list = new RegionCascadedList();
        for (let k of Object.keys(struct)) {
            let obj = struct[k];
            obj["id"] = k;
            list.addSelection(obj);
        }
        return list;
    }

    class RegionCascadedList extends CascadedList {
        onFinalSelect(selection) {
            selectCallback(selection);
        }

        haveCascadeList(selection) {
            return selection["child"];
        }

        getCascadeList(selection) {
            return fnInitCascadedList(selection["child"]);
        }

        getSelectionKey(selection) {
            return selection["display"];
        }

        enableSearch() {
            return true;
        }
    }

    return fnInitCascadedList(submitee["region-model"]);
}

function createTagElement(tagId, configuration, deleteCallback, clickCallback, appendCallback) {
    if (!configuration["tags"]) return;
    let tagMeta = configuration["tags"][tagId];
    if (!tagMeta) return;
    let div = document.createElement("div");
    $(div).addClass("tag-box");
    $(div).addClass("position-relative");
    $(div).css("background-color", tagMeta["color"]);
    if (clickCallback) {
        $(div).html(`<button class="btn-tag-box-append"></button><button class="btn-tag-box-label">${tagMeta["name"]}</button><button class="btn-tag-box-delete" title="删除标签"></button>`);
        $(div).find(".btn-tag-box-label").on("click", () => {
            clickCallback();
        });
    } else {
        $(div).html(`${tagMeta["name"]}<button class="btn-tag-box-delete" title="删除标签"></button>`);
    }
    if (!deleteCallback) {
        $(div).find(".btn-tag-box-delete").remove();
    } else {
        $(div).find(".btn-tag-box-delete").on("click", () => {
            deleteCallback();
        });
    }
    if (!appendCallback) {
        $(div).find(".btn-tag-box-append").remove();
    } else {
        $(div).find(".btn-tag-box-append").on("click", () => {
            appendCallback();
        });
    }
    return div;
}
