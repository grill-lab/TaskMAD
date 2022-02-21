var _maxTasksAssigned = 5;
var _listOfTasks = null;
var _tasksRating = {};
var _userId = null;
var _experimentId = null;
var _startTimeOfCurrentTask = (Date.now() / 1000) | 0; // UTC in seconds.
var _currentTask = null;