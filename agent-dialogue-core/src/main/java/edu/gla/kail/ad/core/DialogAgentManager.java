package edu.gla.kail.ad.core;

import com.google.protobuf.Timestamp;
import edu.gla.kail.ad.Client;
import edu.gla.kail.ad.Client.InteractionRequest;
import edu.gla.kail.ad.CoreConfiguration.AgentConfig;
import edu.gla.kail.ad.agents.DialogflowAgent;
import edu.gla.kail.ad.agents.RestSearchAgent;
import edu.gla.kail.ad.agents.SpeechToTextAgent;
import edu.gla.kail.ad.agents.WizardAgent;
import edu.gla.kail.ad.agents.LLMAgent;
import edu.gla.kail.ad.core.Log.RequestLog;
import edu.gla.kail.ad.core.Log.ResponseLog;
import edu.gla.kail.ad.core.Log.ResponseLog.Builder;
import edu.gla.kail.ad.core.Log.ResponseLog.MessageStatus;
import edu.gla.kail.ad.core.Log.ResponseLogOrBuilder;
import edu.gla.kail.ad.core.Log.Turn;
import edu.gla.kail.ad.core.Log.TurnOrBuilder;
import io.grpc.stub.StreamObserver;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The manager is configured to have conversations with specified agents (of certain agent type,
 * e.g. Dialogflow or Alexa).
 * Conversation management includes handling session state, including starting sessions (assigning
 * session IDs), as well as request identifiers. It also handles serialization of conversation log
 * data.
 *
 * Instruction of usage:
 * 1) Set up the Dialog agents using setUpAgents method.
 * 2) Call the getResponse for passed input.
 *
 * Example usage :
 * DialogAgentManager dialogAgentManager = new DialogAgentManager();
 * dialogAgentManager.setUpAgents(agents);
 * ResponseLog response = dialogAgentManager.getResponse(interactionRequest);
 **/

public class DialogAgentManager {
    // List of instances of used Dialog agents.
    private ArrayList<AgentInterface> _agents;
    // Session ID is a unique identifier of a session which is assigned by the method
    // startSession() called by DialogAgentManager constructor.
    private String _sessionId;
    private LogTurnManagerSingleton _logTurnManagerSingleton;
    // Time of no response from agent, after which there is timeout on getting response from agent.
    private Integer _agentCallTimeoutSeconds = 50;

    /**
     * Create a unique session ID generated with startSession() method.
     */
    public DialogAgentManager() throws IOException {
        startSession();
    }

    public String getSessionId() {
        return _sessionId;
    }

    /**
     * Create a unique sessionId.
     */
    private void startSession() throws IOException {
        _sessionId = generateRandomID();
        _logTurnManagerSingleton = LogTurnManagerSingleton.getLogTurnManagerSingleton();
    }

    /**
     * End a session
     */
    public void endSession() {

    }

    /**
     * Creates random ID used for session ID and request ID.
     *
     * @return String - A random ID consisting of timestamp and a random id generated by UUID.
     */
    private String generateRandomID() {
        return (new java.sql.Timestamp(System.currentTimeMillis())).toString() + UUID.randomUUID
                ().toString();
    }

    /**
     * Set up (e.g. authenticate) all agents and store them to the list of agents.
     *
     * @throws IllegalArgumentException - Raised by _agents.add(new
     *         DialogflowAgent(_sessionId, agentSpecificData.get(0)));
     * @throws IOException, IllegalArgumentException
     */
    public void setUpAgents(List<AgentConfig> agents) throws
            IllegalArgumentException, IOException {
        _agents = new ArrayList<>();
        for (AgentConfig agent : agents) {
            switch (agent.getServiceProvider()) {
                case UNRECOGNISED:
                    break;
                case DIALOGFLOW:
                    _agents.add(new DialogflowAgent(_sessionId, agent));
                    break;
                case WIZARD:
                    try {
                        _agents.add(new WizardAgent(_sessionId, agent));
                    } catch (Exception exception) {
                        // TODO: Implement
                        exception.printStackTrace();
                    }
                    break;
                case SEARCH:
                    try {
                        _agents.add(new RestSearchAgent(agent));
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                    break;
                case SPEECH_TO_TEXT: 
                    try {
                        _agents.add(new SpeechToTextAgent(agent));
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                    break;
                case LLM:
                    try {
                        _agents.add(new LLMAgent(agent));
                        System.out.println("*** Created a new LLM agent");
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                    break;
                default:
                    throw new IllegalArgumentException("The type of the agent provided " +
                            agent.getServiceProvider().toString() + "\" is not supported (yet)!");
            }
        }
    }

    /**
     * Take the request from the service and send back chosen response.
     * Store the turn in the logfile
     *
     * @param interactionRequest - The request sent by the client.
     * @return ResponseLog - The response chosen with a particular method from the list of responses
     *         obtained by calling all the agents.
     */
    public ResponseLog getResponse(InteractionRequest interactionRequest) throws Exception {
        RequestLog requestLog = RequestLog.newBuilder()
                .setRequestId(generateRandomID())
                .setTime(getCurrentTimeStamp())
                .setClientId(interactionRequest.getClientId())
                .setInteraction(interactionRequest.getInteraction()).build();

        List<ResponseLog> responses = getResponsesFromAgents(interactionRequest);
        ResponseLog chosenResponse = chooseOneResponse(responses);
        TurnOrBuilder turnBuilder = Turn.newBuilder()
                .setRequestLog(requestLog)
                .setSessionId(_sessionId)
                .setResponseLog(chosenResponse);
        for (ResponseLog response : responses) {
            ((Turn.Builder) turnBuilder).addCandidateResponse(response);
        }
        Turn turn = ((Turn.Builder) turnBuilder).build();

        // Store the turn in the log file.
        _logTurnManagerSingleton.addTurn(turn);
        return chosenResponse;
    }

    /**
     * Take a request from the service and create a streaming setup.
     *
     */
    public void listResponse(InteractionRequest interactionRequest, StreamObserver<Client.InteractionResponse> responseObserver) throws Exception {
        if (checkNotNull(_agents, "Agents are not set up! Use the method" +
                " setUpAgents() first.").isEmpty()) {
            throw new IllegalArgumentException("The list of agents is empty!");
        }
        ArrayList<AgentInterface> agents = (ArrayList<AgentInterface>) _agents.stream().filter
                (agent -> interactionRequest.getChosenAgentsList().contains(agent.getAgentId()))
                .collect(Collectors.toList());

        for (AgentInterface agent : agents) {
            agent.streamingResponseFromAgent(interactionRequest, responseObserver);
        }
    }

    /**
     * Get Request from Client and convert it to the RequestLog.
     * Return the list of responses for a given request.
     *
     * @param interactionRequest - The a data structure (implemented in log.proto) holding
     *         the interaction input passed to agents.
     * @return List<ResponseLog> - The list of responses of all agents set up on the
     *         setUpAgents(...) method call.
     */
    private List<ResponseLog> getResponsesFromAgents(InteractionRequest interactionRequest) {
        if (checkNotNull(_agents, "Agents are not set up! Use the method" +
                " setUpAgents() first.").isEmpty()) {
            throw new IllegalArgumentException("The list of agents is empty!");
        }
        ArrayList<AgentInterface> agents = (ArrayList<AgentInterface>) _agents.stream().filter
                (agent -> interactionRequest.getChosenAgentsList().contains(agent.getAgentId()))
                .collect(Collectors.toList());
        List<ResponseLog> listOfResponseLogs = asynchronousAgentCaller(interactionRequest, agents);
        // TODO: Remove when the log saving is implemented. Currently we can see the output.
        listOfResponseLogs.forEach(System.out::println);
        return listOfResponseLogs;
    }

    /**
     * Return the responses by calling agents asynchronously.
     *
     * @param interactionRequest - The a data structure (implemented in log .proto) holding
     *         the interaction input sent to the agent.
     * @return List<ResponseLog> - The list of responses of all agents set up on the
     *         setUpAgents(...) method call.
     */
    private List<ResponseLog> asynchronousAgentCaller(InteractionRequest interactionRequest,
                                                      List<AgentInterface> agents) {
        Observable<AgentInterface> agentInterfaceObservable = Observable.fromIterable(agents);
        return (agentInterfaceObservable.flatMap(agentObservable -> Observable
                .just(agentObservable)
                .subscribeOn(Schedulers.computation())
                .take(_agentCallTimeoutSeconds, TimeUnit.SECONDS) // Take only the observable emitted (completed) within specified time.
                .map(agent -> callForResponseAndValidate(agent, interactionRequest))
        ).toList().blockingGet());
    }

    /**
     * Return the responses by calling agents synchronously.
     *
     * @param interactionRequest - The a data structure (implemented in log .proto) holding
     *         the interaction input sent to the agent.
     * @return List<ResponseLog> - The list of responses of all agents set up on the
     *         setUpAgents(...) method call.
     */
    private List<ResponseLog> synchronousAgentCaller(InteractionRequest interactionRequest) {
        List<ResponseLog> listOfResponseLogs = new ArrayList<>();
        for (AgentInterface agent : _agents) {
            listOfResponseLogs.add(callForResponseAndValidate(agent, interactionRequest));
        }
        return listOfResponseLogs;
    }

    /**
     * Return a valid response from an agent within a set time period or return and unsuccessful
     * response.
     *
     * @param agent - The agent which
     * @param interactionRequest - The a data structure (implemented in log .proto) holding
     *         the interaction input sent to the agent.
     * @return ResponseLog - Response from the agent or unsuccessful reponse.
     */
    private ResponseLog callForResponseAndValidate(AgentInterface agent, InteractionRequest
            interactionRequest) {
        // TODO(Adam): Resend a call if unsuccessful? To be done later on.
        Callable<ResponseLog> callableCallForResponseAndValidate = () -> {
            try {
                return checkNotNull(agent.getResponseFromAgent(interactionRequest),
                        "The response from Agent was null!");
            } catch (Exception exception) {
                return ResponseLog.newBuilder()
                        .setMessageStatus(MessageStatus.UNSUCCESSFUL)
                        .setErrorMessage(exception.getMessage())
                        .setServiceProvider(agent.getServiceProvider())
                        .setTime(Timestamp.newBuilder()
                                .setSeconds(Instant.now()
                                        .getEpochSecond())
                                .setNanos(Instant.now()
                                        .getNano())
                                .build())
                        .build();
            }
        };

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<ResponseLog> future = executor.submit(callableCallForResponseAndValidate);
        ResponseLog responseLog;
        try {
            responseLog = future.get(_agentCallTimeoutSeconds, TimeUnit.SECONDS);
        } catch (Exception exception) {
            future.cancel(true); // Cancel and send a thread interrupt.
            ResponseLogOrBuilder responseLogBuilder = ResponseLog.newBuilder()
                    .setMessageStatus(MessageStatus.UNSUCCESSFUL)
                    .setServiceProvider(agent.getServiceProvider())
                    .setTime(getCurrentTimeStamp());
            if (exception.getMessage() == null) {
                ((Builder) responseLogBuilder).setErrorMessage(exception.toString());
            } else {
                ((Builder) responseLogBuilder).setErrorMessage(exception.getMessage());
            }
            responseLog = ((Builder) responseLogBuilder).build();
        } finally {
            executor.shutdownNow();
        }
        return responseLog;
    }


    /**
     * Choose the response from the list obtained from all the agents.
     *
     * @param responses - The list of ResponseLog responses obtained from agents.
     * @return ResponseLog - One of the responses chosen using specified ranking/choosing method.
     * @throws Exception - Throw when the list is not initialized or empty.
     */
    private ResponseLog chooseOneResponse(List<ResponseLog> responses) throws Exception {
        if (checkNotNull(responses, "The list passed to the chooseOneResponse function is not " +
                "initialized!").isEmpty()) {
            throw new IllegalArgumentException("The list of responses is empty!");
        }
        return chooseFirstValidResponse(responses);
    }

    /**
     * Choose the first successful response.
     *
     * @param responses - The list of ResponseLog responses obtained from agents.
     * @return ResponseLog - The first successful response or unsuccessful response if none of the
     *         provided responses were successful.
     */
    private ResponseLog chooseFirstValidResponse(List<ResponseLog> responses) {
        for (ResponseLog responseLog : responses) {
            if (responseLog.getMessageStatus() == MessageStatus.SUCCESSFUL) {
                return responseLog;
            }
        }
        return ResponseLog.newBuilder()
                .setMessageStatus(MessageStatus.UNSUCCESSFUL)
                .setErrorMessage("None of the passed responses had a successful call to the agent.")
                .setTime(getCurrentTimeStamp())
                .build();
    }

    private Timestamp getCurrentTimeStamp() {
        return Timestamp.newBuilder()
                .setSeconds(Instant.now()
                        .getEpochSecond())
                .setNanos(Instant.now()
                        .getNano())
                .build();
    }
}
