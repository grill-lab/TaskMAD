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
            experimentId: "sent_from_index_servlet",
            requestId: "",
            userId: "SentFromIndexWebsite"
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