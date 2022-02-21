function redirectToUserPage() {
    let basicUrl = (new URL(document.location)).origin + (new URL(document.location)).pathname;
    userId = document.getElementById("user").value;
    window.location.replace(basicUrl + "?user=" + userId + "&experiment=" + _experimentId);
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


function loadTasks(_userId) {
    _tasksRating = {};
    $.ajax({
        url: "offline-mt-ranking-servlet",
        type: 'POST',
        headers: {"operation": "loadTasks"},
        dataType: 'json',
        data: {
            userId: _userId,
            maxTasksAssigned: _maxTasksAssigned,
            experimentId: _experimentId
        },
        success: function (response) {
            $("#tasks-buttons").empty();
            _listOfTasks = JSON.parse(response.tasks);
            let $tasks_list = $('.tasks-list');
            $tasks_list.empty();
            for (let i = 0; i < Object.keys(_listOfTasks).length; i++) {
                let $task = $("<a class = 'task-a-element' id = \'" + i + "\' onclick=\'showTaskWithNumber(" + i + ")\'>" +
                    "Task " + (i + 1) + " <img id='tasks-indicator' " +
                    "src='../../resources/img/question-circle-solid.svg' /></a>");
                $tasks_list.append($task).append("<br>");
                _tasksRating[i] = 0;
            }
            $("#tasks-buttons").append("<button id = 'next-batch-button' class = 'submit-button' " +
                "type = 'button' onclick = \'loadTasks(\"" + _userId + "\")\'>Next Batch</button>");
            if (Object.keys(_listOfTasks).length > 0) {
                showTaskWithNumber(0);
            } else {
                $tasks_list.append("<a>The are no more available tasks in the database.</a>");
            }
        },
        error: function (data, status, error) {
            alert("Error data: " + data + "\nStatus: " + status + "\nError message:" + error);
        },
    });
}

function showTaskWithNumber(taskNumber) {
    _startTimeOfCurrentTask = getCurrentTime();
    _currentTask = JSON.parse(_listOfTasks[taskNumber]);
    let $current_task_details = $("#current-task-details");
    $current_task_details.empty();
    $current_task_details.append("<div>Client ID: " + _currentTask.clientId + "</div>");
    $current_task_details.append("<div>Device type: " + _currentTask.deviceType + "</div>");
    $current_task_details.append("<div>Language code: " + _currentTask.language_code + "</div>");
    let $rating_interface_block = $("#conversation-panel");
    $rating_interface_block.empty();
    let turns = JSON.parse(_currentTask.turns);
    for (let i in Object.keys(turns)) {
        let turn = JSON.parse(turns[i]);
        if (turn.request != null) {
            $rating_interface_block.append($('<div id="request"/>')
                .append("<i class = 'date-utterance'>" + new Date(turn.requestTime_seconds * 1000)
                    .toLocaleString() + "</i>" + "<br>" + turn.request));
        }
        if (turn.response != null) {
            $rating_interface_block.append($('<div id="response"/>')
                .append("<i class = 'date-utterance'>" + new Date(turn.responseTime_seconds * 1000)
                    .toLocaleString() + "</i>" + "<br>" + turn.response));
        }
    }
    createMtRating(taskNumber);
    if (taskNumber + 1 < Object.keys(_listOfTasks).length) {
        addNextTaskButton(taskNumber + 1);
    }
}

function addNextTaskButton(taskNumber) {
    let $nextTaskButton = $("<button id = 'next-task-button' class = 'submit-button' type = 'button' " +
        "onclick = 'showTaskWithNumber(" + taskNumber + ")'>Next Task</button>");
    $("#conversation-panel").append($nextTaskButton);
}

function rateTask(taskNumber, score) {
    let $ratingDiv = $("#current-rating");
    $.ajax({
        url: "offline-mt-ranking-servlet",
        type: 'POST',
        headers: {"operation": "rateTask"},
        dataType: 'text',
        data: {
            startTime_seconds: _startTimeOfCurrentTask,
            endTime_seconds: getCurrentTime(),
            ratingScore: score,
            userId: _userId,
            taskId: _currentTask.taskId
        },
        success: function () {
            _tasksRating[taskNumber] = score;
            $(".tasks-list").find('a[id="' + taskNumber + '"]')
                .find('img[id="tasks-indicator"]')[0].src = '../resources/img/check-solid.svg';
            $ratingDiv.find('img[id="rating-indicator"]')[0].src = '../resources/img/check-solid.svg';
        },
        error: function (data, status, error) {
            alert("Error data: " + data + "\nStatus: " + status + "\nError message:" + error);
        },
    });
}

function createMtRating(taskNumber) {
    let rating = _tasksRating[taskNumber];
    $ratingBlock = $("#conversation-panel");
    $ratingBlock.append($('<br><div class = "rating-slider-section"><div id="slider">'));

    $('#slider').slider({
        value: rating,
        min: 1,
        max: 5,
        step: 1,
        id: "rating-slider",
        slide: function (event, ui) {
            rateTask(taskNumber, ui.value);
        }
    })
        .each(function () {
            let opt = $(this).data().uiSlider.options;
            let vals = opt.max - opt.min;
            for (let i = 0; i <= vals; i++) {
                let $label = $('<label>' + _ratingValues[i] + '</label>').css('left', (i / vals * 100) + '%');
                $("#slider").append($label);
            }
        });
    $ratingBlock.append($('<br>'));
}

function getCurrentTime() {
    return ((Date.now() / 1000) | 0);
}