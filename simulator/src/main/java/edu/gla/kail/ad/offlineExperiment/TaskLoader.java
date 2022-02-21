package edu.gla.kail.ad.offlineExperiment;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.protobuf.Timestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Loads available tasks from the Firestore Database.
 */
class TaskLoader {
    private Firestore _database;
    private String _userId;

    /**
     * Load tasks from the database for a particular user, for a particular experiment.
     */
    String loadTasks(Firestore database, String userId, Integer maxNumberOfTasksAssigned, String
            experimentId) {
        _database = database;
        _userId = userId;
        JsonObject json = new JsonObject();

        DocumentReference userDocRef = _database.collection("clientWebSimulator").document
                ("agent-dialogue-experiments").collection("users").document(_userId);
        try {
            DocumentSnapshot user = userDocRef.get().get();
            if (user.exists()) {
                Map<String, Object> userData = user.getData();
                ArrayList<String> listOfOpenTaskIds;

                if (userData.containsKey("openTaskIds")) {
                    listOfOpenTaskIds = (ArrayList<String>) userData.get("openTaskIds");
                } else {
                    listOfOpenTaskIds = new ArrayList<>();
                }
                int numberOfOpenRatings = listOfOpenTaskIds.size();
                ArrayList<String> listOfCompletedTaskIds;

                if (userData.containsKey("completedTaskIds")) {
                    listOfCompletedTaskIds = (ArrayList<String>) userData.get("completedTaskIds");
                } else {
                    listOfCompletedTaskIds = new ArrayList<>();
                }
                HashSet allAssignedTaskIds = new HashSet(listOfCompletedTaskIds);

                allAssignedTaskIds.addAll(listOfOpenTaskIds);
                // Assign more tasks to the user if the user can have more open tasks and there
                // are some more available.
                if (numberOfOpenRatings <= maxNumberOfTasksAssigned) {
                    listOfOpenTaskIds = assignMoreTasksToUser(allAssignedTaskIds,
                            listOfOpenTaskIds, maxNumberOfTasksAssigned, numberOfOpenRatings,
                            userDocRef, experimentId);
                }
                json.addProperty("tasks", getOpenTasks(listOfOpenTaskIds));
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return json.toString();
    }

    /**
     * Called to assign more tasks to the user if possible:
     * if there are more existing tasks in the database that fulfill certain criteria.
     */
    private ArrayList<String> assignMoreTasksToUser(HashSet<String> allAssignedTaskIds,
                                                    ArrayList<String> listOfOpenTaskIds, Integer
                                                            maxNumberOfTasksAssigned, Integer
                                                            numberOfOpenRatings,
                                                    DocumentReference userDocRef, String
                                                            experimentId) throws
            ExecutionException, InterruptedException {
        HashSet<String> remainingAvailableTasks = new HashSet<>();

        // Get all candidate tasks.
        ApiFuture<QuerySnapshot> tasksFuture = _database.collection
                ("clientWebSimulator").document("agent-dialogue-experiments")
                .collection("tasks").whereEqualTo("experimentId", experimentId).get();
        List<QueryDocumentSnapshot> tasks = tasksFuture.get().getDocuments();
        for (DocumentSnapshot task : tasks) {
            remainingAvailableTasks.add((String) task.get("taskId"));
        }
        // Leave only not completed and not open tasks.
        remainingAvailableTasks.removeAll(allAssignedTaskIds);

        for (int i = maxNumberOfTasksAssigned; i > numberOfOpenRatings; i--) {
            // Check if we can create any more ratings.
            if (remainingAvailableTasks.size() <= 0) {
                break;
            }
            String createdTaskId = remainingAvailableTasks.iterator().next();
            listOfOpenTaskIds.add(createdTaskId);

            // Create open rating for this user.
            String createdRatingId = createNewOpenRating(createdTaskId);

            // Add this ratingId to ratingIds list in the Task Document.
            updateTaskDocument(createdTaskId, createdRatingId);

            remainingAvailableTasks.remove(createdTaskId);
        }

        // Add this taskId to openTaskIds list in the User Document.
        Map<String, Object> helperMap = new HashMap<>();
        helperMap.put("openTaskIds", listOfOpenTaskIds);
        userDocRef.update(helperMap);
        return listOfOpenTaskIds;
    }

    /**
     * Add the rating ID to the task entry in the database, for a newly assigned rating task.
     */
    private void updateTaskDocument(String taskId, String ratingId) throws ExecutionException,
            InterruptedException {
        DocumentReference taskDocRef = _database.collection("clientWebSimulator").document
                ("agent-dialogue-experiments").collection("tasks").document(taskId);
        Map<String, Object> taskData = taskDocRef.get().get().getData();
        ArrayList<String> ratingIds;
        if (taskData.containsKey("ratingIds")) {
            ratingIds = (ArrayList<String>) taskData.get("ratingIds");
        } else {
            ratingIds = new ArrayList<>();
        }
        ratingIds.add(ratingId);
        HashMap<String, Object> helperMap = new HashMap<>();
        helperMap.put("ratingIds", ratingIds);
        Long numberOfRemainingRatings = (Long) taskData.get("numberOfRemainingRatings");
        numberOfRemainingRatings -= 1;
        helperMap.put("numberOfRemainingRatings", numberOfRemainingRatings);
        taskDocRef.update(helperMap);
    }

    /**
     * Create a new open rating for the particular task. for a particular user.
     */
    private String createNewOpenRating(String taskId) throws ExecutionException,
            InterruptedException {
        String ratingId = taskId + "_" + _userId;
        DocumentReference createdRatingDocRef = _database.collection
                ("clientWebSimulator").document("agent-dialogue-experiments")
                .collection("ratings").document(ratingId);
        Map<String, Object> data = new HashMap<>();
        Timestamp timeNow = getTimeStamp();
        data.put("ratingId", ratingId);
        data.put("assignTime_nanos", timeNow.getNanos());
        data.put("assignTime_seconds", timeNow.getSeconds());
        data.put("complete", false);
        data.put("taskId", taskId);
        data.put("userId", _userId);
        data.put("experimentId", _database.collection
                ("clientWebSimulator").document("agent-dialogue-experiments")
                .collection("tasks").document(taskId)
                .get().get().getData().get("experimentId"));
        createdRatingDocRef.set(data);
        return ratingId;
    }

    /**
     * Return the list of all open tasks - the tasks assigned to the user that haven't been
     * completed yet.
     */
    private String getOpenTasks(ArrayList<String> listOfOpenTaskIds) throws ExecutionException,
            InterruptedException {
        JsonObject jsonOfTasks = new JsonObject();
        for (Integer taskIndex = 0; taskIndex < listOfOpenTaskIds.size(); taskIndex++) {
            String taskId = listOfOpenTaskIds.get(taskIndex);
            Map<String, Object> taskMap = _database.collection("clientWebSimulator")
                    .document("agent-dialogue-experiments").collection("tasks")
                    .document(taskId).get().get().getData();
            JsonObject taskJson = new JsonObject();
            taskJson.addProperty("clientId", (String) taskMap.get("clientId"));
            taskJson.addProperty("deviceType", (String) taskMap.get("deviceType"));
            taskJson.addProperty("taskId", (String) taskMap.get("taskId"));
            taskJson.addProperty("language_code", (String) taskMap.get("language_code"));
            JsonObject jsonTurns = new JsonObject();
            ArrayList<Object> taskTurns = ((ArrayList<Object>) taskMap.get("turns"));
            for (Integer turnIndex = 0; turnIndex < taskTurns.size(); turnIndex++) {
                jsonTurns.addProperty(turnIndex.toString(), (new Gson().toJson(taskTurns
                        .get(turnIndex))));
            }
            taskJson.addProperty("turns", jsonTurns.toString());
            jsonOfTasks.addProperty(taskIndex.toString(), taskJson.toString());
        }
        return jsonOfTasks.toString();
    }

    private Timestamp getTimeStamp() {
        return Timestamp.newBuilder()
                .setSeconds(Instant.now()
                        .getEpochSecond())
                .setNanos(Instant.now()
                        .getNano())
                .build();
    }
}
