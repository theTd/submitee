class TextFieldController extends FieldController {
    constructor() {
        super("text");
        this.displayName = "文本";
    }

    /**
     *
     * @param {SField} field
     * @returns {string}
     */
    generateSubmissionHtml(field) {
        let containerId = this.getContainerId(field);
        let inputId = `${containerId}-${field.name}`;
        let placeholder = field.attributeMap.get("placeholder");
        if (!placeholder) placeholder = "";
        return `<input type="text" placeholder="${placeholder}" id="${inputId}"/>`;
    }

    resolveSubmission(field) {
        let containerId = this.getContainerId(field);
        let inputId = `${containerId}-${field.name}`;
        return $("#" + inputId).val();
    }

    generateConfigurationHtml(field) {
        let inputId = "text-config-" + field.name;
        let value = field.attributeMap.get("placeholder") || "";
        return `<label class="w-100" for="${inputId}">提示文字</label><input type="text" id="${inputId}" value="${value}"/>`;
    }

    applyConfiguration(field) {
        let inputId = "text-config-" + field.name;
        field.attributeMap.set("placeholder", $("#" + inputId).val());
    }
}

submitee.fieldControllers['text'] = new TextFieldController();

class AbstractListFieldController extends FieldController {
    generateConfigurationHtml(field) {
        let id = makeid(6);
        let selections = field.attributeMap.get("selections");
        let lst = new class extends EditableCascadedList {
            allowCreate() {
                return true;
            }

            allowEdit() {
                return true;
            }
        }
        if (selections) {
            for (let sel of selections) {
                lst.addSelection(sel);
            }
        }
        setTimeout(() => {
            lst.initList();
            $("#" + id).append(lst.outerContainer);
        })
        field.selectionEditor = lst;
        return `<label class="w-100">编辑可选项</label><div id="${id}" class="card" style="width: fit-content"></div>`;
    }

    applyConfiguration(field) {
        let lst = field.selectionEditor;
        field.attributeMap.set("selections", lst.selections);
    }

    validateConfiguration(field) {
        let values = field.attributeMap.get("selections");
        if (!values || values.length === 0) {
            return "尚未配置可选项目";
        }
    }
}

class RadioFieldController extends AbstractListFieldController {
    constructor() {
        super("radio");
        this.displayName = "单选";
    }

    /**
     *
     * @param {SField} field
     * @returns {string}
     */
    resolveSubmission(field) {
        let containerId = super.getContainerId(field);
        return $(`#${containerId} input[name=${field.name}]:checked`).val();
    }

    /**
     *
     * @param {SField} field
     * @returns {string}
     */
    generateSubmissionHtml(field) {
        let selections = field.attributeMap.get("selections");
        if (!selections) return super.generateConfigurationHtml(field);
        let radioList = "";
        selections.forEach(function (sel) {
            let id = makeid(6);
            radioList += `
<input id="${id}" type="radio" name="${field.name}" value="${sel.name}" />
<label class="ml-1" for="${id}" style="color: ${sel.color}">${sel.name}</label><br/>`;
        })
        return radioList;
    }
}

submitee.fieldControllers['radio'] = new RadioFieldController();

class CheckboxFieldController extends AbstractListFieldController {
    constructor() {
        super("checkbox");
        this.displayName = "多选"
    }

    /**
     *
     * @param {SField} field
     * @returns {string}
     */
    resolveSubmission(field) {
        let containerId = super.getContainerId(field);
        return $(`#${containerId} input[name=${field.name}]:checked`).val();
    }

    /**
     *
     * @param {SField} field
     * @returns {string}
     */
    generateSubmissionHtml(field) {
        let selections = field.attributeMap.get("selections");
        if (!selections) return super.generateConfigurationHtml(field);
        let radioList = "";
        selections.forEach(function (sel) {
            let id = makeid(6);
            radioList += `
<input id="${id}" type="checkbox" name="${field.name}" value="${sel.name}" />
<label class="ml-1" for="${id}" style="color: ${sel.color}">${sel.name}</label><br/>`;
        })
        return radioList;
    }
}

submitee.fieldControllers['checkbox'] = new CheckboxFieldController();

class RichTextFieldController extends FieldController {
    constructor() {
        super("rich-text");
        this.displayName = "富文本"
    }

    async submissionInit() {
        await submitee_safe.loadScriptPromise("https://cdn.jsdelivr.net/npm/wangeditor@latest/dist/wangEditor.min.js");
    }

    generateSubmissionHtml(field) {
        let containerId = this.getContainerId(field);
        let editorId = containerId + "-editor";

        setTimeout(function () {
            let element = document.getElementById(editorId);
            if (!element) return;
            let editor = new window.wangEditor(element);
            editor.config.zIndex = 100;
            editor.config.menus = [
                'bold',
                'fontSize',
                'fontName',
                'italic',
                'underline',
                'strikeThrough',
                'lineHeight',
                'foreColor',
                'backColor',
                'list',
                'justify',
                'code',
            ]
            if (field.attributeMap.get("allow-upload-images")) {
                editor.config.menus.push("image");
                editor.config.uploadImgServer = `../upload/${field.owner.uniqueId}/${field.name}`;
                editor.config.uploadImgHooks = {
                    fail: function (xhr, editor, result) {
                        toast_ajax_error(xhr);
                    },
                    error: function (xhr, editor) {
                        toast_ajax_error(xhr)
                    },
                    timeout: function (xhr, editor) {
                        toast_ajax_error(xhr);
                    },
                    customInsert: function (insertImg, result, editor) {
                        insertImg(result.url)
                    }
                }
            }
            editor.create();
            element.editor = editor;
        }, 1);

        return `<div id="${editorId}"></div>`;
    }

    resolveSubmission(field) {
        let editorId = this.getContainerId(field) + "-editor";
        return document.getElementById(editorId).editor.txt.html();
    }

    generateConfigurationHtml(field) {
        let containerId = this.getContainerId(field);
        let id = makeid(6);

        let storage = field.attributeMap.get("blob-storage");
        let options = submitee.createBlobStorageOptions(storage);

        let allowUploadImages = field.attributeMap.get("allow-upload-images");
        let checked = allowUploadImages ? "checked" : "";
        let collapsed = allowUploadImages ? "" : "collapse";

        setTimeout(() => {
            let checkbox = $("#" + id);
            let collapse = $("#" + containerId).find(".richtext-config-blobstorage");
            checkbox.on("change", () => {
                let allow = checkbox.prop("checked");
                collapse.collapse(allow ? 'show' : 'hide');
            }).bootstrapToggle();
        });
        return `
<div class="row col">
<label class="w-100" for="${id}">允许上传图片</label>
<input type="checkbox" data-size="sm" data-toggle="toggle" data-on="开" data-off="关" id="${id}" ${checked}/>
</div>
<div class="row col ${collapsed} richtext-config-blobstorage" data-toggle="collapse">
<label class="w-100" for="${id}-storage">使用文件存储:</label>
</div>
<div class="row col ${collapsed} richtext-config-blobstorage" data-toggle="collapse">
<select id="${containerId}-storage">${options}</select>
</div>
`;
    }

    applyConfiguration(field) {
        let containerId = this.getContainerId(field);
        let allow = $("#" + containerId).find("input[type=checkbox]").prop("checked");
        let storage = $("#" + containerId + "-storage option:selected").val();
        field.attributeMap.set("allow-upload-images", allow);
        field.attributeMap.set("blob-storage", storage);
    }

    validateConfiguration(field) {
        let allow = field.attributeMap.get("allow-upload-images");
        let storage = field.attributeMap.get("blob-storage");
        if (allow) {
            return submitee.validateBlobStorage(storage);
        }
    }
}

submitee.fieldControllers["rich-text"] = new RichTextFieldController();

class FileFieldController extends FieldController {
    constructor() {
        super("file");
        this.displayName = "文件";
    }

    async submissionInit() {
        submitee.fileTemp = {};
        await new Promise(resolve => {
            submitee_safe.loadAllScript([
                "https://cdn.jsdelivr.net/npm/uppy@1.27.0/dist/uppy.min.js",
                "https://cdn.jsdelivr.net/npm/@uppy/locales@1.18.0/dist/zh_CN.min.js"
            ]).then(resolve)
        })
    }

    /**
     *
     * @param {SField} field
     * @returns {string}
     */
    generateSubmissionHtml(field) {
        let uploadFieldId = this.getContainerId(field) + "-drop-field";

        let maxFiles = field.attributeMap.get("max-files");
        let allowedTypes = field.attributeMap.get("allowed-types");
        if (allowedTypes.length === 0) allowedTypes = ["*/*"];
        setTimeout(function () {
            let uppy = Uppy.Core({
                locale: Uppy.locales.zh_CN,
                restrictions: {
                    maxNumberOfFiles: maxFiles,
                    allowedFileTypes: allowedTypes
                }
            }).use(Uppy.Dashboard, {
                inline: true,
                target: '#' + uploadFieldId
            }).use(Uppy.XHRUpload, {endpoint: `../upload/${field.owner.uniqueId}/${field.name}`})

            uppy.on('complete', (result) => {
                let completed = result['successful'];
                if (completed) {
                    completed.forEach(function (entry) {
                        let key = entry["response"]["body"]["key"];
                        let arr = submitee.fileTemp[field.name];
                        if (!arr) {
                            arr = Array();
                            submitee.fileTemp[field.name] = arr;
                        }
                        arr.push(key);
                    })
                } else {
                    // todo
                    console.log(result);
                }
            })
        }, 1);
        return `<div id="${uploadFieldId}"></div>`;
    }

    resolveSubmission(field) {
        return submitee.fileTemp[field.name];
    }

    generateConfigurationHtml(field) {
        let id = this.getContainerId(field);

        let value = field.attributeMap.get("blob-storage");
        let options = submitee.createBlobStorageOptions(value);

        let maxFiles = field.attributeMap.get("max-files");
        let types = field.attributeMap.get("allowed-types")
        let typesConfig = "";
        if (types) {
            types.forEach(function (val, index) {
                typesConfig += val;
                if (index !== types.length - 1) typesConfig += " ";
            })
        }
        return `
<div class="row col">
<label class="w-100" for="${id}-storage">使用文件存储:</label>
</div>
<div class="row col">
<select id="${id}-storage">${options}</select>
</div>
<div class="row mt-2 col">
<label for="${id}-max-files">最大文件数量</label>
</div>
<div class="row col">
<input type="number" value="${maxFiles}" id="${id}-max-files"/>
</div>
<div class="row mt-2 col">
<label for="${id}-allowed-types">允许文件类型(使用空格分隔)</label>
<p style="color: gray; font-style: italic; font-size: 0.8rem; margin: 0">例: image/* image/jpeg .jpg .jpeg .png .gif</p>
</div>
<div class="row col">
<input type="text" value="${typesConfig}" id="${id}-allowed-types"/>
</div>
`;
    }

    applyConfiguration(field) {
        let id = this.getContainerId(field);

        let storage = $(`#${id}-storage option:selected`).val();
        field.attributeMap.set("blob-storage", storage);

        let max = parseInt($(`#${id}-max-files`).val())
        field.attributeMap.set("max-files", max);

        let typesConfig = $(`#${id}-allowed-types`).val();
        let types = Array();
        typesConfig.split(" ").forEach(function (value) {
            if (value) types.push(value);
        })
        field.attributeMap.set("allowed-types", types);
    }

    validateConfiguration(field) {
        let value = field.attributeMap.get("blob-storage");
        return submitee.validateBlobStorage(value);
    }

    async generateReportHtml(field, value) {
        return new Promise(async (resolve) => {
            let html = "";
            for (let key of value) {
                let meta = await submitee.getFileMeta(key);
                html += `<p style="margin: 0"><a href="../get-file/${key}"><i class="material-icons">link</i>${meta["filename"]} (${this.formatFilesize(meta["size"])})</a></p>`;
            }
            resolve(html);
        });
    }

    formatFilesize(size) {
        let unit = "B";
        if (size > 1024) {
            size /= 1024
            unit = "KiB";
        }
        if (size > 1024) {
            size /= 1024;
            unit = "MiB";
        }
        if (size > 1024) {
            size /= 1024;
            unit = "GiB";
        }
        return new Intl.NumberFormat().format(size) + unit;
    }
}

submitee.fieldControllers["file"] = new FileFieldController();
submitee.createBlobStorageOptions = function (selected, name) {
    let options = `<option name="${name}" value="">---------------</option>`;
    Object.keys(configuration["blob_storages"]).forEach(storageName => {
        let providerName = configuration["blob_storage_providers"][configuration["blob_storages"][storageName]['provider']];
        let sel = selected === storageName ? "selected" : ""
        options += `<option name="${name}" value="${storageName}" ${sel}>${storageName} (${providerName})</option>`;
    });
    return options;
}

submitee.validateBlobStorage = function (value) {
    if (!value) {
        return "未配置文件存储";
    }
    if (!configuration["blob_storages"][value]) {
        return "配置的文件存储已不再可用";
    }
    if (configuration["blob_storage_errors"]) {
        if (configuration["blob_storage_errors"][value]) {
            return "配置的文件存储存在错误: " + configuration["blob_storage_errors"][value];
        }
    }
}

class CascadedListFieldController extends FieldController {
    constructor() {
        super("cascaded-list")
        this.displayName = "级联选择器"
    }

    resolveSubmission(field) {
        return field.selectedPath;
    }

    generateSubmissionHtml(field) {
        let containerId = this.getContainerId(field);
        setTimeout(() => {
            let renderFn = this.renderSelection;
            let updateFn = function () {
                let path = field.selectedPath;
                if (!path) {
                    $("#" + containerId).find("button").text("开始选择");
                } else {
                    $("#" + containerId).find("button").html(renderFn(field.selectedPath));
                }
            }
            $("#" + containerId).find("button").on("click", () => {
                let dataRoot = field.attributeMap.get("selections");
                let clz = class extends EditableCascadedList {
                    constructor(path) {
                        super();
                        this.path = path;
                    }

                    haveCascadeList(selection) {
                        return selection["child"];
                    }

                    getPathOfSelection(selection) {
                        let trim = {};
                        Object.assign(trim, selection);
                        delete trim["child"];
                        let path = this.path ? JSON.parse(JSON.stringify(this.path)) : Array();
                        path.push(trim);
                        return path;
                    }

                    getCascadeList(selection) {
                        return createFn(selection["child"], this.getPathOfSelection(selection));
                    }

                    onFinalSelect(selection) {
                        field.selectedPath = this.getPathOfSelection(selection);
                        updateFn();
                        rootList.close();
                    }
                }
                let createFn = function (data, path) {
                    let l = new clz(path);
                    for (let d of data) {
                        l.addSelection(d);
                    }
                    return l;
                }
                let rootList = createFn(dataRoot, null);
                rootList.show("#" + containerId + " button", "top");
                field.selectionEditor = rootList;
            });
            updateFn();
        });
        return `<button type="button" class="btn btn-outline-primary" style="min-width: 10rem"></button>`;
    }

    generateConfigurationHtml(field) {
        let containerId = this.getContainerId(field);
        setTimeout(() => {
            let dataRoot = field.attributeMap.get("selections");
            if (!dataRoot) dataRoot = [];
            field.dataRoot = dataRoot;

            $("#" + containerId).find("button").on("click", () => {
                let clz = class extends EditableCascadedList {
                    constructor(dataList) {
                        super();
                        this.dataList = dataList;
                    }

                    allowCreate() {
                        return true;
                    }

                    allowSort() {
                        return true;
                    }

                    allowEdit() {
                        return true;
                    }

                    allowCreateCascadeList() {
                        return true;
                    }

                    haveCascadeList(selection) {
                        return selection["child"];
                    }

                    getCascadeList(selection) {
                        let d = this.dataList[this.findIdx(selection)];
                        let child = d["child"]
                        if (!child) {
                            child = [];
                            d["child"] = child;
                        }
                        return createFn(child);
                    }

                    close() {
                        // field.dataRoot = this.dataList;
                        console.log(field.dataRoot);
                        super.close();
                    }

                    findIdx(selection) {
                        for (let i = 0; i < this.dataList.length; i++) {
                            if (this.dataList[i].name === selection.name) {
                                return i;
                            }
                        }
                    }

                    onEdit(selection, newSelection) {
                        this.dataList.splice(this.findIdx(selection), 1, newSelection);
                    }

                    onDelete(selection) {
                        this.dataList.splice(this.findIdx(selection), 1);
                    }

                    onCreate(selection) {
                        this.dataList.push(selection);
                    }

                    onMoveUp(selection) {
                        let idx = this.findIdx(selection);
                        let newIdx = idx - 1;
                        let rm = this.dataList.splice(idx, 1)[0];
                        this.dataList.splice(newIdx, 0, rm);
                    }

                    onMoveDown(selection) {
                        let idx = this.findIdx(selection);
                        let newIdx = idx + 1;
                        let rm = this.dataList.splice(idx, 1)[0];
                        this.dataList.splice(newIdx, 0, rm);
                    }
                }
                let createFn = function (data) {
                    let l = new clz(data);
                    for (let d of data) {
                        l.addSelection(d);
                    }
                    return l;
                }
                let rootList = createFn(field.dataRoot);
                rootList.show("#" + containerId + " button", "bottom");
            });
        });
        return `<button type="button" class="btn btn-primary">编辑</button>`;
    }

    applyConfiguration(field) {
        field.attributeMap.set("selections", field.dataRoot);
    }

    validateConfiguration(field) {
        let selections = field.attributeMap.get("selections");
        if (!selections || selections.length === 0) {
            return "未配置可选项";
        }
    }

    renderSelection(path) {
        if (!path || path.length === 0) return "";
        let display = "";
        for (let i = 0; i < path.length; i++) {
            let p = path[i];
            display += `<span class='ml-1 mr-1' style='color: ${p.color}'>${p.name}</span>`;
            if (i !== path.length - 1) {
                display += "<span style='color: #007bff; font-weight: bold'>&gt;</span>";
            }
        }
        return display;
    }

    generateReportHtml(field, value) {
        return this.renderSelection(value);
    }
}

submitee.fieldControllers["cascaded-list"] = new CascadedListFieldController();
