function init_toast() {
    if ($("#template-toast")[0]) return;

    let template = document.createElement("template");
    template.id = "template-toast";
    template.innerHTML = `
<div class="toast" style="position: absolute; top: 0; right: 0;">
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
    container.style.zIndex = '999'
    document.body.appendChild(container);
}

function create_toast(title, content, delay) {
    init_toast();

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

function toast_ajax_error(error) {
    if (error.responseText) {
        create_toast("重要提示", error.responseText, 10000)
    } else {
        create_toast("重要提示", error.statusText, 10000)
    }
}

function init_tooltip() {
    if ($("#template-error-tooltip")[0]) return;

    // region load css
    let link = document.createElement('link');
    link.rel = 'stylesheet';
    link.href = 'https://fonts.googleapis.com/icon?family=Material+Icons';
    let head = document.getElementsByTagName('head')[0];
    head.appendChild(link);
    console.log("loading material icon stylesheet");
    // endregion

    let template = document.createElement("template");
    template.id = "template-error-tooltip";
    template.innerHTML = `
<span class="position-relative error-tooltip">
    <span class="position-absolute align-middle">
        <i data-toggle="tooltip" data-placement="top" title="测试"
           class="material-icons" style="font-size:2rem;color:red">error</i>
    </span>
</span>`;

    document.body.appendChild(template);
}

function createErrorTooltip(message) {
    init_tooltip();
    let template = $("#template-error-tooltip")[0];
    template.content.querySelector("i").setAttribute("title", message);
    let node = document.importNode(template.content, true);
    setInterval(() => {
        $(".error-tooltip").find("i").tooltip();
    }, 1);
    return node;
}
