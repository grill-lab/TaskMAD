package edu.gla.kail.ad.service;

import com.google.gson.JsonObject;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Struct;
import com.google.protobuf.StructOrBuilder;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.JsonFormat;
import edu.gla.kail.ad.Client.InputInteraction;
import edu.gla.kail.ad.Client.InteractionRequest;
import edu.gla.kail.ad.Client.InteractionRequest.Builder;
import edu.gla.kail.ad.Client.InteractionRequestOrBuilder;
import edu.gla.kail.ad.Client.InteractionResponse;
import edu.gla.kail.ad.Client.InteractionResponse.ClientMessageStatus;
import edu.gla.kail.ad.Client.InteractionType;
import edu.gla.kail.ad.Client.OutputInteraction;
import edu.gla.kail.ad.PropertiesSingleton;
import edu.gla.kail.ad.SimulatorConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;


import static edu.gla.kail.ad.Client.ClientId.WEB_SIMULATOR;

/**
 * Connect to AdCoreClient and therefore enable interaction with Agent Dialogue Core.
 * Accessible from JavaScript, through RESTful calls.
 */
@WebServlet("/ad-client-service-servlet")
public class AdCoreClientServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger( AdCoreClientServlet.class.getName() );

    private static AdCoreClient _client;

    private void addCORS(HttpServletRequest request,
                         HttpServletResponse response)
    {
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, PUT, DELETE, HEAD");
        response.addHeader("Access-Control-Allow-Headers", "Operation, Origin, X-Requested-With, Content-Type, Accept");
        response.addHeader("Access-Control-Max-Age", "1728000");
    }

    protected void doOptions(HttpServletRequest request,
                             HttpServletResponse response)
                      throws IOException
    {
        addCORS(request, response);
        response.setStatus(HttpServletResponse.SC_OK);
    }

    /**
     * Handle POST request.
     */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SimulatorConfiguration.SimulatorConfig config = PropertiesSingleton.getSimulatorConfig();
        addCORS(request, response);
        if (_client == null) {
            _client = new AdCoreClient(config.getGrpcCoreServerHost(),
                    config.getGrpcCoreServerPort());
        }
        logger.info(request.toString());
        switch (request.getHeader("operation")) {
            case "sendRequest":
                sendRequestToAgents(request, response);
                break;
            case "updateRating":
                updateRating(request, response);
                break;
            default:
                JsonObject json = new JsonObject();
                json.addProperty("message", "The Operation passed in the header is not supported.");
                response.getWriter().write(json.toString());
        }
        logger.info(response.toString());

    }

    /**
     * Handle GET request.
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        doPost(request, response);
    }

    /**
     * Add proto buffer for passed rating to the log.
     *
     * @param request
     * @param response
     */
    private void updateRating(HttpServletRequest request, HttpServletResponse response) throws
            IOException {
        LogManagerSingleton.getLogManagerSingleton().addRating(request.getParameter
                ("ratingScore"), request.getParameter
                ("responseId"), request.getParameter("experimentId"), request.getParameter
                ("requestId"));
    }

    /**
     * Send request to agent and write (return) JSON back with response and it's details.
     * Store request and response in log files.
     * TODO: Make this multi-threaded!!
     */
    private void sendRequestToAgents(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        JsonObject json = new JsonObject();
        InteractionRequest interactionRequest = getInteractionRequestFromText(request
                .getParameter("textInput"), request.getParameter("language"), request
                .getParameter("chosen_agents"), request.getParameter("agent_request_parameters"), request.getParameter("userId"));
        LogManagerSingleton.getLogManagerSingleton().addInteraction(interactionRequest, null);
        json.addProperty("interactionRequest", interactionRequest.toString());
        InteractionResponse interactionResponse;
        try {
            interactionResponse = _client.getInteractionResponse(interactionRequest);
            LogManagerSingleton.getLogManagerSingleton().addInteraction(null, interactionResponse);
            json.addProperty("message", handleResponse(interactionResponse));
            json.addProperty("interactionResponse", interactionResponse.toString());
            json.addProperty("responseId", interactionResponse.getResponseId());
            response.getWriter().write(json.toString());
        } catch (Exception exception) {
            logger.warn("Error: " + exception.getMessage());
            interactionResponse = InteractionResponse.newBuilder()
                    .setMessageStatus(ClientMessageStatus.ERROR)
                    .setErrorMessage(exception.getMessage() + "\n\n" + exception.getStackTrace())
                    .setTime(getTimeStamp())
                    .build();
            LogManagerSingleton.getLogManagerSingleton().addInteraction(null, interactionResponse);
            json.addProperty("message", "There was a fatal error! (Probably could not connect to " +
                    "the server)");
            json.addProperty("interactionResponse", interactionResponse.toString());
            response.getWriter().write(json.toString());
        }
    }

    /**
     * Creates text presented to the used depending on the MessageStatus.
     *
     * @param interactionResponse - The response obtained from agents.
     * @return String - Either the interactions passed by the agent or error message.
     * @throws Exception - Thrown when the MessageStatus is not recognised.
     */
    private String handleResponse(InteractionResponse interactionResponse) throws Exception {
        switch (interactionResponse.getMessageStatus()) {
            case ERROR:
                return interactionResponse.getErrorMessage();
            case SUCCESSFUL:
                StringBuilder concatenatedResponses = new StringBuilder();
                List<OutputInteraction> outputInteractionList = interactionResponse
                        .getInteractionList();
                for (OutputInteraction outputInteraction : outputInteractionList) {
                    concatenatedResponses.append(handleOutputInteraction(outputInteraction));
                }
                return concatenatedResponses.toString();
            default:
                return ("There was an error, contact the developer: " + interactionResponse
                        .toString());

        }
    }

    /**
     * Handle single outputInteraction (passed from outputInteractionList obtained from
     * InteractionRequest).
     */
    private String handleOutputInteraction(OutputInteraction outputInteraction) throws Exception {
        switch (outputInteraction.getType()) {
            case TEXT:
                return outputInteraction.getText();
            // TODO(Adam): Implement handling audio and action output.
            case ACTION:
                throw new Exception("Handling actions not available yet!");
            case AUDIO:
                throw new Exception("Handling audio not available yet!");
            default:
                throw new Exception("There was an error! Not recognised OutputInteraction Type!");
        }

    }

    /**
     * Helper method: create InteractionRequests from text Input.
     */
    private InteractionRequest getInteractionRequestFromText(String textInput, String
            languageCode, String chosen_agents, String agent_request_parameters, String userId)
            throws InvalidProtocolBufferException {
        StructOrBuilder agentRequestParameters = Struct.newBuilder();
        if (agent_request_parameters != null && !agent_request_parameters.equals("")) {
            JsonFormat.parser().merge(agent_request_parameters, (Struct.Builder)
                    agentRequestParameters);
        }
        return ((Builder) getInteractionRequestBuilder(InputInteraction
                .newBuilder()
                .setLanguageCode(languageCode)
                .setDeviceType(deviceType())
                .setType(InteractionType.TEXT)
                .setText(textInput)
                .build()))
                .setUserId(userId)
                .addAllChosenAgents(Arrays.asList(chosen_agents.split(",")))
                .setAgentRequestParameters(((Struct.Builder) agentRequestParameters).build())
                .build();
    }

    /**
     * Helper method: create InteractionRequestBuilders.
     */
    private InteractionRequestOrBuilder getInteractionRequestBuilder(InputInteraction
                                                                             inputInteraction) {
        return InteractionRequest.newBuilder()
                .setClientId(WEB_SIMULATOR)
                .setTime(getTimeStamp())
                .setInteraction(inputInteraction);
    }

    private Timestamp getTimeStamp() {
        return Timestamp.newBuilder()
                .setSeconds(Instant.now()
                        .getEpochSecond())
                .setNanos(Instant.now()
                        .getNano())
                .build();
    }

    /**
     * Return the device type.
     * TODO(Adam): Implement getting device type.
     */
    private String deviceType() {
        return "sampleDeviceType-to-be-implemented";
    }
}

