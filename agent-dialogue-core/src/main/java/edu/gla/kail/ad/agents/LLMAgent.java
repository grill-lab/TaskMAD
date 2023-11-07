package edu.gla.kail.ad.agents;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

import com.google.api.core.SettableApiFuture;
import com.google.gson.JsonObject;
import com.google.protobuf.Struct;
import com.google.protobuf.Timestamp;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.gla.kail.ad.Client;
import edu.gla.kail.ad.Client.InteractionRequest;
import edu.gla.kail.ad.Client.InteractionResponse;
import edu.gla.kail.ad.Client.InteractionType;
import edu.gla.kail.ad.Client.OutputInteraction;
import edu.gla.kail.ad.Client.InteractionResponse.ClientMessageStatus;
import edu.gla.kail.ad.CoreConfiguration.AgentConfig;
import edu.gla.kail.ad.CoreConfiguration.ServiceProvider;
import edu.gla.kail.ad.core.AgentInterface;
import edu.gla.kail.ad.core.Log.ResponseLog;
import edu.gla.kail.ad.core.Log.SystemAct;
import edu.gla.kail.ad.core.Log.ResponseLog.MessageStatus;
import io.grpc.stub.StreamObserver;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

public class LLMAgent implements AgentInterface {

    private static final Logger logger = LoggerFactory.getLogger(LLMAgent.class);

    // Hold the wizard agent's configuration, including necessary db credentials.
    private AgentConfig _agent;
    // Hold the hardcoded agent ID.
    private String _agentId = null;

    private JSONObject _llmApiEndpointInfo = null;

    public LLMAgent(AgentConfig agent) throws Exception {
        this._agent = agent;
        this._agentId = _agent.getProjectId();
        initAgent();
    }

    /**
     * Initialize the agent.
     *
     * @throws Exception
     */
    private void initAgent() throws Exception {
        // load the info from the JSON config file. this will tell the agent the request type and API
        // endpoint to use, with the request body being sent by the WoZ app
        this._llmApiEndpointInfo = new JSONObject(IOUtils.toString(new FileInputStream(_agent.getConfigurationFileURL()), "UTF-8"));
        if (_llmApiEndpointInfo.isEmpty()) {
            throw new Exception("Model API config file in the wrong format.");
        }
    }

    @Override
    public ServiceProvider getServiceProvider() {
        return _agent.getServiceProvider();
    }

    @Override
    public String getAgentId() {
        return _agentId;
    }

    @Override
    public ResponseLog getResponseFromAgent(InteractionRequest interactionRequest) throws Exception {
        // parameters sent by the remote client. the only field we're expecting here is 
        // the request_body, but the request_type and api_endpoint can be overriden by
        // values provided in the request parameters.
        Map<String, Value> fieldsMap = interactionRequest.getAgentRequestParameters().getFieldsMap();
        String result = "";

        String request_type = this._llmApiEndpointInfo.getString("request_type");
        if(fieldsMap.containsKey("request_type")) {
            logger.info("Overriding request_type");
            request_type = fieldsMap.get("request_type").getStringValue();
        }

        String api_endpoint = this._llmApiEndpointInfo.getString("api_endpoint");
        if(fieldsMap.containsKey("api_endpoint")) {
            logger.info("Overriding api_endpoint");
            api_endpoint = fieldsMap.get("api_endpoint").getStringValue();
        }

        // must have a request_body for a valid request
        if(!fieldsMap.containsKey("request_body")) {
            throw new Exception("request_body not found in request parameters!");
        }
        String request_body = JsonFormat.printer().preservingProtoFieldNames().print(fieldsMap.get("request_body").getStructValue());

        logger.info("Received agent parameters"  + fieldsMap);
        logger.info("Request type is " + request_type);
        logger.info("API endpoint is " + api_endpoint);
        logger.info("Request body is " + request_body);

        try {
            HttpClient client = HttpClient.newBuilder().build();
            HttpRequest request = HttpRequest.newBuilder(new URI(api_endpoint))
                                    .version(HttpClient.Version.HTTP_2)
                                    .POST(HttpRequest.BodyPublishers.ofString(request_body))
                                    .headers("Content-Type", "application/json")
                                    .timeout(Duration.ofMinutes(2))
                                    .build();

            HttpResponse response = client.send(request, HttpResponse.BodyHandlers.ofString());
            logger.info("HttpClient response is " + response.statusCode());
            if(response.statusCode() != 200) {
                throw new Exception("Failed : HTTP error code : " + response.statusCode());
            }

            logger.info("HttpClient body is " + response.body());
            result = (String)response.body();
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("HTTP error: " + e.getMessage());
        }

        Struct.Builder builder = Struct.newBuilder();
        JsonFormat.parser().merge(result, builder);

        // Return a ResponseLog object
        return ResponseLog.newBuilder().setClientId(Client.ClientId.EXTERNAL_APPLICATION)
                .setServiceProvider(ServiceProvider.LLM).setMessageStatus(MessageStatus.SUCCESSFUL)
                .setRawResponse(result).addAction(SystemAct.newBuilder().setInteraction(OutputInteraction.newBuilder()
                        .setType(InteractionType.TEXT).setText(result).setUnstructuredResult(builder).build()))
                .build();
    }

    @Override
    public void streamingResponseFromAgent(InteractionRequest interactionRequest,
            StreamObserver<InteractionResponse> responseObserver) throws Exception {

    }

    @Override
    public void endSession() {
    }

}
