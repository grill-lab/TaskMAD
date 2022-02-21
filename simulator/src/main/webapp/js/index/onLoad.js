let _LANGUAGES = {
    AMERICAN_ENGLISH: {value: "en-US", name: "American English"},
    BRITISH_ENGLISH: {value: "en-UK", name: "British English"},
};

/**
 * Do on loading HTML.
 */
$(document).ready(function () {
    populateLanguages();
    $('#awaiting-responses').text(0);

    // When user presses "enter" in textarea, the request is being sent.
    $('#message').keypress(function(keyPressed){
        if(keyPressed.which == 13 && !keyPressed.shiftKey){
            keyPressed.preventDefault();
            sendRequest();
        }
    });
});

// Populate languages from _LANGUAGES dictionary.
function populateLanguages() {
    let $languageFieldset = $('<form id = "language-form">').append("<h5><legend>Language:</legend></h5>");

    for (let language in _LANGUAGES) {
        let parameters = _LANGUAGES[language];
        let $label = $("<label for=" + parameters.value + ">").text(parameters.name);
        let $input = $('<input type="radio" name="language">').attr({
            id: parameters.value,
            value: parameters.value
        });

        $languageFieldset.append($label)
            .append($input);

        $('.options').append($languageFieldset);
    }
    $("#" + _LANGUAGES.AMERICAN_ENGLISH.value).prop("checked", true);
}

