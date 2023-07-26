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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
        // parameters sent by the remote client
        Map<String, Value> fieldsMap = interactionRequest.getAgentRequestParameters().getFieldsMap();
        String result = "";

        // expected parameter names:
        //  api_endpoint (String): the URL for the RU-LLM API
        //  request_type (String): GET/POST
        //  request_body (protobuf Struct): the content of the request (ultimately JSON)
        //

        // default to POST
        String request_type = "POST";
        if (fieldsMap.containsKey("request_type")) {
            request_type = fieldsMap.get("request_type").getStringValue();
        }
        System.out.println(fieldsMap);

        if(!fieldsMap.containsKey("request_body") || !fieldsMap.containsKey("api_endpoint")) {
            throw new Exception("api_endpoint or request_body not specified.");
        }
        String request_body = JsonFormat.printer().preservingProtoFieldNames().print(fieldsMap.get("request_body").getStructValue());
        String api_endpoint = fieldsMap.get("api_endpoint").getStringValue();

        logger.info("Received agent parameters"  + fieldsMap);
        logger.info("Request type is " + request_type);
        logger.info("API endpoint is " + api_endpoint);
        logger.info("Request body is " + request_body);

        try {
            // Construct the url where to perform the request
            URL url = new URL(api_endpoint);

            // Start the connection
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(request_type);
            conn.setRequestProperty("Content-Type", "application/json");
            //conn.setRequestProperty("Authorization:", "Bearer <KEY>");
            conn.setDoOutput(true);

            JSONObject requestBody = new JSONObject(request_body);
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(requestBody.toString());
            wr.flush();

            // If the response is not successful we raise an error
            if (conn.getResponseCode() != 200) {
                System.out.println(conn.getResponseMessage());
                System.out.println(url.toString());
                throw new Exception("Failed : HTTP error code : " + conn.getResponseCode());
            }

            // We need now to parse the returned message from the API
            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

            result = br.lines().collect(Collectors.joining());

            conn.disconnect();

        } catch (MalformedURLException e) {
            e.printStackTrace();
            throw new Exception("Malformed API URL:" + e.getMessage());

        } catch (IOException e) {
            e.printStackTrace();
            throw new Exception("IOException:" + e.getMessage());

        }

        // TODO return proper MessageStatus if something goes wrong

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

}
