package edu.gla.kail.ad.agents;

import com.google.cloud.Tuple;
import com.google.cloud.dialogflow.v2beta1.AudioEncoding;
import com.google.cloud.dialogflow.v2beta1.Context;
import com.google.cloud.dialogflow.v2beta1.DetectIntentRequest;
import com.google.cloud.dialogflow.v2beta1.DetectIntentResponse;
import com.google.cloud.dialogflow.v2beta1.InputAudioConfig;
import com.google.cloud.dialogflow.v2beta1.QueryInput;
import com.google.cloud.dialogflow.v2beta1.QueryResult;
import com.google.cloud.dialogflow.v2beta1.SessionName;
import com.google.cloud.dialogflow.v2beta1.SessionsClient;
import com.google.cloud.dialogflow.v2beta1.TextInput;
import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import com.google.protobuf.Value;
import edu.gla.kail.ad.Client;
import edu.gla.kail.ad.Client.InputInteraction;
import edu.gla.kail.ad.Client.InteractionRequest;
import edu.gla.kail.ad.Client.InteractionType;
import edu.gla.kail.ad.Client.OutputInteraction;
import edu.gla.kail.ad.CoreConfiguration.AgentConfig;
import edu.gla.kail.ad.CoreConfiguration.ServiceProvider;
import edu.gla.kail.ad.core.AgentInterface;
import edu.gla.kail.ad.core.Log.ResponseLog;
import edu.gla.kail.ad.core.Log.ResponseLog.MessageStatus;
import edu.gla.kail.ad.core.Log.Slot;
import edu.gla.kail.ad.core.Log.SystemAct;
import io.grpc.stub.StreamObserver;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static edu.gla.kail.ad.agents.DialogflowAgentAuthorizationSingleton
        .getProjectIdAndSessionsClient;

/**
 * This class is initialized once per session per agent. (Different sessions have different
 * instances for the same agent.)
 * The request sent to the agent is validated.
 * (There are no invalid characters which can make dialogflow throw errors.)
 * // TODO(Adam) Use shutdown() method for closing _sessionClient stream? Implement it in
 * AgentInterface?
 */
public class DialogflowAgent implements AgentInterface {
    // The SessionsClient and SessionName are needed for the Dialogflow interaction. The are
    // created upon agent initialization.
    private SessionsClient _sessionsClient;
    private SessionName _session;
    // A unique ID passed by DialogAgentManager.
    private String _sessionId;
    // A unique ID of the agent this instance is of.
    private String _agentId;

    /**
     * Initialize a ready-to-work DialogflowAgent.
     *
     * @param sessionId - A unique ID passed to the method by DialogAgentManager.
     * @throws IOException - T setUpAgent method may throw exception if the data passed in
     *         the tupleOfProjectIdAndAuthorizationFile is invalid.
     */
    public DialogflowAgent(String sessionId, AgentConfig agent)
            throws IOException {
        _sessionId = sessionId;
        setUpAgent(agent);
    }

    @Override
    public String getAgentId() {
        return _agentId;
    }

    @Override
    public ServiceProvider getServiceProvider() {
        return ServiceProvider.DIALOGFLOW;
    }

    /**
     * Create the SessionClients and SessionNames for the agent.
     *
     * @throws IOException - When a there is something wrong with getting ProjectID and
     *         Session Client of the Agent.
     */
    private void setUpAgent(AgentConfig agent) throws IOException {
        Tuple<String, SessionsClient> projectIdAndSessionsClient = getProjectIdAndSessionsClient
                (agent);
        _sessionsClient = projectIdAndSessionsClient.y();
        _agentId = projectIdAndSessionsClient.x();
        _session = SessionName.of(projectIdAndSessionsClient.x(), _sessionId);
    }

    /**
     * Validate the inputInteraction for Dialogflow usage.
     * Checks: inputInteraction, type, respective type field, language code.
     *
     * @param inputInteraction - A data structure (implemented in log.proto) holding the
     *         incoming interaction that is being sent to an agent.
     * @throws IllegalArgumentException - Thrown when the input is invalid.
     */
    private void validateInputInteraction(InputInteraction inputInteraction) throws
            IllegalArgumentException {
        checkNotNull(inputInteraction, "The passed inputInteraction is null!");
        if (checkNotNull(inputInteraction.getLanguageCode(), "The inputInteraction LanguageCode " +
                "is set to null!").isEmpty()) {
            throw new IllegalArgumentException("The inputInteraction LanguageCode is empty!");
        }
        String ERROR_MESSAGE = "The inputInteraction of type %s has %s %s field!";
        if (checkNotNull(inputInteraction.getType(), "The inputInteraction type is null!")
                .toString().isEmpty()) {
            throw new IllegalArgumentException("The inputInteraction type is not set!");
        }
        switch (inputInteraction.getType()) {
            case TEXT:
                if (checkNotNull(inputInteraction.getText(), String.format(ERROR_MESSAGE, "TEXT",
                        "a null", "text")).isEmpty()) {
                    throw new IllegalArgumentException(String.format(ERROR_MESSAGE, "TEXT",
                            "an empty", "text"));
                }
                break;
            case ACTION:
                if (checkNotNull(inputInteraction.getActionList(), String.format(ERROR_MESSAGE,
                        "ACTION", "a null", "action")).isEmpty()) {
                    throw new IllegalArgumentException(String.format(ERROR_MESSAGE, "ACTION",
                            "an empty", "action"));
                }
                break;
            case AUDIO:
                if (checkNotNull(inputInteraction.getAudioBytes(), String.format(ERROR_MESSAGE,
                        "AUDIO", "a null", "audio")).isEmpty()) {
                    throw new IllegalArgumentException(String.format(ERROR_MESSAGE, "AUDIO",
                            "an empty", "audio"));
                }
                break;
            default:
                throw new IllegalArgumentException("Unrecognised interaction type.");
        }
    }

    /**
     * Return Query input for any type of inputInteraction the user may get: audio, text or action.
     *
     * @param inputInteraction - A data structure (implemented in log.proto) holding the
     *         incoming interaction that is being sent to an agent.
     * @return queryInput - A data structure which holds the query that needs to be send to
     *         Dialogflow.
     */
    private DetectIntentResponse detectIntentResponseMethod(InputInteraction inputInteraction) {
        validateInputInteraction(inputInteraction);
        // Get a response from a Dialogflow agent for a particular request (inputInteraction type).
        switch (inputInteraction.getType()) {
            case TEXT:
                TextInput.Builder textInput = TextInput.newBuilder().setText(inputInteraction
                        .getText())
                        .setLanguageCode(inputInteraction.getLanguageCode());
                return _sessionsClient.detectIntent(_session, QueryInput.newBuilder().setText
                        (textInput).build());
            case AUDIO:
                // AudioEncoding and sampleRateHertz hardcoded for simplicity, prone to changes.
                AudioEncoding audioEncoding = AudioEncoding.AUDIO_ENCODING_LINEAR_16;
                int sampleRateHertz = 16000;
                InputAudioConfig inputAudioConfig = InputAudioConfig.newBuilder()
                        .setLanguageCode(inputInteraction.getLanguageCode())
                        .setAudioEncoding(audioEncoding)
                        .setSampleRateHertz(sampleRateHertz)
                        .build();
                byte[] inputAudio = inputInteraction.getAudioBytes().getBytes();
                return _sessionsClient.detectIntent(DetectIntentRequest.newBuilder()
                        .setSession(_session.toString())
                        .setQueryInput(QueryInput.newBuilder().setAudioConfig(inputAudioConfig)
                                .build())
                        .setInputAudio(ByteString.copyFrom(inputAudio))
                        .build());
            case ACTION:
//                EventInput eventInput = EventInput.newBuilder()
//                        .setLanguageCode(inputInteraction.getLanguageCode())
//                        .setName() // Needs an argument String
//                        .setParameters(Struct.newBuilder().) // Optional, needs a Struct
//                        .build();
//                return _sessionsClient.detectIntent(_session, QueryInput.newBuilder().setEvent
//                        (eventInput).build());
                throw new IllegalArgumentException("The ACTION method for DialogFlow is not" +
                        " yet supported" +
                        "."); // TODO(Adam): implement;
            default:
                throw new IllegalArgumentException("Unrecognised interaction type.");
        }
    }

    /**
     * Send the request to the particular agent, using Dialogflow API.
     *
     * @throws IllegalArgumentException - The exception is being thrown when the type of the
     *         interaction requested is not recognised or supported.
     */
    @Override
    public ResponseLog getResponseFromAgent(InteractionRequest interactionRequest) throws
            IllegalArgumentException {
        DetectIntentResponse response = detectIntentResponseMethod(interactionRequest
                .getInteraction());
        QueryResult queryResult = response.getQueryResult();
        Timestamp timestamp = Timestamp.newBuilder()
                .setSeconds(Instant.now()
                        .getEpochSecond())
                .setNanos(Instant.now()
                        .getNano())
                .build();

        ResponseLog.Builder responseLogBuilder = ResponseLog.newBuilder()
                .setResponseId(response.getResponseId())
                .setTime(timestamp)
                .setServiceProvider(ServiceProvider.DIALOGFLOW)
                .setRawResponse(response.toString());

        SystemAct.Builder systemActBuilder = SystemAct.newBuilder()
                .setAction(queryResult.getAction())
                .setInteraction(OutputInteraction.newBuilder()
                        .setType(InteractionType.TEXT) // TODO(Adam): If more advanced Dialogflow
                        // agents can send a response with different interaction type, this needs
                        // to be changed.
                        .setText(queryResult.getFulfillmentText())
                        .build());

        for (Context context : queryResult.getOutputContextsList()) {
            // Set the slot's name and value for every Slot.
            for (Map.Entry<String, Value> parameterEntry : context.getParameters()
                    .getFieldsMap().entrySet()) {
                systemActBuilder.addSlot(Slot.newBuilder()
                        .setName(parameterEntry.getKey())
                        .setValue(parameterEntry.getValue().toString())
                        .build());
            }
        }
        responseLogBuilder.addAction(systemActBuilder.build());
        return responseLogBuilder.setMessageStatus(MessageStatus.SUCCESSFUL).build();
    }

    @Override
    public void streamingResponseFromAgent(InteractionRequest interactionRequest,
                                    StreamObserver<Client.InteractionResponse> responseObserver) throws Exception {
        responseObserver.onError(new NotImplementedException());
    }
}