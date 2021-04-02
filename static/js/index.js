function test_result() {
    let iframes = $(".resize-iframe");
    let size = iframes.length;

    let report = "";
    iframes.each(function (index) {
        let f = iframes[index];
        if (!f.id) {
            console.warn("element " + f + " marked as submitee-iframe but does not defined id");
            return;
        }
        if (typeof iframes[index].contentWindow.submiteeFeedResult === 'function') {
            let result = iframes[index].contentWindow.submiteeFeedResult();
            if (Array.isArray(result) && result.length === 0) result = undefined;

            if (result) {
                report += "[i] " + f.id + ": " + result;
            } else {
                report += "[x] " + f.id;
            }
        } else {
            console.warn("element " + f.id +
                " marked as submitee-iframe but does not contains submiteeFeedResult() function")
            return;
        }
        if (index !== size - 1) {
            report += "<br/>";
        }
    });

    if (!report) {
        $("#test_result_output").html("contents are empty");
    } else {
        $("#test_result_output").html(report);
    }
}

function resizeIframe(iframe) {
    iframe.scrolling = 'no';
    $(iframe).css("height", iframe.contentDocument.body.scrollHeight + 'px')
}

let fields = $('.resize-iframe');

fields.on('load', function (event) {
    resizeIframe(event.target);
})

fields.each(function (index) {
    if (fields[index].loaded) {
        resizeIframe();
    }
})
