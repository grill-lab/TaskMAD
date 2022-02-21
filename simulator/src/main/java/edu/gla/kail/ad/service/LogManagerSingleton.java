package edu.gla.kail.ad.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.google.protobuf.Timestamp;
import edu.gla.kail.ad.Client.InteractionRequest;
import edu.gla.kail.ad.Client.InteractionResponse;
import edu.gla.kail.ad.Client.OutputInteraction;
import edu.gla.kail.ad.PropertiesSingleton;
import edu.gla.kail.ad.eval.Ratings.Rating;
import edu.gla.kail.ad.eval.Ratings.Rating.Builder;
import edu.gla.kail.ad.eval.Ratings.RatingOrBuilder;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Manage Client _interactions and store them in a certain directory/path.
 * TODO(Adam): Implement the use of LogStash, or maybe log4j, or make this thread safe.
 */
public class LogManagerSingleton {
    private static LogManagerSingleton _instance;
    private static OutputStream _interactionsOutputStream;
    private static OutputStream _ratingsOutputStream;
    private static Firestore _database;

    /**
     * Get instance of this class.
     *
     * @return LogManagerSingleton - An instance of the class itself.
     */
    static synchronized LogManagerSingleton getLogManagerSingleton() throws IOException {
        if (_instance == null) {
            GoogleCredentials credentials;
            credentials = GoogleCredentials
                    .fromStream(new URL(PropertiesSingleton
                            .getSimulatorConfig()
                            .getFirestoreAuthorizationURL())
                            .openStream());

            checkNotNull(credentials, "Credentials used to initialise FireStore are null.");
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(credentials)
                    .build();
            FirebaseApp.initializeApp(options);

            _database = FirestoreClient.getFirestore();


            _instance = new LogManagerSingleton();
            // Directory to the folder with logs.
            String logInteractionsPath = Paths
                    .get(PropertiesSingleton.getSimulatorConfig().getLogStoragePath() +
                            "/interactions_logs/").toString();
            directoryExistsOrCreate(logInteractionsPath);
            logInteractionsPath += "/" + ZonedDateTime.now().toLocalDate().toString();
             _interactionsOutputStream = new FileOutputStream(logInteractionsPath);

            String logRatingsPath = Paths
                    .get(PropertiesSingleton.getSimulatorConfig().getLogStoragePath() +
                            "/ratings_logs/").toString();
            directoryExistsOrCreate(logRatingsPath);
            logRatingsPath += "/" + ZonedDateTime.now().toLocalDate().toString();

            _ratingsOutputStream = new FileOutputStream(logRatingsPath);
        }
        return _instance;
    }

    public static Firestore returnDatabase() throws IOException {
        return getLogManagerSingleton()._database;
    }

    /**
     * Validate whether the directory exists and if not, then create it.
     */
    private static void directoryExistsOrCreate(String path) throws IOException {
        File directory = new File(path);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        if (!directory.exists()) {
            throw new IOException("Unable to create directory." + path);
        }
    }

    /**
     * Add the interaction to the Output buffer. Nullable parameters are acceptable.
     *
     * @param interactionRequest - The instance of interactionRequest from proto buffer to
     *         be saved.
     * @param interactionResponse - The instance of interactionResponse from proto buffer to
     *         be saved.
     * @throws IOException - Thrown when... TODO
     */
    public synchronized void addInteraction(@Nullable InteractionRequest interactionRequest,
                                            @Nullable InteractionResponse interactionResponse)
            throws IOException {
        // TODO(Adam): Handle the exception.
        if (interactionRequest != null) {
            interactionRequest.writeDelimitedTo(_interactionsOutputStream);
            DocumentReference docRef = _database.collection("clientWebSimulator").document
                    ("experimentalUi").collection("clientInteractionRequest").document
                    (interactionRequest.getTime().getSeconds() + "_" + interactionRequest.getTime
                            ().getNanos());
            Map<String, Object> data = new HashMap<>();

            data.put("client_id", interactionRequest.getClientIdValue());
            data.put("time_seconds", interactionRequest.getTime().getSeconds());
            data.put("time_nanos", interactionRequest.getTime().getNanos());
            data.put("userID", interactionRequest.getUserId());
            data.put("interaction_type", interactionRequest.getInteraction().getTypeValue());
            data.put("interaction_device_type", interactionRequest.getInteraction().getDeviceType
                    ());
            data.put("interaction_language_code", interactionRequest.getInteraction()
                    .getLanguageCode());
            data.put("interaction_text", interactionRequest.getInteraction().getText());
            data.put("interaction_audio_bytes", interactionRequest.getInteraction().getAudioBytes
                    ());
            data.put("interaction_action_list", interactionRequest.getInteraction().getActionList
                    ().toString());

            docRef.set(data);
        } else if (interactionResponse != null) {
            interactionResponse.writeDelimitedTo(_interactionsOutputStream);
            DocumentReference docRef = _database.collection("clientWebSimulator").document
                    ("experimentalUi").collection("clientInteractionResponse").document
                    (interactionResponse.getTime().getSeconds() + "_" + interactionResponse
                            .getTime().getNanos());
            Map<String, Object> data = new HashMap<>();

            data.put("response_id", interactionResponse.getResponseId());
            data.put("time_seconds", interactionResponse.getTime().getSeconds());
            data.put("time_nanos", interactionResponse.getTime().getNanos());
            data.put("client_id", interactionResponse.getClientIdValue());
            data.put("message_status", interactionResponse.getMessageStatusValue());
            data.put("error_message", interactionResponse.getErrorMessage());
            data.put("userID", interactionResponse.getUserId());
            docRef.set(data);

            Integer counter = 0;
            for (OutputInteraction outputInteraction : interactionResponse.getInteractionList()) {
                Map<String, Object> outputInteractionData = new HashMap<>();
                outputInteractionData.put("text", outputInteraction.getText());
                outputInteractionData.put("audio_bytes", outputInteraction.getAudioBytes());
                outputInteractionData.put("type", outputInteraction.getTypeValue());
                outputInteractionData.put("action_list", outputInteraction.getActionList()
                        .toString());
                docRef.collection("output_interaction").document(counter.toString()).set
                        (outputInteractionData);
                counter++;
            }
        }
        _interactionsOutputStream.flush();
    }


    /**
     * Add the ratings to the rating Output buffer.
     *
     * @param ratingScore
     * @param responseId
     * @param experimentId
     * @param requestId
     * @throws IOException
     */
    public synchronized void addRating(String ratingScore, String responseId, String
            experimentId, @Nullable String requestId) throws
            IOException {
        RatingOrBuilder ratingOrBuilder = Rating.newBuilder()
                .setScore(Integer.valueOf(ratingScore))
                .setResponseId(responseId)
                .setTime(Timestamp.newBuilder()
                        .setSeconds(Instant.now()
                                .getEpochSecond())
                        .setNanos(Instant.now()
                                .getNano())
                        .build())
                .setExperimentId(experimentId);
        if (requestId != null) {
            ((Builder) ratingOrBuilder).setRequestId(requestId);
        }
        Rating rating = ((Builder) ratingOrBuilder).build();
        rating.writeDelimitedTo(_ratingsOutputStream);
        _ratingsOutputStream.flush();

        DocumentReference docRef = _database.collection("clientWebSimulator").document
                ("experimentalUi").collection("clientRatings")
                .document(rating.getTime()
                        .getSeconds() + "_" + rating.getTime().getNanos());
        Map<String, Object> data = new HashMap<>();

        data.put("experiment_id", rating.getExperimentId());
        data.put("time_seconds", rating.getTime().getSeconds());
        data.put("time_nanos", rating.getTime().getNanos());
        data.put("response_id", rating.getResponseId());
        data.put("score", rating.getScore());
        data.put("request_id", rating.getRequestId());

        docRef.set(data);
    }


    /**
     * Close the stream = save the file; set the instance to null.
     *
     * @throws IOException - Thrown when... TODO
     */
    public void saveAndExit() throws Exception {
        // TODO(Adam): Handle the exception.
        _interactionsOutputStream.close();
        _instance = null;
        _instance = LogManagerSingleton.getLogManagerSingleton();
    }

}