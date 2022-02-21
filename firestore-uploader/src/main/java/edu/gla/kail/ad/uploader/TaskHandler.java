package edu.gla.kail.ad.uploader;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.common.primitives.Ints;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutionException;

import static com.google.common.base.Preconditions.checkNotNull;


/**
 * Create Turn entries in the Firestore database. Update experiment entries with tasks
 */
class TaskHandler {
    private Firestore _database;

    /**
     * Process the file passed under tsvFileBufferedReader.
     */
    void handleTasks(BufferedReader tsvFileBufferedReader, ArrayList<String>
            arrayOfParameters, Firestore database) throws IOException {
        _database = database;

        StringTokenizer stringTokenizer;
        String nextRow = tsvFileBufferedReader.readLine();

        String taskId = null;
        HashMap<String, Object> updateHelperMap = new HashMap<>(); // Store the data about the
        // a particular task.
        HashMap<String, Object> turnMap = new HashMap<>();
        ArrayList<Object> turnsArray = new ArrayList<>();

        while (nextRow != null) {
            stringTokenizer = new StringTokenizer(nextRow, "\t");
            HashMap<String, Object> supportingHelperMap = new HashMap<>(); // Store the data
            // about the turn within a task.
            ArrayList<String> dataArray = new ArrayList<>();
            while (stringTokenizer.hasMoreElements()) {
                dataArray.add(stringTokenizer.nextElement().toString());
            }

            // Add data as Integer if possible, if it's not ID.
            for (int i = 0; i < dataArray.size(); i++) {
                String data = dataArray.get(i);
                Integer dataInteger = Ints.tryParse(data);
                if (dataInteger != null && !arrayOfParameters.get(i).contains("Id")) {
                    supportingHelperMap.put(arrayOfParameters.get(i), dataInteger);
                } else {
                    supportingHelperMap.put(arrayOfParameters.get(i), data);
                }
            }

            // If line is empty, then read next line and continue executing while loop.
            if (dataArray.size() == 0) {
                nextRow = tsvFileBufferedReader.readLine();
                continue;
            }
            checkNotNull(supportingHelperMap.get("taskId"), "Table not formatted correctly; " +
                    "taskId non existent.");
            // If task if of the next data row is different from the previous task id, then add
            // the turn to the database.
            if (taskId != null && !taskId.equals(supportingHelperMap.get("taskId").toString())) {
                addTaskToDatabase(supportingHelperMap, updateHelperMap);
                updateHelperMap = new HashMap<>();
            }
            // Update taskId.
            taskId = supportingHelperMap.get("taskId").toString();


            String type_of_turn = supportingHelperMap.remove("type_of_turn").toString();
            String utterance = supportingHelperMap.remove("utterance").toString();
            Integer time_seconds = (Integer) supportingHelperMap.remove("time_seconds");

            // If it is a new task, then populate updateHelperMap.
            if (updateHelperMap.isEmpty()) {
                for (Entry<String, Object> entry : supportingHelperMap.entrySet()) {
                    updateHelperMap.put(entry.getKey(), entry.getValue());
                }
            } else {
                turnsArray = (ArrayList<Object>) updateHelperMap.get("turns");
            }
            if (type_of_turn.equals("request") || type_of_turn.equals("response")) {
                turnMap.put(type_of_turn, utterance);
                turnMap.put(type_of_turn + "Time_seconds", Integer.valueOf(time_seconds));
                turnMap.put("clientId", supportingHelperMap.get("clientId"));
                turnMap.put("deviceType", supportingHelperMap.get("deviceType"));
                turnMap.put("language_code", supportingHelperMap.get("language_code"));
            } else {
                System.out.println("Unrecognised type of turn: " + type_of_turn);
                throw new IOException();
            }
            turnsArray.add(turnMap);
            updateHelperMap.put("turns", turnsArray);

            nextRow = tsvFileBufferedReader.readLine();
            if (nextRow == null) {
                addTaskToDatabase(supportingHelperMap, updateHelperMap);
            }
            turnMap = new HashMap<>();
            turnsArray = new ArrayList<>();
        }
    }

    /**
     * Create a new task in the Firestore Database.
     *
     * @param supportingHelperMap - Store the data about the turn within a task.
     * @param updateHelperMap - The map that stores the data about the a particular task.
     */
    private void addTaskToDatabase(HashMap<String, Object> supportingHelperMap,
                                   HashMap<String, Object> updateHelperMap) {
        // Update or create experiment in experiments database.
        String experimentId = supportingHelperMap.get("experimentId").toString();
        ArrayList<String> taskIds = new ArrayList<>();
        DocumentReference experimentDocRef = _database
                .collection("clientWebSimulator")
                .document("agent-dialogue-experiments")
                .collection("experiments")
                .document(experimentId);
        Map<String, Object> createExperimentHelperMap = new HashMap<>();
        if (!verifyExperimentExistence(experimentId)) {
            // If not, then create experiment and add taskIds list.
            taskIds.add(supportingHelperMap.get("taskId").toString());
            createExperimentHelperMap.put("experimentId", experimentId);
            createExperimentHelperMap.put("taskIds", taskIds);
            experimentDocRef.set(createExperimentHelperMap);
        } else {
            // Get taskIds list and update it.
            try {
                if (experimentDocRef.get().get().getData().containsKey("taskIds")) {
                    taskIds = (ArrayList<String>) experimentDocRef
                            .get().get().getData()
                            .get("taskIds");
                }
                taskIds.add(supportingHelperMap.get("taskId").toString());
                createExperimentHelperMap.put("taskIds", taskIds);
                experimentDocRef.update(createExperimentHelperMap);
            } catch (Exception exception) {
                System.out.println("There was a problem with getting taskIds list for " +
                        "the: " + experimentId + "\n" + exception.getStackTrace().toString());
            }
        }

        // Create a new task.
        DocumentReference turnDocRef = _database
                .collection("clientWebSimulator")
                .document("agent-dialogue-experiments")
                .collection("tasks")
                .document(updateHelperMap.get("taskId").toString());
        turnDocRef.set(updateHelperMap);
    }


    /**
     * Checks the existence of the experiment in Firestore database.
     *
     * @param experimentId - The unique ID of the particular experiment.
     * @return - report whether the document exists or not.
     */
    private Boolean verifyExperimentExistence(String experimentId) {
        if (experimentId == null || experimentId.equals("")) {
            return false;
        }
        DocumentReference experimentDocRef = _database
                .collection("clientWebSimulator")
                .document("agent-dialogue-experiments")
                .collection("experiments")
                .document(experimentId);
        ApiFuture<DocumentSnapshot> future = experimentDocRef.get();
        try {
            if (future.get().exists()) {
                return true;
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return false;
    }
}
