var _userValid = false;

function addUserToUrl() {
    let basicUrl = (new URL(document.location)).origin + (new URL(document.location)).pathname;
    userId = document.getElementById("userId").value;
    window.location.replace(basicUrl + "?user=" + userId + "&conversation=" + _conversationId);
}

function chooseConversation() {
    let basicUrl = (new URL(document.location)).origin + (new URL(document.location)).pathname;
    conversationId = document.getElementById("conversationId").value;
    window.location.replace(basicUrl + "?user=" + _userId + "&conversation=" + conversationId);
}

function loadConversation(_userId, _conversationId) {
    _userValid = true;
}


/**
 * Send message request to servlet when the Submit button is pressed.
 * Use AJAX.
 */
function sendRequest() {
    // TODO: only if the userid and conversation id are specified
    if (_userId === "null" || _userId === null || _conversationId === "null"
        || _conversationId === null || _userValid === false) {
        alert("User ID and Conversation ID not specified. Pleas, fill them first.")
        return false;
    }

    let textInput = $('textarea#message').val();
    let language = "en-US"; // TODO: Push language onto agent specific parameters.
    let createRatingBool = $('input[name=rating-enabled]:checked', '#rating-options-form').val();
    $('textarea#message').val("");
    $("#conversation-panel").append($('<div id="request"/>').append(textInput));
    agent_request_parameters = {
        "conversationId": _conversationId
    };
    $.ajax({
        url: "ad-client-service-servlet",
        type: 'POST',
        headers: {"Operation": "sendRequest"},
        dataType: 'json',
        data: {
            textInput: textInput,
            language: language,
            userId: _userId,
            chosen_agents: "WizardOfOz",
            agent_request_parameters: JSON.stringify(agent_request_parameters)
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
        }
    });
}