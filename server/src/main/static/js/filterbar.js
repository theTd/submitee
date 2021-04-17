function createFilterBar(selector) {
    let element = document.querySelector(selector);
    element.classList.add("filter-container");
    return new FilterBarHandle(element);
}

class FilterBarHandle {
    constructor(element) {
        this.element = element;
        this.map = {}
    }

    add(title, value, removeCallback) {
        if (this.map[title]) return;
        this.map[title] = value;

        let node = document.createElement("div");
        node.classList.add("filter-element");
        node.innerHTML = `<span class="filter-element-title">${title}</span>
            <div class="filter-element-value">${value}</div>
            <button class="filter-element-button">
                <i class="material-icons">close</i>
            </button>`;
        node.querySelector("button").addEventListener("click", () => {
            delete this.map[title];
            node.parentNode.removeChild(node);
            removeCallback();
        });
        this.element.appendChild(node);
    }

    get(title) {
        return this.map[title];
    }
}