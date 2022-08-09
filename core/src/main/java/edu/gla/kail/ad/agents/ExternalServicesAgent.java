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
import edu.gla.kail.ad.service.Utils;
import io.grpc.stub.StreamObserver;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

public class ExternalServicesAgent implements AgentInterface {

    private static final Logger logger = LoggerFactory.getLogger(ExternalServicesAgent.class);

    // Hold the wizard agent's configuration, including necessary db credentials.
    private AgentConfig _agent;
    // Gold the hardcoded agent ID.
    private String _agentId = null;

    private JSONObject _supportedAPiEndpoints = null;

    public ExternalServicesAgent(AgentConfig agent) throws Exception {
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

        if(this.isAgentConfigFileValid(_agent)){

            FileInputStream file = new FileInputStream(this._agent.getConfigurationFileURL()); 
            this._supportedAPiEndpoints = new JSONObject(
                IOUtils.toString(file, "UTF-8"));
        }else{
            throw new Exception("External Services Agent Config file in the wrong format");
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
        // 1) service_name: which specifies which service should be called. The name is 
        //                  specific to the one that we specified in search_api_config.json
        // 2) api_endpoint: The api endpoint that we want to call 
        // 3) request_body: the specific request body that need to be passed to the api

// If these two parameters are not specified we return an error
        Map<String, Value> fieldsMap = interactionRequest.getAgentRequestParameters().getFieldsMap();
        String result = "";
        if (fieldsMap.containsKey("service_name") && fieldsMap.containsKey("api_endpoint") && fieldsMap.containsKey("request_body")) {
            try {
                // Get the service name 
                String serviceName = fieldsMap.get("service_name").getStringValue();
                String apiEndpoint = fieldsMap.get("api_endpoint").getStringValue();
                String requestBodyString = JsonFormat.printer().preservingProtoFieldNames()
                        .print(fieldsMap.get("request_body").getStructValue());

                // Check that none of the fields is empty and that the model is actually specified in the config file 
                if(Utils.isBlank(serviceName) || 
                Utils.isBlank(apiEndpoint) || 
                Utils.isBlank(requestBodyString) || 
                !_supportedAPiEndpoints.has(serviceName)) throw  new Exception("Invalid parameters provided");


                // Get the specific service configuration
                JSONObject modelConfig = _supportedAPiEndpoints.getJSONObject(serviceName);

                // Construct the url where to perform the request
                URL url = new URL(modelConfig.getString("root_url")
                        + modelConfig.getJSONObject("api_endpoints").getJSONObject(apiEndpoint).getString("endpoint"));

                String request_method = modelConfig.getJSONObject("api_endpoints").getJSONObject(apiEndpoint).getString("request_method").toUpperCase();


                // Start the connection 
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod(request_method);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);

                JSONObject requestBody = new JSONObject(requestBodyString);
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

        } else {
            throw new Exception("Service name, api endpoint or request body not specified.");
        }

        Struct.Builder builder = Struct.newBuilder();
        JsonFormat.parser().merge(result, builder);

        // Return a ResponseLog object
        return ResponseLog.newBuilder().setClientId(Client.ClientId.EXTERNAL_APPLICATION)
                .setServiceProvider(ServiceProvider.EXTERNAL_SERVICES).setMessageStatus(MessageStatus.SUCCESSFUL)
                .setRawResponse(result).addAction(SystemAct.newBuilder().setInteraction(OutputInteraction.newBuilder()
                        .setType(InteractionType.TEXT).setText(result).setUnstructuredResult(builder).build()))
                .build();

    }

    @Override
    public void streamingResponseFromAgent(InteractionRequest interactionRequest,
            StreamObserver<InteractionResponse> responseObserver) throws Exception {

    }

    @Override
    public boolean isAgentConfigFileValid(AgentConfig config) {
        if(config != null && config.getConfigurationFileURL() != null && !Utils.isBlank(config.getConfigurationFileURL())){
            // Get the agent specific configuration 
            JSONObject agentConfiguration;
            try {
              agentConfiguration = new JSONObject(
                IOUtils.toString(new FileInputStream(config.getConfigurationFileURL()), "UTF-8"));
                    // Custom Checks for this specific config file
                    for(String key: agentConfiguration.keySet()){
                        // Extract the service and check if it's in the valid format.
                        JSONObject service = agentConfiguration.getJSONObject(key);
                        // Independently from the service, and external service must have
                        // 1. A root_url
                        // 2. On or more api_endpoints 
                        // 3. A logs_folder
                        if(service.has("root_url") && service.getString("root_url") != null && !Utils.isBlank(service.getString("root_url"))){
                            // Get the API endpoints
                            if(service.has("api_endpoints")){
                                JSONObject apiEndpoints = service.getJSONObject("api_endpoints");
                                // Check for each endpoint if they have a request method and specific endpoint path
                                for(String apiEndpointStr: apiEndpoints.keySet()){
                                    JSONObject apiEnpointOjb = apiEndpoints.getJSONObject(apiEndpointStr);
                                    if(!apiEnpointOjb.has("request_method") || !apiEnpointOjb.has("endpoint") 
                                    || apiEnpointOjb.getString("request_method") == null || apiEnpointOjb.getString("endpoint") == null
                                    || Utils.isBlank(apiEnpointOjb.getString("request_method")) || Utils.isBlank(apiEnpointOjb.getString("endpoint"))){
                                        return false;
                                    }            
                                }
                                // Lastly check if we have the logs_folder
                                if(service.has("logs_folder") && service.getString("logs_folder") != null && !Utils.isBlank(service.getString("logs_folder"))){
                                    return true;
                                }

                            }else{
                                return false;
                            }
                        }else{
                            return false;
                        }
                    }
                }
            catch (Exception e) {
              return false;
            }
          }
          return false;
    }

}
