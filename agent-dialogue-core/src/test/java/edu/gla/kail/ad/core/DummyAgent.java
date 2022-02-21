/*
package edu.gla.kail.ad.core;

import com.google.protobuf.Timestamp;
import edu.gla.kail.ad.Client;
import edu.gla.kail.ad.Client.InputInteraction;
import edu.gla.kail.ad.Client.InteractionType;
import edu.gla.kail.ad.Client.OutputInteraction;
import edu.gla.kail.ad.core.Log.ResponseLog;
import edu.gla.kail.ad.CoreConfiguration.Agent.ServiceProvider;;
import edu.gla.kail.ad.core.Log.Slot;
import edu.gla.kail.ad.core.Log.SystemAct;

import java.time.Instant;


*/
/**
 * This is a dummy agent created for testing purposes.
 * Returns valid inputInteraction.
 *//*

class DummyAgent implements AgentInterface {
    private Timestamp timestamp = Timestamp.newBuilder()
            .setSeconds(Instant.now()
                    .getEpochSecond())
            .setNanos(Instant.now()
                    .getNano())
            .build();

    @Override
    public ServiceProvider getServiceProvider() {
        return ServiceProvider.DUMMYAGENT;
    }

    @Override
    public ResponseLog getResponseFromAgent(InputInteraction inputInteraction) {
        return ResponseLog.newBuilder()
                .setResponseId("ResponseId set by DummyBuilder")
                .setTime(timestamp)
                .setClientId(Client.ClientId.EXTERNAL_APPLICATION)
                .setServiceProvider(ServiceProvider.DUMMYAGENT)
                .setRawResponse("RawResponse set by DummyBuilder")
                .addAction(SystemAct.newBuilder()
                        .setAction("Action set by DummyBuilder")
                        .setInteraction(OutputInteraction.newBuilder()
                                .setText("Text set by DummyBuilder")
                                .setType(InteractionType.TEXT).build())
                        .addSlot(Slot.newBuilder()
                                .setName("SlotName set by DummyBuilder")
                                .setValue("SlotValue set by DummyBuilder").build())
                        .build())
                .build();
    }
}
*/
