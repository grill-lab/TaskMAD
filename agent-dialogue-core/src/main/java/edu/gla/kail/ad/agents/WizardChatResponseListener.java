package edu.gla.kail.ad.agents;

import com.google.cloud.firestore.*;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import edu.gla.kail.ad.Client;
import edu.gla.kail.ad.core.Log;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WizardChatResponseListener implements EventListener<QuerySnapshot> {

  private static final Logger logger = LoggerFactory.getLogger(WizardChatResponseListener.class);

  private StreamObserver<Client.InteractionResponse> m_observer;

  private ListenerRegistration m_registration;


  public WizardChatResponseListener(StreamObserver<Client.InteractionResponse> observer) {
    m_observer = observer;
  }

  @Override
  public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirestoreException e) {
    if (e != null) {
      logger.error("Listen failed: " + e);
      return;
    }
    if (snapshots != null && !snapshots.isEmpty()) {
      List<DocumentChange> documentChanges = snapshots.getDocumentChanges();
      logger.debug("Num document changes:" + documentChanges.size());
      for (DocumentChange change : documentChanges) {
        Log.ResponseLog response;
        Map<String, Object> changeData = change.getDocument().getData();

        // NOTE: This is replaying events from the DB for streaming.  It should copy the correct values from the
        // change data to the response to mimic the original response correctly.

        // TODO: Factor this out into its own method.
        Object responseIdString = changeData.get("response_id");
        String responseId = null;
        if (responseIdString != null) {
          responseId = (String) responseIdString;
        }
        response = WizardAgent.buildResponse(responseId, changeData);

        // Information about the original change.
        String userId = null;
        Object userString = changeData.get("user_id");
        if (userString != null) {
          userId = (String) userString;
        } else {
          userId = "undefined";
        }

        Object timestampString = changeData.get("timestamp");
        Timestamp timestamp = null;
        try {
          com.google.cloud.Timestamp cloudTimestamp = (com.google.cloud.Timestamp) timestampString;
          timestamp = Timestamps.fromNanos(cloudTimestamp.getNanos());
        } catch (Exception e1) {
          logger.error("Unable to get timestamp for object. " + timestampString.toString());
          e1.printStackTrace();
        }
        Client.InteractionResponse interactionResponse;
        try {
          interactionResponse = Client.InteractionResponse.newBuilder()
                  .setResponseId(responseId)
                  .setSessionId("blah")
                  .setTime(timestamp)
                  .setUserId(userId)
                  .setMessageStatus(Client.InteractionResponse.ClientMessageStatus.SUCCESSFUL)
                  .addAllInteraction(response.getActionList().stream()
                          .map(action -> action.getInteraction())
                          .collect(Collectors.toList()))
                  .build();
          logger.debug("Sending response: " + interactionResponse.getResponseId());
          m_observer.onNext(interactionResponse);
        } catch (Exception exception) {
          logger.warn("Error processing request :" + exception.getMessage() + " " + exception.getMessage());
          try {
            if (m_registration != null) {
              m_registration.remove();
            }
          } catch (Exception e1) {
            logger.warn("Unable to remove listener:" + e1.getMessage() + " " + e1.getMessage());

          }
          m_observer.onError(exception);
        }
      }
    }
  }

  public void setRegistration(ListenerRegistration registration) {
    m_registration = registration;
  }
}
