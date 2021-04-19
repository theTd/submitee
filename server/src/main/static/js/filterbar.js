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
        let removeCallback = props["removeCallback"];
        let selectionFeeder = props["selectionFeeder"];
        let setValueCallback = props["setValueCallback"];
        let datePicker = props["datePicker"];

        let displayValue = value;
        if (selectionFeeder) displayValue = selectionFeeder[value] || displayValue;
        if (datePicker) displayValue = (value ? new Date(value) : new Date()).toLocaleString();

        if (this.map[key]) return;
        this.map[key] = value;

        let node = document.createElement("div");
        node.classList.add("filter-element");
        node.innerHTML = `
<span class="filter-element-title">${displayKey}</span>
<div class="filter-element-value">${displayValue}</div>
<button class="filter-close-button"></button>
`;
        node.querySelector("button").addEventListener("click", () => {
            delete this.map[key];
            node.parentNode.removeChild(node);
            if (removeCallback) removeCallback();
        });

        if (setValueCallback) {
            let editing = false;
            let filterbarMap = this.map;
            $(node).find(".filter-element-value").on("click", function () {
                if (editing) return;
                editing = true;

                let val = filterbarMap[key];
                let valueDiv = $(node).find(".filter-element-value");
                if (selectionFeeder) {
                    let options = "";
                    for (let selectionKey of Object.keys(selectionFeeder)) {
                        displayValue = selectionFeeder[selectionKey] || selectionKey;
                        options += `<option value="${selectionKey}" ${val === selectionKey ? 'selected' : ''}>${displayValue}</option>`;
                    }
                    valueDiv.html(`<form><select>${options}</select></form>`);
                    valueDiv.find("form").on("submit focusout", function (evt) {
                        editing = false;
                        evt.preventDefault();
                        val = valueDiv.find("option:selected").val();
                        filterbarMap[key] = val;
                        displayValue = val || selectionFeeder[val];
                        valueDiv.text(displayValue);
                        setValueCallback(val);
                    });
                } else if (datePicker) {
                    let time = (val ? new Date(val) : new Date());
                    displayValue = time.toLocaleString();
                    let iso = time.toISOString();
                    iso = iso.substr(0, iso.length - 1);
                    valueDiv.html(`<form><input type="datetime-local" value="${iso}"/></form>`);
                    valueDiv.find("form").on("submit focusout", function (evt) {
                        editing = false;
                        evt.preventDefault();
                        val = valueDiv.find("input").val();
                        filterbarMap[key] = new Date(val).getTime();
                        displayValue = new Date(val).toLocaleString();
                        valueDiv.text(displayValue);
                        setValueCallback(val);
                    });
                } else {
                    val = val || "";
                    valueDiv.html(`<form><input type='text' value='${val}'/></form>`);
                    $(node).find("form").on("submit focusout", function (evt) {
                        editing = false;
                        evt.preventDefault();
                        val = valueDiv.find("input").val();
                        filterbarMap[key] = val;
                        if (!val || val === "") {
                            node.parentNode.removeChild(node);
                            if (removeCallback) removeCallback();
                        } else {
                            valueDiv.text(val);
                            setValueCallback(val);
                        }
                        return false;
                    })
                }
            })
        }

        this.element.appendChild(node);
    }

    get(title) {
        return this.map[title];
    }
}
