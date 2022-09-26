package edu.gla.kail.ad.agents;

import com.google.api.core.SettableApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;

import edu.gla.kail.ad.Client;
import edu.gla.kail.ad.AgentsConfig.WizardConfig;
import edu.gla.kail.ad.Client.InteractionAction;
import edu.gla.kail.ad.Client.InteractionRequest;
import edu.gla.kail.ad.Client.InteractionType;
import edu.gla.kail.ad.Client.OutputInteraction;
import edu.gla.kail.ad.Client.OutputInteraction.Builder;
import edu.gla.kail.ad.CoreConfiguration.AgentConfig;
import edu.gla.kail.ad.CoreConfiguration.ServiceProvider;
import edu.gla.kail.ad.core.AgentInterface;
import edu.gla.kail.ad.core.Log.ResponseLog;
import edu.gla.kail.ad.core.Log.ResponseLog.MessageStatus;
import edu.gla.kail.ad.service.Utils;
import edu.gla.kail.ad.core.Log.SystemAct;
import io.grpc.stub.StreamObserver;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * This is a Wizard-of-Oz agent created for experiments. It allows multiple
 * users to chat with one another. It uses Firestore for storing the messages
 * and to listen for events on shared chat 'documents'.
 */
public class WizardAgent implements AgentInterface {

    private static final Logger logger = LoggerFactory.getLogger(WizardAgent.class);

    // The firestore database connection.
    private Firestore database;

    // Analogous to the "wizard project" - something like a project id in a
    // dialogflow agent.
    private String projectId;

    // Hold the wizard agent's configuration, including necessary db credentials.
    private AgentConfig agent;

    // Gold the hardcoded agent ID.
    private String agentId;
    private String sessionId;
    private WizardConfig wizardConfig;

    /**
     * Construct a new WizardAgent.
     *
     * @param sessionId
     * @throws Exception
     */
    public WizardAgent(String sessionId, AgentConfig agent) throws Exception {
        this.agent = agent;
        this.projectId = agent.getProjectId();
        this.agentId = agent.getProjectId();
        this.sessionId = sessionId;
        initAgent();
    }

    private WizardConfig buildWizardConfig(String filePath) throws IOException {
        WizardConfig.Builder wizardConfigBuilder = WizardConfig.newBuilder();
        String jsonText = IOUtils.toString(new FileInputStream(filePath), StandardCharsets.UTF_8);
        JsonFormat.parser().merge(jsonText, wizardConfigBuilder);
        return wizardConfigBuilder.build();
    }

    private GoogleCredentials getGoogleCredentials(String credentialsFile) throws IOException {
        return GoogleCredentials.fromStream(new FileInputStream(credentialsFile));
    }

    private Firestore getFirestoreDatabase(GoogleCredentials googleCredentials) {
        FirebaseOptions options = new FirebaseOptions.Builder().setCredentials(googleCredentials).build();
        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options);
        }
        return FirestoreClient.getFirestore();
    }

    /**
     * Initialize the agent.
     *
     * @throws Exception
     */
    private void initAgent() throws Exception {
        this.wizardConfig = this.buildWizardConfig(this.agent.getConfigurationFileURL());
        if (this.isAgentConfigFileValid(wizardConfig)) {
            GoogleCredentials credentials = this.getGoogleCredentials(this.wizardConfig.getServerKey());
            if (credentials == null) {
                throw new Exception("Credentials used to initialise FireStore are null.");
            }
            this.database = this.getFirestoreDatabase(credentials);

        } else {
            throw new Exception("Wizard Agent Config file in the wrong format");
        }

    }

    /**
     * Specify the firestore configuration where messages will be read / written.
     * <p>
     *
     * @param conversationId
     * @return
     */
    private CollectionReference getDbCollection(String conversationId) {
        return this.database.collection(this.wizardConfig.getServerCollection()).document(this.projectId)
                .collection("conversations").document(conversationId).collection("messages");
    }

    @Override
    public String getAgentId() {
        return this.agentId;
    }

    @Override
    public ServiceProvider getServiceProvider() {
        return this.agent.getServiceProvider();
    }

    @Override
    public ResponseLog getResponseFromAgent(InteractionRequest interactionRequest) throws Exception {
        String responseId = ResponseIdGenerator.generate();
        if (userExit(interactionRequest)) {
            Map<String, Object> data = new HashMap<>();
            data.put("interaction_text", "Goodbye!");
            return buildResponse(responseId, data);
        }
        return addUserRequestWaitForReply(responseId, interactionRequest);
    }

    @Override
    public void streamingResponseFromAgent(InteractionRequest interactionRequest,
            StreamObserver<Client.InteractionResponse> observer) throws Exception {

        // Get the conversation id from the request parameters.
        Map<String, Value> fieldsMap = interactionRequest.getAgentRequestParameters().getFieldsMap();
        if (!fieldsMap.containsKey("conversationId")) {
            throw new IllegalArgumentException(
                    "Request must specify the conversationId in the agent request parameters.");
        }
        String conversationId = fieldsMap.get("conversationId").getStringValue();

        CollectionReference conversationCollection = getDbCollection(conversationId);

        // Wait for a response after the current time (filter out past messages).
        Query query = conversationCollection;
        logger.debug(String.format("Waiting on listener: %s", query.toString()));
        WizardChatResponseListener wizardChatResponseListener = new WizardChatResponseListener(observer);
        ListenerRegistration registration = query.addSnapshotListener(wizardChatResponseListener);
        wizardChatResponseListener.setRegistration(registration);
    }

    /**
     * Determine whether a request is from a wizard or not. This should be contained
     * in the request parameters. TODO(Jeff): Fix how a wizard is detected
     *
     * @param interactionRequest
     * @return true if the request is from the user (and not a wizard)
     */
    private boolean isRequestFromUser(InteractionRequest interactionRequest) {
        return !interactionRequest.getUserId().startsWith("ADwizard");
    }

    /**
     * Simple intent detector to exit the the conversation.
     *
     * @param interactionRequest
     * @return true if the intent is to exit the conversation
     */
    private boolean userExit(InteractionRequest interactionRequest) {
        return interactionRequest.getInteraction().getText().toLowerCase().equals("exit");
    }

    /**
     * The core of the wizard. This adds the current message to the database and
     * waits for a reply.
     *
     * It listens on the conversation messages. When a new message is added it
     * returns a response with the given input text.
     *
     * @param responseId
     * @param interactionRequest
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws TimeoutException
     */
    private ResponseLog addUserRequestWaitForReply(String responseId, InteractionRequest interactionRequest)
            throws InterruptedException, ExecutionException, TimeoutException {
        checkNotNull(interactionRequest, "The passed interaction request is null!");

        logger.debug("Handling request from client" + interactionRequest.getClientId());

        // Create a future for our aysnc response.
        final SettableApiFuture<ResponseLog> future = SettableApiFuture.create();

        // Get the conversation id from the request parameters.
        Map<String, Value> fieldsMap = interactionRequest.getAgentRequestParameters().getFieldsMap();
        if (!fieldsMap.containsKey("conversationId")) {
            throw new IllegalArgumentException(
                    "Request must specify the conversationId in the agent request parameters.");
        }
        String conversationId = fieldsMap.get("conversationId").getStringValue();

        DocumentReference documentReference = addInteractionRequestToDatabase(responseId, conversationId,
                interactionRequest);

        Map<String, Object> data = new HashMap<>();
        data.put("interaction_text", "Success, message added!");
        ResponseLog response = buildResponse(responseId, data);
        Client.InteractionResponse interactionResponse;
        Timestamp timestamp = Timestamp.newBuilder().setSeconds(Instant.now().getEpochSecond())
                .setNanos(Instant.now().getNano()).build();
        try {
            interactionResponse = Client.InteractionResponse.newBuilder().setResponseId(response.getResponseId())
                    .setSessionId("blah").setTime(timestamp).setClientId(response.getClientId())
                    .setUserId(interactionRequest.getUserId())
                    .setMessageStatus(Client.InteractionResponse.ClientMessageStatus.SUCCESSFUL)
                    .addAllInteraction(response.getActionList().stream().map(action -> action.getInteraction())
                            .collect(Collectors.toList()))
                    .build();
        } catch (Exception exception) {
            logger.warn("Error processing request :" + exception.getMessage() + " " + exception.getMessage());
        }
        return response;
    }

    /**
     * Builds the response message from a firestore database message.
     *
     * Currently, this only sends back the interaction_text from the data.
     *
     * @param responseId
     * @param data
     * @return
     */
    protected static ResponseLog buildResponse(String responseId, Map<String, Object> data) {

        // Get the input text, use a fallback
        Object messageText = data.get("interaction_text");
        String responseString = null;
        if (messageText != null) {
            responseString = (String) messageText;
        } else {
            logger.error(
                    String.format("Message does not contain text. Returning fallback. For response: %s", responseId));
            responseString = "I'm sorry, message does not contain text.";
        }

        String userId = null;
        Object userString = data.get("user_id");
        if (userId != null) {
            userString = (String) userId;
        } else {
            userString = "No valid user.";
        }

        // We need to get and set the interaction type
        InteractionType responseInteractionType = InteractionType.TEXT;
        Timestamp timestamp = Timestamp.newBuilder().setSeconds(Instant.now().getEpochSecond())
                .setNanos(Instant.now().getNano()).build();

        try {
            // Get the interaction type
            String interactionTypeObj = String.valueOf(data.get("interaction_type"));
            if (interactionTypeObj != null) {
                responseInteractionType = InteractionType.valueOf(Integer.parseInt(interactionTypeObj));
            }

            if (String.valueOf(data.get("time_seconds")) != null && String.valueOf(data.get("time_nanos")) != null) {
                timestamp = Timestamp.newBuilder().setSeconds(Long.parseLong(String.valueOf(data.get("time_seconds"))))
                        .setNanos(Integer.parseInt(String.valueOf(data.get("time_nanos")))).build();
            }

        } catch (Exception e) {
            responseInteractionType = InteractionType.TEXT;
        }

        Builder outputInteractionBuilder = OutputInteraction.newBuilder().setType(responseInteractionType)
                .setText(responseString).setInteractionTime(timestamp);

        // Get the interaction actions and convert the string to array
        if (data.containsKey("interaction_action_list") && data.get("interaction_action_list") != null) {
            ArrayList<Long> interactionActionsListObj = new ArrayList<>();
            if (data.get("interaction_action_list") instanceof ArrayList) {
                interactionActionsListObj = (ArrayList<Long>) data.get("interaction_action_list");
            }
            // We need to add all the actions to the OutputInteraction object
            for (int i = 0; i < interactionActionsListObj.size(); i++) {
                outputInteractionBuilder
                        .addActionType(InteractionAction.valueOf(interactionActionsListObj.get(i).intValue()));
            }
        }

        return ResponseLog.newBuilder().setResponseId(responseId).setTime(timestamp)
                .setClientId(Client.ClientId.EXTERNAL_APPLICATION).setServiceProvider(ServiceProvider.WIZARD)
                .setMessageStatus(MessageStatus.SUCCESSFUL).setRawResponse("Text response: " + responseString)
                .addAction(SystemAct.newBuilder().setInteraction(outputInteractionBuilder.build())).build();
    }

    /**
     * Add an interaction request to the message database.
     * 
     * @param responseId
     * @param conversationId
     * @param interactionRequest
     * @return Reference to the document added.
     */
    private DocumentReference addInteractionRequestToDatabase(String responseId, String conversationId,
            InteractionRequest interactionRequest) {
        DocumentReference chatReference = getDbCollection(conversationId).document(responseId);
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("response_id", responseId);
        data.put("client_id", interactionRequest.getClientIdValue());
        data.put("time_seconds", interactionRequest.getTime().getSeconds());
        data.put("time_nanos", interactionRequest.getTime().getNanos());
        data.put("user_id", interactionRequest.getUserId());
        data.put("interaction_type", interactionRequest.getInteraction().getTypeValue());
        data.put("interaction_device_type", interactionRequest.getInteraction().getDeviceType());
        data.put("interaction_language_code", interactionRequest.getInteraction().getLanguageCode());
        data.put("interaction_text", interactionRequest.getInteraction().getText());
        data.put("interaction_audio_bytes", interactionRequest.getInteraction().getAudioBytes());

        // Convert the interaction list to integer format
        ArrayList<Integer> actionsList = new ArrayList<Integer>();
        for (int i = 0; i < interactionRequest.getInteraction().getActionTypeList().size(); i++) {
            actionsList.add(interactionRequest.getInteraction().getActionTypeList().get(i).getNumber());
        }
        data.put("interaction_action_list", actionsList);
        data.put("timestamp", com.google.cloud.Timestamp.now());

        try {
            String logsJsonString = JsonFormat.printer().preservingProtoFieldNames()
                    .print(interactionRequest.getInteraction().getInteractionLogs());
            HashMap<String, Object> logsHashMap = Utils.jsonStringToHashMap(logsJsonString);
            data.put("interaction_logs", logsHashMap);
        } catch (Exception e) {
            data.put("interaction_logs", new HashMap<String, Object>());
        }

        chatReference.set(data);
        return chatReference;
    }

    @Override
    public boolean isAgentConfigFileValid(Message agentConfig) {
        if (agentConfig instanceof WizardConfig) {
            WizardConfig wizardConfigObj = (WizardConfig) agentConfig;
            return !Utils.isBlank(wizardConfigObj.getServerKey())
                    && !Utils.isBlank(wizardConfigObj.getServerCollection());
        }
        return false;

    }

}