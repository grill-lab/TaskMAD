package edu.gla.kail.ad.agents;

import com.google.api.core.SettableApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.google.protobuf.Timestamp;
import com.google.protobuf.Value;
import edu.gla.kail.ad.Client;
import edu.gla.kail.ad.Client.InteractionRequest;
import edu.gla.kail.ad.Client.InteractionType;
import edu.gla.kail.ad.Client.LoggedBotInteraction;
import edu.gla.kail.ad.Client.OutputInteraction;
import edu.gla.kail.ad.Client.OutputInteraction.Builder;
import edu.gla.kail.ad.CoreConfiguration.AgentConfig;
import edu.gla.kail.ad.CoreConfiguration.ServiceProvider;
import edu.gla.kail.ad.core.AgentInterface;
import edu.gla.kail.ad.core.Log.ResponseLog;
import edu.gla.kail.ad.core.Log.ResponseLog.MessageStatus;
import edu.gla.kail.ad.core.Log.SystemAct;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import java.io.FileInputStream;

/**
 * This is a Wizard-of-Oz agent created for experiments. It allows multiple users to chat
 * with one another.
 * It uses Firestore for storing the messages and to listen for events on shared chat 'documents'.
 */
public class WizardAgent implements AgentInterface {

  private static final Logger logger = LoggerFactory.getLogger(WizardAgent.class);

  // The firestore database connection.
  private Firestore _database;

  // Analogous to the "wizard project" - something like a project id in a dialogflow agent.
  private String _projectId;

  // Default timeout to wait for a response in seconds.
  private static final long DEFAULT_TIMEOUT_SECONDS = 60;

  // Hold the wizard agent's configuration, including necessary db credentials.
  private AgentConfig _agent;

  // Gold the hardcoded agent ID.
  private String _agentId = null;

  /**
   * Construct a new WizardAgent.
   *
   * @param sessionId
   * @throws Exception
   */
  public WizardAgent(String sessionId, AgentConfig agent) throws Exception {
    _agent = agent;
    _projectId = _agent.getProjectId();
    _agentId = _agent.getProjectId();
    initAgent();
  }

  /**
   * Initialize the agent.
   *
   * @throws Exception
   */
  private void initAgent() throws Exception {
    //URL configFileURL = new URL(_agent.getConfigurationFileURL());
    GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(_agent.getConfigurationFileURL()));
    checkNotNull(credentials, "Credentials used to initialise FireStore are null.");

    FirebaseOptions options = new FirebaseOptions.Builder()
            .setCredentials(credentials)
            .build();
    if (FirebaseApp.getApps().isEmpty()) {
      FirebaseApp.initializeApp(options);
    }
    _database = FirestoreClient.getFirestore();
  }

  /**
   * Specify the firestore configuration where messages will be read / written.
   * <p>
   * TODO(Jeff): Make the db location configurable from an agent config file.
   *
   * @param conversationId
   * @return
   */
  private CollectionReference getDbCollection(String conversationId) {
    CollectionReference collectionReference = _database.collection("wizard")
            .document(_projectId)
            .collection("conversations")
            .document(conversationId)
            .collection("messages");
    return collectionReference;
  }

  @Override
  public String getAgentId() {
    return _agentId;
  }

  @Override
  public ServiceProvider getServiceProvider() {
    return _agent.getServiceProvider();
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
  public void streamingResponseFromAgent(InteractionRequest interactionRequest, StreamObserver<Client.InteractionResponse> observer)
          throws Exception {

    // Get the conversation id from the request parameters.
    Map<String, Value> fieldsMap = interactionRequest.getAgentRequestParameters().getFieldsMap();
    if (!fieldsMap.containsKey("conversationId")) {
      throw new IllegalArgumentException("Request must specify the conversationId in the agent request parameters.");
    }
    String conversationId = fieldsMap.get("conversationId").getStringValue();
    Client.ClientId clientId = interactionRequest.getClientId();

    CollectionReference conversationCollection = getDbCollection(conversationId);

    // Wait for a response after the current time (filter out past messages).
    Query query = conversationCollection;
    logger.debug("Waiting on listener: " + query.toString());
    WizardChatResponseListener wizardChatResponseListener = new WizardChatResponseListener(observer);
    ListenerRegistration registration = query.addSnapshotListener(wizardChatResponseListener);
    wizardChatResponseListener.setRegistration(registration);
  }


  /**
   * Determine whether a request is from a wizard or not. This should be contained
   * in the request parameters.
   * TODO(Jeff): Fix how a wizard is detected
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
   * The core of the wizard. This adds the current message to the database and waits for a reply.
   *
   * It listens on the conversation messages. When a new message is added it returns a response
   * with the given input text.
   *
   * @param responseId
   * @param interactionRequest
   * @return
   * @throws InterruptedException
   * @throws ExecutionException
   * @throws TimeoutException
   */
  private ResponseLog addUserRequestWaitForReply(String responseId,
                                                 InteractionRequest interactionRequest)
          throws InterruptedException, ExecutionException, TimeoutException {
    checkNotNull(interactionRequest, "The passed interaction request is null!");

    logger.debug("Handling request from client" + interactionRequest.getClientId());

    // Create a future for our aysnc response.
    final SettableApiFuture<ResponseLog> future = SettableApiFuture.create();

    // Get the conversation id from the request parameters.
    Map<String, Value> fieldsMap = interactionRequest.getAgentRequestParameters().getFieldsMap();
    if (!fieldsMap.containsKey("conversationId")) {
      throw new IllegalArgumentException("Request must specify the conversationId in the agent request parameters.");
    }
    String conversationId = fieldsMap.get("conversationId").getStringValue();

    DocumentReference documentReference =
            addInteractionRequestToDatabase(responseId, conversationId, interactionRequest);

    Map<String, Object> data = new HashMap<>();
    data.put("interaction_text", "Success, message added!");
    ResponseLog response =  buildResponse(responseId, data);
    Client.InteractionResponse interactionResponse;
    Timestamp timestamp = Timestamp.newBuilder()
            .setSeconds(Instant.now()
                    .getEpochSecond())
            .setNanos(Instant.now()
                    .getNano())
            .build();
    try {
      interactionResponse = Client.InteractionResponse.newBuilder()
              .setResponseId(response.getResponseId())
              .setSessionId("blah")
              .setTime(timestamp)
              .setClientId(response.getClientId())
              .setUserId(interactionRequest.getUserId())
              .setMessageStatus(Client.InteractionResponse.ClientMessageStatus.SUCCESSFUL)
              .addAllInteraction(response.getActionList().stream()
                      .map(action -> action.getInteraction())
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
      logger.error("Message does not contain text. Returning fallback. For response: " + responseId);
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
    Timestamp timestamp = Timestamp.newBuilder()
      .setSeconds(Instant.now()
              .getEpochSecond())
      .setNanos(Instant.now()
              .getNano())
      .build(); 

    // List used to handle actions to be performed
    String[] interactionActionsList = {};
      
    try {
      // Get the interaction type
      String interactionTypeObj = String.valueOf(data.get("interaction_type"));
      if(interactionTypeObj != null){
        responseInteractionType = InteractionType.valueOf(Integer.parseInt(interactionTypeObj));
      }

      if(String.valueOf(data.get("time_seconds")) != null && String.valueOf(data.get("time_nanos")) != null){
        timestamp = Timestamp.newBuilder()
              .setSeconds(Long.parseLong(String.valueOf(data.get("time_seconds"))))
              .setNanos(Integer.parseInt(String.valueOf(data.get("time_nanos"))))
              .build();
      }

      // Get the interaction actions and convert the string to array 
      String interactionActionsListObj = String.valueOf(data.get("interaction_action_list")).replace("[", "").replace("]", "").replace("\"", "");
      if(interactionActionsListObj.length() > 0){
        interactionActionsList = interactionActionsListObj.split(",");
      }
      
    } catch (Exception e) {
      responseInteractionType = InteractionType.TEXT;
    }

    Builder outputInteractionBuilder = OutputInteraction.newBuilder()
      .setType(responseInteractionType)
      .setText(responseString)
      .setInteractionTime(timestamp);

    // We need to add all the actions to the OutputInteraction object
    for(int i = 0; i < interactionActionsList.length; i++){
      outputInteractionBuilder.addAction(interactionActionsList[i]);
    }

    return ResponseLog.newBuilder()
            .setResponseId(responseId)
            .setTime(timestamp)
            .setClientId(Client.ClientId.EXTERNAL_APPLICATION)
            .setServiceProvider(ServiceProvider.WIZARD)
            .setMessageStatus(MessageStatus.SUCCESSFUL)
            .setRawResponse("Text response: " + responseString)
            .addAction(SystemAct.newBuilder().setInteraction(outputInteractionBuilder.build())
            ).build();
  }

  /**
   * Add an interaction request to the message database.
   * @param responseId
   * @param conversationId
   * @param interactionRequest
   * @return Reference to the document added.
   */
  private DocumentReference addInteractionRequestToDatabase(String responseId, String conversationId, InteractionRequest interactionRequest) {
    DocumentReference chatReference = getDbCollection(conversationId).document(responseId);
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
    data.put("timestamp", com.google.cloud.Timestamp.now());
    data.put("logged_search_queries", interactionRequest.getInteraction().getLoggedSearchQueriesList());
    data.put("logged_search_queries_timestamp", interactionRequest.getInteraction().getLoggedSearchQueriesTimestampList());
    data.put("logged_page_ids", interactionRequest.getInteraction().getLoggedPageIdsList());
    data.put("logged_paragraph_ids", interactionRequest.getInteraction().getLoggedParagraphIdsList());
    data.put("logged_paragraph_texts", interactionRequest.getInteraction().getLoggedParagraphTextsList());
    data.put("logged_page_origins", interactionRequest.getInteraction().getLoggedPageOriginsList());
    data.put("logged_page_titles", interactionRequest.getInteraction().getLoggedPageTitlesList());
    data.put("logged_section_titles", interactionRequest.getInteraction().getLoggedSectionTitlesList());
    data.put("logged_paragraph_timestamp", interactionRequest.getInteraction().getLoggedParagraphTimestampList());

    data.put("logged_user_recipe_page_ids", interactionRequest.getInteraction().getLoggedUserRecipePageIdsList());
    data.put("logged_user_recipe_page_title", interactionRequest.getInteraction().getLoggedUserRecipePageTitleList());
    data.put("logged_user_recipe_section", interactionRequest.getInteraction().getLoggedUserRecipeSectionList());
    data.put("logged_user_recipe_section_value", interactionRequest.getInteraction().getLoggedUserRecipeSectionValueList());
    data.put("logged_user_recipe_select_timestamp", interactionRequest.getInteraction().getLoggedUserRecipeSelectTimestampList());


    // Log specific bot interaction values
    Map<String, Object> loggedBotInteractionData = new HashMap<String, Object>();
    for(LoggedBotInteraction botInteraction: interactionRequest.getInteraction().getLoggedBotInteractionList()){
      System.out.println(botInteraction.getContent().getIssuedQuery());
      Map<String, Object> loggedBotInteractionInnerData = new HashMap<String, Object>();
      loggedBotInteractionInnerData.put("issued_query", botInteraction.getContent().getIssuedQuery());
      loggedBotInteractionInnerData.put("bot_response", botInteraction.getContent().getBotResponse());
      loggedBotInteractionInnerData.put("bot_rewritten_response", botInteraction.getContent().getBotRewrittenResponse());
      loggedBotInteractionData.put(botInteraction.getId(), loggedBotInteractionInnerData);
    }

    data.put("logged_bot_interaction", loggedBotInteractionData);

    chatReference.set(data);
    return chatReference;
  }

}
