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

class RadioFieldController extends FieldController {
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
        let values = field.attributeMap.get("values");

        if (!values) {
            return `<div class="alert alert-warning">存在问题的字段: ${field.name}</div>`;
        }

        let radioList = "";
        let containerId = this.getContainerId(field);
        values.forEach(function (val) {
            let radioId = `${containerId}-${val}`;
            radioList += `<input id="${radioId}" type="radio" name="${field.name}" value="${val}" /><label class="ml-1" for="${radioId}">${val}</label><div class="w-100"></div>`;
        })
        return radioList;
    }

    generateConfigurationHtml(field) {
        let id = "radio-conf-" + field.name;

        let values = field.attributeMap.get("values");
        let present = "";
        if (values) {
            values.forEach(function (val, index) {
                present += val;
                if (index !== values.length - 1) present += " ";
            })
        }

        return `
<label class="w-100" for="${id}">可选项目： (使用空格分隔)</label>
<input id="${id}" type="text" id="radio-conf-${field.name}" value="${present}"/>
`;
    }

    applyConfiguration(field) {
        let id = "radio-conf-" + field.name;
        let valuesString = $("#" + id).val();
        let values = Array();
        valuesString.split(" ").forEach(value => {
            if (value) values.push(value);
        });
        field.attributeMap.set("values", values);
    }

    validateConfiguration(field) {
        let values = field.attributeMap.get("values");
        if (!values || values.length === 0) {
            return "尚未配置可选项目";
        }
    }
}

submitee.fieldControllers['radio'] = new RadioFieldController();

class CheckboxFieldController extends FieldController {
    constructor() {
        super("checkbox");
        this.displayName = "多选"
    }

    generateSubmissionHtml(field) {
        let values = field.attributeMap.get("values");

        if (!values) {
            return `<div class="alert alert-warning">存在问题的字段: ${field.name}</div>`;
        }

        let checkboxList = "";
        let containerId = this.getContainerId(field);
        values.forEach(function (val) {
            let checkboxId = `${containerId}-${val}`;
            checkboxList += `<input id="${checkboxId}" type="checkbox" name="${field.name}" value="${val}" /><label class="ml-1" for="${checkboxId}">${val}</label><div class="w-100"></div>`;
        })
        return checkboxList;
    }

    resolveSubmission(field) {
        let containerId = super.getContainerId(field);
        let checked = $(`#${containerId} input[name=${field.name}]:checked`);

        let array = Array();
        for (let val of checked) {
            array.push(val.value);
        }

        return array;
    }

    generateConfigurationHtml(field) {
        let id = "checkbox-conf-" + field.name;

        let values = field.attributeMap.get("values");
        let present = "";
        if (values) {
            values.forEach(function (val, index) {
                present += val;
                if (index !== values.length - 1) present += " ";
            })
        }

        return `
<label class="w-100" for="${id}">可选项目： (使用空格分隔)</label>
<input id="${id}" type="text" id="checkbox-conf-${field.name}" value="${present}"/>
`;
    }

    applyConfiguration(field) {
        let id = "checkbox-conf-" + field.name;
        let valuesString = $("#" + id).val();
        let values = Array();
        valuesString.split(" ").forEach(value => {
            if (value) values.push(value);
        });
        field.attributeMap.set("values", values);
    }

    validateConfiguration(field) {
        let values = field.attributeMap.get("values");
        if (!values || values.length === 0) {
            return "尚未配置可选项目";
        }
    }
}

submitee.fieldControllers['checkbox'] = new CheckboxFieldController();

class RichTextFieldController extends FieldController {
    constructor() {
        super("rich-text");
        this.displayName = "富文本"
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
