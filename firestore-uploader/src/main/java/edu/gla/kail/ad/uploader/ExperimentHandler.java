package edu.gla.kail.ad.uploader;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.common.primitives.Ints;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;


/**
 * Creates experiment entries in the Firestore database.
 */
class ExperimentHandler {

    void handleExperiments(BufferedReader tsvFileBufferedReader, ArrayList<String>
            arrayOfParameters, Firestore database) throws IOException {
        StringTokenizer stringTokenizer;
        String nextRow = tsvFileBufferedReader.readLine();

        while (nextRow != null) {
            stringTokenizer = new StringTokenizer(nextRow, "\t");
            Map<String, Object> updateHelperMap = new HashMap<>();
            ArrayList<String> dataArray = new ArrayList<>();

            while (stringTokenizer.hasMoreElements()) {
                dataArray.add(stringTokenizer.nextElement().toString());
            }

            // Add data as Integer if possible, if it's not ID.
            for (int i = 0; i < dataArray.size(); i++) {
                String data = dataArray.get(i);
                Integer dataInteger = Ints.tryParse(data);
                if (dataInteger != null && !arrayOfParameters.get(i).contains("Id")) {
                    updateHelperMap.put(arrayOfParameters.get(i), dataInteger);
                } else {
                    updateHelperMap.put(arrayOfParameters.get(i), data);
                }
            }

            // If line is empty, then read next line and continue executing while loop.
            if (updateHelperMap.size() == 0) {
                nextRow = tsvFileBufferedReader.readLine();
                continue;
            }

            DocumentReference experimentDocRef = database
                    .collection("clientWebSimulator")
                    .document("agent-dialogue-experiments")
                    .collection("experiments")
                    .document(updateHelperMap.get("experimentId").toString());
            experimentDocRef.set(updateHelperMap);
            nextRow = tsvFileBufferedReader.readLine();
        }
    }
}
