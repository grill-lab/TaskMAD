package edu.gla.kail.ad.core;

import com.google.api.core.ApiFuture;
import com.google.api.core.SettableApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.database.annotations.Nullable;
import com.google.protobuf.Timestamp;
import edu.gla.kail.ad.Client;

import java.io.FileInputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

public class FirestoreTest {

  private Firestore _database;

  private String _firebaseCredentialsPath =
          "/home/jeff/code/agent-dialogue/agent-dialogue-core/src/main/resources/agentdialogue-2cd4b-firebase-adminsdk-z39zw" +
                  "-4d5427d1fc.json";

  private static final long TIMEOUT_SECONDS = 60;

  public FirestoreTest() throws Exception {
    initDb();
  }

  private void initDb() throws Exception {
    GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(_firebaseCredentialsPath));
    checkNotNull(credentials, "Credentials used to initialise FireStore are null.");

    FirestoreOptions fireStoreOptions =
            FirestoreOptions.newBuilder().setTimestampsInSnapshotsEnabled(true)
                    .setCredentials(credentials)
                    .setProjectId("agentdialogue-2cd4b")
                    .build();

    _database = fireStoreOptions.getService();
  }

  private CollectionReference getDbCollection() {
    CollectionReference collectionReference = _database.collection("test");
    return collectionReference;
  }

  List<DocumentChange> listenForChanges() throws Exception {
    SettableApiFuture<List<DocumentChange>> future = SettableApiFuture.create();
    _database.collection("test").whereEqualTo("name", "jeff")
            .addSnapshotListener(new EventListener<QuerySnapshot>() {
              @Override
              public void onEvent(@Nullable QuerySnapshot snapshots,
                                  @Nullable FirestoreException e) {
                if (e != null) {
                  System.err.println("Listen failed: " + e);
                  return;
                }

                for (DocumentChange dc : snapshots.getDocumentChanges()) {
                  switch (dc.getType()) {
                    case ADDED:
                      System.out.println("New msg: " + dc.getDocument().getData());
                      break;
                    case MODIFIED:
                      System.out.println("Modified msg: " + dc.getDocument().getData());
                      break;
                    case REMOVED:
                      System.out.println("Removed msg: " + dc.getDocument().getData());
                      break;
                    default:
                      break;
                  }
                }
                // [START_EXCLUDE silent]
                if (!future.isDone()) {
                  future.set(snapshots.getDocumentChanges());
                }
                // [END_EXCLUDE]
              }
            });
    // [END listen_for_changes]

    return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
  }

  private DocumentReference addInteractionRequestToDatabase() throws ExecutionException, InterruptedException {
    Map<String, Object> data = new HashMap();
    data.put("name", "jeff");
    ApiFuture<DocumentReference> chatReference = getDbCollection().add(data);
    return chatReference.get();
  }

  public static void main(String[] args) throws Exception {
    FirestoreTest updater = new FirestoreTest();
    DocumentReference ref = updater.addInteractionRequestToDatabase();
    System.out.println("Added document: " + ref.getPath());
    List<DocumentChange> documentChanges = updater.listenForChanges();
    return;
  }
}
