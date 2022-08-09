package edu.gla.kail.ad.agents;

import java.io.FileInputStream;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import com.google.auth.oauth2.ServiceAccountCredentials;

import edu.gla.kail.ad.Client;
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
import edu.gla.kail.ad.service.speechToText.SpeechToTextGoogleAPI;
import io.grpc.stub.StreamObserver;


public class SpeechToTextAgent implements AgentInterface{

    // Hold the wizard agent's configuration, including necessary db credentials.
    private AgentConfig _agent;
    // Gold the hardcoded agent ID.
    private String _agentId = null;

    private ServiceAccountCredentials googleCredentials = null;

    private String _logs_folder = null;

    public SpeechToTextAgent(AgentConfig agent) throws Exception {
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
        if(this.isAgentConfigFileValid(this._agent)){

            JSONObject agentConfiguration = new JSONObject(
            IOUtils.toString(new FileInputStream(this._agent.getConfigurationFileURL()), "UTF-8"));

            String credentialsFile = agentConfiguration.getString("key");
            this._logs_folder = agentConfiguration.getString("logs_folder");
            
            this.googleCredentials = ServiceAccountCredentials.fromStream(new FileInputStream(credentialsFile));
        }else{
            throw new Exception("Speech-To-Text Agent Config file in the wrong format");
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
       
        String resultText = "";
        try  {
            SpeechToTextGoogleAPI speechToTextClient = new SpeechToTextGoogleAPI(this.googleCredentials);
            resultText = speechToTextClient.speechToText(interactionRequest.getInteraction().getAudioBase64());
        }catch(Exception e){
            System.out.println(e);
        }
          
        // Return a ResponseLog object
        return ResponseLog.newBuilder().setClientId(Client.ClientId.EXTERNAL_APPLICATION)
                .setServiceProvider(ServiceProvider.SPEECH_TO_TEXT).setMessageStatus(MessageStatus.SUCCESSFUL)
                .setRawResponse(resultText).addAction(SystemAct.newBuilder().setInteraction(OutputInteraction.newBuilder()
                        .setType(InteractionType.TEXT).setText(resultText).build()))
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
              if(agentConfiguration.has("key") && agentConfiguration.has("logs_folder")){
                if(agentConfiguration.getString("key") != null && !Utils.isBlank(agentConfiguration.getString("key"))
                && agentConfiguration.getString("logs_folder") != null && !Utils.isBlank(agentConfiguration.getString("logs_folder"))){
                  return true;
                }
              }
              
            } catch (Exception e) {
              return false;
            }
        }
        return false;
    }

}
