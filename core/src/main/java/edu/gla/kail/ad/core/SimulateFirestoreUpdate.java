package edu.gla.kail.ad.core;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.google.protobuf.Timestamp;
import edu.gla.kail.ad.Client;

import javax.swing.text.Document;
import java.io.FileInputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static com.google.common.base.Preconditions.checkNotNull;

public class SimulateFirestoreUpdate {

  private Firestore _database;

  private String _firebaseCredentialsPath =
          "/home/jeff/code/agent-dialogue/agent-dialogue-core/src/main/resources/agentdialogue-2cd4b-firebase-adminsdk-z39zw" +
                  "-4d5427d1fc.json";

  // Analogous to the "wizard project" - something like a project id in a dialogflow agent.
  private String _projectId = "Ct5UbiTWQmkDbmF0aJrt";

  // A unique ID passed set in the constructor, passed by DialogAgentManager.
  private String _sessionId = "1";

  private String _conversationId = "NslkRXnAntZJa2QRvet6";

  private String _curResponseText = null;

  private static final long TIMEOUT_SECONDS = 60;


  public SimulateFirestoreUpdate(String sessionId) throws Exception {
    _sessionId = sessionId;
    initAgent();
  }

  private void initAgent() throws Exception {
    GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(_firebaseCredentialsPath));
    checkNotNull(credentials, "Credentials used to initialise FireStore are null.");

    FirebaseOptions options = new FirebaseOptions.Builder()
            .setCredentials(credentials)
            .build();
    FirebaseApp.initializeApp(options);
    _database = FirestoreClient.getFirestore();
  }

  private CollectionReference getDbCollection() {
    CollectionReference collectionReference = _database.collection("wizard")
            .document(_projectId)
            .collection("conversations")
            .document(_conversationId)
            .collection("messages");
    return collectionReference;
  }

  public void updateFirestoreWithMessage(String responseId, Client.InteractionRequest request) throws ExecutionException, InterruptedException {
    DocumentReference ref = addInteractionRequestToDatabase(responseId, request);
    System.out.println("Added document: " + ref.getPath());
  }

  private DocumentReference addInteractionRequestToDatabase(String responseId, Client.InteractionRequest interactionRequest) throws ExecutionException, InterruptedException {

    Map<String, Object> data = new HashMap();
    data.put("response_id", responseId);
    data.put("client_id", interactionRequest.getClientIdValue());
    data.put("time_seconds", interactionRequest.getTime().getSeconds());
    data.put("time_nanos", interactionRequest.getTime().getNanos());
    data.put("user_id", interactionRequest.getUserId());
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
    ApiFuture<DocumentReference> chatReference = getDbCollection().add(data);
    return chatReference.get();
  }

  public static void main(String[] args) throws Exception {
    Client.InteractionRequest request = Client.InteractionRequest.newBuilder()
            .setTime(Timestamp.newBuilder()
                    .setSeconds(Instant.now().getEpochSecond())
                    .setNanos(Instant.now().getNano())
                    .build())
            .setUserId("2")
            .setClientId(Client.ClientId.EXTERNAL_APPLICATION)
            .setInteraction(Client.InputInteraction.newBuilder()
                    .setType(Client.InteractionType.TEXT)
                    .setText("my test message")
                    .setDeviceType("wizard")
                    .setLanguageCode("en-US"))
            .build();

    SimulateFirestoreUpdate updater = new SimulateFirestoreUpdate("magicsessionid");
    updater.updateFirestoreWithMessage("2", request);
  }
}
