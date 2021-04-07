function init_toast() {
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