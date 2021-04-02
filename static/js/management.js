$.ajax({
    url: "edit-template.html",
    success: function (data) {
        $("#management-container").html(data);
    }
});
