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

public class ModelInteractionAgent implements AgentInterface {

    private static final Logger logger = LoggerFactory.getLogger(RestSearchAgent.class);

    // Hold the wizard agent's configuration, including necessary db credentials.
    private AgentConfig _agent;
    // Gold the hardcoded agent ID.
    private String _agentId = null;

    private JSONObject _supportedAPiEndpoints = null;

    public ModelInteractionAgent(AgentConfig agent) throws Exception {
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
        this._supportedAPiEndpoints = new JSONObject(
                IOUtils.toString(new FileInputStream(_agent.getConfigurationFileURL()), "UTF-8"));

        if (_supportedAPiEndpoints.isEmpty()) {
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
        // We need to perform a call based on the request specified.
        // The function handles any type of specific call. The only three parameters
        // that must be specified as agentRequestParameters are:
        // 1) model_name: which specifies which model should be called. The name is 
        //                  specific to the one that we specified in model_api_config.json
        // 2) api_endpoint: The api endpoint that we want to call 
        // 3) request_body: the specific request body that need to be passed to the api

        Map<String, Value> fieldsMap = interactionRequest.getAgentRequestParameters().getFieldsMap();
        String result = "";
        // Get request type
        String request_type = "POST";
        if(fieldsMap.containsKey("request_type")){
            request_type = fieldsMap.get("request_type").getStringValue();
        }
        // If these two parameters are not specified we return an error
        if (fieldsMap.containsKey("model_name") && fieldsMap.containsKey("api_endpoint") && fieldsMap.containsKey("request_body")) {
            try {
                // Get the model name 
                String modelName = fieldsMap.get("model_name").getStringValue();
                String apiEndpoint = fieldsMap.get("api_endpoint").getStringValue();
                String requestBodyString = JsonFormat.printer().preservingProtoFieldNames()
                        .print(fieldsMap.get("request_body").getStructValue());

                // Check that none of the fields is empty and that the model is actually specified in the config file 
                if(modelName.isEmpty() || 
                apiEndpoint.isEmpty() || 
                requestBodyString.isEmpty() || 
                !_supportedAPiEndpoints.has(modelName)) throw  new Exception("Invalid config file format");


                JSONObject modelConfig = _supportedAPiEndpoints.getJSONObject(modelName);

                // Construct the url where to perform the request
                URL url = new URL(modelConfig.getString("root_url")
                        + modelConfig.getJSONObject("api_endpoints").getString(apiEndpoint));

                // Start the connection 
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod(request_type);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);

                JSONObject requestBody = new JSONObject(requestBodyString);
                OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
                wr.write(requestBody.toString());
                wr.flush();

                // If the response is not successful we raise an error
                if (conn.getResponseCode() != 200) {
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

        } else {
            throw new Exception("Model name, api endpoint or request body not specified.");
        }

        Struct.Builder builder = Struct.newBuilder();
        JsonFormat.parser().merge(result, builder);

        // Return a ResponseLog object
        return ResponseLog.newBuilder().setClientId(Client.ClientId.EXTERNAL_APPLICATION)
                .setServiceProvider(ServiceProvider.MODEL_INFERENCE).setMessageStatus(MessageStatus.SUCCESSFUL)
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
