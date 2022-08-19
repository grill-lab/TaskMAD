package edu.gla.kail.ad.agents;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.google.protobuf.Message;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.gla.kail.ad.Client;
import edu.gla.kail.ad.AgentsConfig.ExternalServicesConfig;
import edu.gla.kail.ad.AgentsConfig.ExternalServicesConfig.ApiEndpoint;
import edu.gla.kail.ad.AgentsConfig.ExternalServicesConfig.ApiRequestMethod;
import edu.gla.kail.ad.AgentsConfig.ExternalServicesConfig.ExternalService;
import edu.gla.kail.ad.Client.InteractionRequest;
import edu.gla.kail.ad.Client.InteractionResponse;
import edu.gla.kail.ad.Client.InteractionType;
import edu.gla.kail.ad.Client.OutputInteraction;
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
    private AgentConfig agent;
    // Gold the hardcoded agent ID.
    private String agentId;
    private ExternalServicesConfig externalServicesConfig;

    public ExternalServicesAgent(AgentConfig agent) throws Exception {
        this.agent = agent;
        this.agentId = this.agent.getProjectId();
        initAgent();
    }

    private ExternalServicesConfig buildExternalServicesConfig(String filePath) throws IOException {
        ExternalServicesConfig.Builder externalServicesConfigBuilder = ExternalServicesConfig.newBuilder();
        String jsonText = IOUtils.toString(new FileInputStream(filePath), StandardCharsets.UTF_8);
        JsonFormat.parser().merge(jsonText, externalServicesConfigBuilder);
        return externalServicesConfigBuilder.build();
    }

    /**
     * Initialize the agent.
     *
     * @throws Exception
     */
    private void initAgent() throws Exception {

        this.externalServicesConfig = this.buildExternalServicesConfig(this.agent.getConfigurationFileURL());
        if (!this.isAgentConfigFileValid(this.externalServicesConfig)) {
            throw new Exception("External Services Agent Config file in the wrong format");
        }
    }

    @Override
    public ServiceProvider getServiceProvider() {
        return this.agent.getServiceProvider();
    }

    @Override
    public String getAgentId() {
        return this.agentId;
    }

    private ExternalService getExternalServiceByName(String name) {
        for (ExternalService es : this.externalServicesConfig.getExternalServicesList()) {
            if (es.getName().equals(name)) {
                return es;
            }
        }
        return null;
    }

    private ApiEndpoint getServiceApiEndpointByName(ExternalService ea, String name) {
        for (ApiEndpoint aep : ea.getApiEndpointsList()) {
            if (aep.getName().equals(name)) {
                return aep;
            }
        }
        return null;
    }

    @Override
    public ResponseLog getResponseFromAgent(InteractionRequest interactionRequest) throws Exception {
        // We need to perform a call based on the request specified.
        // The function handles any type of specific call. The only three parameters
        // that must be specified as agentRequestParameters are:
        // 1) service_name: which specifies which service should be called. The name is
        // specific to the one that we specified in search_api_config.json
        // 2) api_endpoint: The api endpoint that we want to call
        // 3) request_body: the specific request body that need to be passed to the api

        // If these two parameters are not specified we return an error
        Map<String, Value> fieldsMap = interactionRequest.getAgentRequestParameters().getFieldsMap();
        String result = "";
        if (fieldsMap.containsKey("service_name") && fieldsMap.containsKey("api_endpoint")
                && fieldsMap.containsKey("request_body")) {
            try {
                // Get the service name
                String requestServiceName = fieldsMap.get("service_name").getStringValue();
                String requestApiEndpoint = fieldsMap.get("api_endpoint").getStringValue();
                String requestBodyString = JsonFormat.printer().preservingProtoFieldNames()
                        .print(fieldsMap.get("request_body").getStructValue());

                // Check that none of the fields is empty and that the model is actually
                // specified in the config file
                if (Utils.isBlank(requestServiceName) || Utils.isBlank(requestApiEndpoint)
                        || Utils.isBlank(requestBodyString))
                    throw new Exception("Invalid parameters provided");

                // We now need to extract the selected service name
                ExternalService externalService = this.getExternalServiceByName(requestServiceName);
                if (externalService == null) {
                    throw new Exception("Invalid service name provided");
                }

                // Now we need to extract the api endpoint
                ApiEndpoint apiEndpoint = this.getServiceApiEndpointByName(externalService, requestApiEndpoint);
                if (apiEndpoint == null) {
                    throw new Exception("Invalid api endpoint provided");
                }

                // Construct the url where to perform the request
                URL url = new URL(externalService.getRootUrl() + apiEndpoint.getEndpoint());

                String requestMethod = apiEndpoint.getRequestMethod().name();

                // Start the connection
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod(requestMethod);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);

                JSONObject requestBody = new JSONObject(requestBodyString);
                OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
                wr.write(requestBody.toString());
                wr.flush();

                // If the response is not successful we raise an error
                if (conn.getResponseCode() != 200) {
                    logger.error(conn.getResponseMessage());
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
    public boolean isAgentConfigFileValid(Message agentConfig) {

        if (agentConfig instanceof ExternalServicesConfig) {
            // Now we need to check that all the external services are valid
            ExternalServicesConfig externalServicesConfigObj = (ExternalServicesConfig) agentConfig;
            List<ExternalService> externalServices = externalServicesConfigObj.getExternalServicesList();
            // Validate all the external services
            for (ExternalService ea : externalServices) {
                if (!Utils.isBlank(ea.getName()) && !Utils.isBlank(ea.getRootUrl())) {
                    // Check all the apiEndpoints
                    List<ApiEndpoint> apiEndpoints = ea.getApiEndpointsList();
                    for (ApiEndpoint aep : apiEndpoints) {
                        if (Utils.isBlank(aep.getName()) || Utils.isBlank(aep.getEndpoint())
                                || !(aep.getRequestMethod() instanceof ApiRequestMethod)) {
                            // If any of these conditions is false it means the file is not valid so we
                            // return
                            return false;
                        }
                    }
                    return true;
                }
            }
        }
        return false;

    }

}
