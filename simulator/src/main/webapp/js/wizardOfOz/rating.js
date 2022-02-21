function updateRating(score, responseId) {
    let $ratingDiv = $("#" + responseId.toString() + ".rating");
    $.ajax({
        url: "ad-client-service-servlet",
        type: 'POST',
        headers: {"operation": "updateRating"},
        dataType: 'text',
        data: {
            ratingScore: score,
            responseId: responseId,
            // TODO(Adam): Delete experimentId and requestId.
            experimentId: "from_wizard_oz_interface",
            requestId: "",
            userId: _userId
        },
        success: function () {
            $ratingDiv.find('img[id="rating-indicator"]')[0].src = '../resources/img/check-solid.svg';
        },
        error: function (data, status, error) {
            alert("Error data: " + data + "\nStatus: " + status + "\nError message:" + error);
        },
    });
}

function createRating(responseId) {
    $output = $("#conversation-panel");
    $output.append($('<div class = "rating" id = \"' + responseId + '\"><div class="slider" id = \"' + responseId + '\"/><div>'));
    $rating = $("#" + responseId + ".rating");
    $slider = $("#" + responseId + ".slider");
    $slider.slider({
        value: 1,
        min: 1,
        max: 5,
        step: 1,
        id: responseId,
        slide: function (event, ui) {
            updateRating(ui.value, responseId);
        }
    })
        .each(function () {
            let opt = $(this).data().uiSlider.options;
            let vals = opt.max - opt.min;
            for (let i = 0; i <= vals; i++) {
                let $label = $('<label>' + _ratingValues[i] + '</label>').css('left', (i / vals * 100) + '%');
                $slider.append($label);
            }
        });
    $rating.append("<img id='rating-indicator' src='../../resources/img/question-circle-solid.svg' />");
    $output.append($('<br>'));
}

function validateUser() {
    let userValidity = true;
    $.ajax({
        url: "offline-mt-ranking-servlet",
        type: 'POST',
        headers: {"operation": "validateUser"},
        dataType: 'text',
        data: {
            userId: _userId
        },
        success: function (response) {
            if (response == "false") {
                alert("The User Id: " + _userId + " is invalid.");
                $('.user-details-form').append($("<div>").text("INVALID USER ID"));
                userValidity = false
            }
        },
        error: function (data, status, error) {
            alert("Error data: " + data + "\nStatus: " + status + "\nError message:" + error);
            userValidity = false;
        },
    });
    return userValidity;
}