package edu.gla.kail.ad.offlineExperiment;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Used for storing user rating in the Database and in Logs.
 */
class TaskRater {
    private Firestore _database;

    /**
     * Update the Firestore entry for the open rating and assign the score and time of the rating.
     * Mark rating as completed.
     */
    void rateTask(Firestore database, String userId, Integer ratingScore, String taskId, Long
            startTime_seconds, Long endTime_seconds) {
        _database = database;
        // Update rating in the ratings collection.
        String ratingId = taskId + "_" + userId;
        DocumentReference ratingDocRef = _database.collection("clientWebSimulator").document
                ("agent-dialogue-experiments").collection("ratings").document(ratingId);
        Map<String, Object> updateHelperMap = new HashMap<>();
        updateHelperMap.put("ratingScore", ratingScore);
        updateHelperMap.put("startTime_seconds", startTime_seconds);
        updateHelperMap.put("endTime_seconds", endTime_seconds);
        updateHelperMap.put("totalTime_seconds", endTime_seconds - startTime_seconds);
        updateHelperMap.put("complete", true);
        ratingDocRef.update(updateHelperMap);

        // Update user database - remove task ID from list of open tasks in User document.
        // Add rated task to the list of completed tasks.
        DocumentReference userDocRef = _database.collection("clientWebSimulator").document
                ("agent-dialogue-experiments").collection("users").document(userId);
        try {
            Map<String, Object> userData = userDocRef.get().get().getData();
            ArrayList<String> listOfOpenTaskIds;
            if (userData.containsKey("openTaskIds")) {
                listOfOpenTaskIds = (ArrayList<String>) userData.get("openTaskIds");
            } else {
                listOfOpenTaskIds = new ArrayList<>();
            }
            ArrayList<String> completedTaskIds;
            if (userData.containsKey("completedTaskIds")) {
                completedTaskIds = (ArrayList<String>) userData.get("completedTaskIds");
            } else {
                completedTaskIds = new ArrayList<>();
            }

            updateHelperMap = new HashMap<>();
            if (listOfOpenTaskIds.remove(taskId)) {
                updateHelperMap.put("openTaskIds", listOfOpenTaskIds);
            }
            if (!completedTaskIds.contains(taskId)) {
                completedTaskIds.add(taskId);
                updateHelperMap.put("completedTaskIds", completedTaskIds);
            }
            if (!updateHelperMap.isEmpty()) {
                userDocRef.update(updateHelperMap);
            }
        } catch (InterruptedException | ExecutionException exception) {
            // TODO(Adam): Handle this.
        }

        // Update task document
        DocumentReference taskDocRef = _database.collection("clientWebSimulator").document
                ("agent-dialogue-experiments").collection("tasks").document(taskId);
        try {
            Map<String, Object> taskData = taskDocRef.get().get().getData();
            ArrayList<String> listOfRatings;
            if (taskData.containsKey("ratingIds")) {
                listOfRatings = (ArrayList<String>) taskData.get("ratingIds");
            } else {
                listOfRatings = new ArrayList<>();
            }
            Long numberOfRemainingRatings = (Long) taskDocRef.get().get()
                    .getData().get("numberOfRemainingRatings");
            if (!listOfRatings.contains(ratingId)) {
                listOfRatings.add(ratingId);
                numberOfRemainingRatings -= 1;
                updateHelperMap.clear();
                updateHelperMap.put("ratingIds", listOfRatings);
                updateHelperMap.put("numberOfRemainingRatings", numberOfRemainingRatings);
                taskDocRef.update(updateHelperMap);
            }
        } catch (InterruptedException | ExecutionException exception) {
            // TODO(Adam): Handle this.
        }
    }
}
