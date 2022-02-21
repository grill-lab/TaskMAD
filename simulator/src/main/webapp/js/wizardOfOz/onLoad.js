/**
 * Do on loading HTML.
 */
$(document).ready(function () {
    $('#userId').keypress(function (keyPressed) {
        if (keyPressed.which == 13) {
            keyPressed.preventDefault();
            addUserToUrl();
        }
    });

    $('#conversationId').keypress(function (keyPressed) {
        if (keyPressed.which == 13) {
            keyPressed.preventDefault();
            chooseConversation();
        }
    });

    // When user presses "enter" in textarea, the request is being sent.
    $('#message').keypress(function(keyPressed){
        if(keyPressed.which == 13 && !keyPressed.shiftKey){
            keyPressed.preventDefault();
            sendRequest();
        }
    });

    _userId = (new URL(document.location)).searchParams.get("user");
    _conversationId = (new URL(document.location)).searchParams.get("conversation");

    if (_userId != null) {
        document.getElementById("userId").value = _userId;
        document.getElementById("conversationId").value = _conversationId;
        $('#user-submit-button').text("Change username");
        if (_conversationId != null && _conversationId !== "null" && _conversationId != "null") {
            if (validateUser() == true) {
                loadConversation(_userId, _conversationId);
            } else {
                _userValid = false;
            }
        } else {
            alert("Specify the conversation ID to be able to participate!");
        }
    }
});