function _init_toast() {
    if ($("#template-toast")[0]) return;

    let template = document.createElement("template");
    template.id = "template-toast";
    template.innerHTML = `
<div class="toast" style="position: absolute; top: 0; right: 0; z-index: 11000">
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
    container.id = "toast-container";
    container.classList.add("position-fixed");
    container.classList.add("w-100");
    container.style.minHeight = '0';
    container.style.top = '0';
    container.style.right = '0';
    container.style.zIndex = '20000';
    document.body.appendChild(container);
}

function create_toast(title, content, delay) {
    _init_toast();
    if (!delay) delay = 2000;

    let template = $("#template-toast")[0];
    let id = makeid(6);

    template.content.querySelector(".toast").id = id;
    template.content.querySelector("strong").textContent = title;
    template.content.querySelector(".toast-body").innerHTML = content;

    let node = document.importNode(template.content, true);
    let container = $("#toast-container")[0];
    container.appendChild(node);

    setTimeout(() => {
        $("#" + id).toast({
            delay: delay
        }).toast('show');
    }, 1);
    setTimeout(() => {
        container.removeChild(container.querySelector("#" + id));
    }, delay + 200);
}

/**
 *
 * @param {jqXHR} xhr
 */
function getMessageFromAjaxError(xhr) {
    let title = decodeURIComponent(xhr.getResponseHeader("SUBMITEE-ERROR-TITLE"));
    if (title) {
        return title;
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
<i data-toggle="popover" data-container="body" data-placement="left" 
type="button" data-html="true" class="material-icons" 
style="font-size:2rem;color:red">error</i>
`;
    document.body.appendChild(template);

    template = document.createElement("template");
    template.id = "template-icon-tooltip-link";
    template.innerHTML = `
<a style="line-height: 0">
<i data-toggle="popover" data-container="body" data-placement="left" 
type="button" data-html="true" class="material-icons" 
style="font-size:2rem;color:red">error</i>
</a>
`;
    document.body.appendChild(template);
}

function createErrorTooltip(html) {
    return createOutFlowIconTooltip("error", "2rem", "red", html, "top");
}

function createIconTooltip(icon, size, color, html, placement, link) {
    _init_icon_tooltip();

    let elem = document.createElement("div");
    elem.innerHTML = html;

    let template = link ? $("#template-icon-tooltip-link")[0] : $("#template-icon-tooltip")[0];
    let i = template.content.querySelector("i");

    i.textContent = icon;
    i.style.fontSize = size;
    i.style.color = color;
    let id = makeid(6);
    i.id = id;

    if (link) {
        template.content.querySelector("a").href = link;
    }

    let node = document.importNode(template.content, true);
    setInterval(() => {
        $("#" + id).tooltip({
            html: true,
            title: elem,
            placement: placement,
            content: function () {
                return html;
            }
        });
    });
    return node;
}

function createOutFlowIconTooltip(icon, size, color, message, placement, link) {
    return createOutFlow(createIconTooltip(icon, size, color, message, placement, link));
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