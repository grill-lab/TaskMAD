var _awaitingResponses = 0;

/**
 * Send message request to servlet when the Submit button is pressed.
 * Use AJAX.
 */
function sendRequest() {
    let textInput = $('textarea#message').val();
    let language = $('input[name=language]:checked', '#language-form').val();
    let createRatingBool = $('input[name=rating-enabled]:checked', '#rating-options-form').val();
    $('textarea#message').val("");
    $("#conversation-panel").append($('<div id="request"/>').append(textInput));
    _awaitingResponses += 1;
    $('#awaiting-responses').text(_awaitingResponses);
    $.ajax({
        url: "ad-client-service-servlet",
        type: 'POST',
        headers: {"Operation": "sendRequest"},
        dataType: 'json',

        data: {
            textInput: textInput,
            language: language,
            agent_request_parameters: "",
            chosen_agents: _listOfOnlineAgents.toString(),
            userId: "SentFromIndexWebsite"
        },
        success: function (response) {
            $("#conversation-panel").append($('<div id="response">-  </div>')
                .append(response.message)
            );
            if (createRatingBool == "true") {
                createRating(response.responseId);
            }
            requestDetails = response.interactionRequest;
            responseDetails = response.interactionResponse;
        },
        error: function (data, status, error) {
            alert("Error data: " + data + "\nStatus: " + status + "\nError message:" + error);
        },
        complete: function () {
            _awaitingResponses -= 1;
            $('#awaiting-responses').text(_awaitingResponses);
        }
    });
}