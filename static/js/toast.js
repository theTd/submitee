function _init_toast() {
    if ($("#template-toast")[0]) return;

    let template = document.createElement("template");
    template.id = "template-toast";
    template.innerHTML = `
<div class="toast" style="position: absolute; top: 0; right: 0; z-index: 999">
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
    document.body.appendChild(container);
}

function create_toast(title, content, delay) {
    _init_toast();

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
 * @param {jqXHR} error
 */
function getMessageFromAjaxError(error) {
    let title = decodeURI(error.getResponseHeader("SUBMITEE-ERROR-TITLE"));
    if (title) {
        return title;
    } else {
        return error.status === 0 ? "无法连接到服务器" : error.statusText;
    }
}

/**
 *
 * @param {jqXHR} error
 */
function toast_ajax_error(error) {
    create_toast("重要提示", getMessageFromAjaxError(error), 10000)
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
<i data-toggle="popover" data-container="body" data-placement="left" type="button" data-html="true" class="material-icons" style="font-size:2rem;color:red">error</i>
`;

    document.body.appendChild(template);
}

function createErrorTooltip(html) {
    return createOutFlowIconTooltip("error", "2rem", "red", html, "top");
}

function createIconTooltip(icon, size, color, html, placement) {
    _init_icon_tooltip();

    let elem = document.createElement("div");
    elem.innerHTML = html;

    let template = $("#template-icon-tooltip")[0];
    let i = template.content.querySelector("i");
    // console.log(html);
    // i.setAttribute("data-original-title", html);
    // i.setAttribute("data-placement", placement);
    i.textContent = icon;
    i.style.fontSize = size;
    i.style.color = color;
    let id = makeid(6);
    i.id = id;

    let node = document.importNode(template.content, true);
    setInterval(() => {
        $("#" + id).tooltip({
            html: true,
            title: elem,
            placement: placement,
            content: function (){
                return html;
            }
        });
    });
    return node;
}

function createOutFlowIconTooltip(icon, size, color, message, placement) {
    return createOutFlow(createIconTooltip(icon, size, color, message, placement));
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