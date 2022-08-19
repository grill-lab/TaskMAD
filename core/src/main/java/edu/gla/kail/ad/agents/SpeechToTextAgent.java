package edu.gla.kail.ad.agents;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;

import edu.gla.kail.ad.Client;
import edu.gla.kail.ad.AgentsConfig.SpeechToTextConfig;
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

public class SpeechToTextAgent implements AgentInterface {

    private static final Logger logger = LoggerFactory.getLogger(SpeechToTextAgent.class);

    // Hold the wizard agent's configuration, including necessary db credentials.
    private AgentConfig agent;
    // Gold the hardcoded agent ID.
    private String agentId;

    private ServiceAccountCredentials googleCredentials;
    private SpeechToTextConfig speechToTextConfig;

    public SpeechToTextAgent(AgentConfig agent) throws Exception {
        this.agent = agent;
        this.agentId = this.agent.getProjectId();
        initAgent();
    }

    private SpeechToTextConfig buildSpeechToTextConfig(String filePath) throws IOException {
        SpeechToTextConfig.Builder speechToTextConfigBuilder = SpeechToTextConfig.newBuilder();
        String jsonText = IOUtils.toString(new FileInputStream(filePath), StandardCharsets.UTF_8);
        JsonFormat.parser().merge(jsonText, speechToTextConfigBuilder);
        return speechToTextConfigBuilder.build();
    }

    private ServiceAccountCredentials getServiceAccountCredentials(String credentialsFile) throws IOException {
        return ServiceAccountCredentials.fromStream(new FileInputStream(credentialsFile));
    }

    /**
     * Initialize the agent.
     *
     * @throws Exception
     */
    private void initAgent() throws Exception {

        this.speechToTextConfig = this.buildSpeechToTextConfig(this.agent.getConfigurationFileURL());
        if (this.isAgentConfigFileValid(this.speechToTextConfig)) {

            this.googleCredentials = this.getServiceAccountCredentials(this.speechToTextConfig.getServerKey());
        } else {
            throw new Exception("Speech-To-Text Agent Config file in the wrong format");
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

    @Override
    public ResponseLog getResponseFromAgent(InteractionRequest interactionRequest) throws Exception {

        String resultText = "";
        try {
            SpeechToTextGoogleAPI speechToTextClient = new SpeechToTextGoogleAPI(this.googleCredentials);
            resultText = speechToTextClient.speechToText(interactionRequest.getInteraction().getAudioBase64());
        } catch (Exception e) {
            logger.error(e.toString());
        }

        // Return a ResponseLog object
        return ResponseLog.newBuilder().setClientId(Client.ClientId.EXTERNAL_APPLICATION)
                .setServiceProvider(ServiceProvider.SPEECH_TO_TEXT).setMessageStatus(MessageStatus.SUCCESSFUL)
                .setRawResponse(resultText)
                .addAction(SystemAct.newBuilder().setInteraction(
                        OutputInteraction.newBuilder().setType(InteractionType.TEXT).setText(resultText).build()))
                .build();

    }

    @Override
    public void streamingResponseFromAgent(InteractionRequest interactionRequest,
            StreamObserver<InteractionResponse> responseObserver) throws Exception {

    }

    @Override
    public boolean isAgentConfigFileValid(Message agentConfig) {
        if (agentConfig instanceof SpeechToTextConfig) {
            SpeechToTextConfig speechToTextConfigObj = (SpeechToTextConfig) agentConfig;
            return !Utils.isBlank(speechToTextConfigObj.getServerKey());
        }
        return false;

    }

}
