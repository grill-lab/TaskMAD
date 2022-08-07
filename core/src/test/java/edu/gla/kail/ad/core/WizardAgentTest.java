/*
package edu.gla.kail.ad.core;

import com.google.api.core.SettableApiFuture;
import com.google.protobuf.Struct;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.JsonFormat;
import edu.gla.kail.ad.Client;
import edu.gla.kail.ad.Client.InputInteraction;
import edu.gla.kail.ad.Client.InteractionRequest;
import edu.gla.kail.ad.Client.InteractionType;
import edu.gla.kail.ad.CoreConfiguration.AgentConfig;
import edu.gla.kail.ad.CoreConfiguration.ServiceProvider;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import edu.gla.kail.ad.core.Log.ResponseLog;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class WizardAgentTest {

  @Test
  public void testGetResponseFromAgent() throws Exception {

    JSONObject agentRequestJson = new JSONObject();
    agentRequestJson.put("conversation_id", "mytestconversation");

    Struct.Builder builder = Struct.newBuilder();
    JsonFormat.parser().merge(agentRequestJson.toString(), builder);

    InteractionRequest request = InteractionRequest.newBuilder()
            .setTime(Timestamp.newBuilder()
                    .setSeconds(Instant.now().getEpochSecond())
                    .setNanos(Instant.now().getNano())
                    .build())
            .setUserId("testuser1")
            .setClientId(Client.ClientId.EXTERNAL_APPLICATION)
            .setAgentRequestParameters(builder.build())
            .setInteraction(InputInteraction.newBuilder()
                    .setType(InteractionType.TEXT)
                    .setText("Hi")
                    .setDeviceType("iPhone whatever")
                    .setLanguageCode("en-US"))
            .build();

    AgentConfig testConfig = AgentConfig.newBuilder()
            .setProjectId("testproject")
            .setServiceProvider(ServiceProvider.WIZARD)
            .setConfigurationFileURL("src/main/resources/agentdialogue-2cd4b-firebase-adminsdk-z39zw" +
                    "-4d5427d1fc.json")
            .build();

    WizardAgent wizardAgent = new WizardAgent("magicsessionid");
    assertTrue("The response from Wizard is valid", wizardAgent
            .getResponseFromAgent(request).isInitialized());
  }

//    WizardAgent user = new WizardAgent("magicsessionid", testConfig);
//    Log.ResponseLog wizardResponse = null;
//    SettableApiFuture<ResponseLog> responseFuture =  SettableApiFuture.create();
//    responseFuture.set(user.getResponseFromAgent(request));
//    wizardResponse = responseFuture.get(120, TimeUnit.SECONDS);
//    assertTrue("The response from Wizard is valid", wizardResponse.isInitialized());
//
//
//    InteractionRequest wizardReply = InteractionRequest.newBuilder()
//            .setTime(Timestamp.newBuilder()
//                    .setSeconds(Instant.now().getEpochSecond())
//                    .setNanos(Instant.now().getNano())
//                    .build())
//            .setUserId("wizard1")
//            .setClientId(Client.ClientId.EXTERNAL_APPLICATION)
//            .setAgentRequestParameters(builder.build())
//            .setInteraction(InputInteraction.newBuilder()
//                    .setType(InteractionType.TEXT)
//                    .setText("I am the wizard")
//                    .setDeviceType("iPhone whatever")
//                    .setLanguageCode("en-US"))
//            .build();
//
//    WizardAgent user = new WizardAgent("magicsessionid", testConfig);
//    Log.ResponseLog wizardResponse = null;
//    SettableApiFuture<ResponseLog> responseFuture =  SettableApiFuture.create();
//    responseFuture.set(user.getResponseFromAgent(request));
//    wizardResponse = responseFuture.get(120, TimeUnit.SECONDS);
//    assertTrue("The response from Wizard is valid", wizardResponse.isInitialized());
//
//
//  }
}
*/
