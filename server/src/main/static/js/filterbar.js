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

    add(props) {
        let key = props["key"];
        let displayKey = props["displayKey"] || key;
        let value = props["value"];
        let displayValue = props["displayValue"] || value;
        let removeCallback = props["removeCallback"];


        if (this.map[key]) return;
        this.map[key] = value;

        let node = document.createElement("div");
        node.classList.add("filter-element");
        node.innerHTML = `
<span class="filter-element-title">${displayKey}</span>
<div class="filter-element-value">${displayValue}</div>
<button class="close-button"></button>
`;
        node.querySelector("button").addEventListener("click", () => {
            delete this.map[key];
            node.parentNode.removeChild(node);
            if (removeCallback) removeCallback();
        });
        this.element.appendChild(node);
    }

    get(title) {
        return this.map[title];
    }
}