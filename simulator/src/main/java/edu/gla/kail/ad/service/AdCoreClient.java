package edu.gla.kail.ad.service;

import edu.gla.kail.ad.Client.InteractionRequest;
import edu.gla.kail.ad.Client.InteractionResponse;
import edu.gla.kail.ad.service.AgentDialogueGrpc.AgentDialogueBlockingStub;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.util.concurrent.TimeUnit;

public class AdCoreClient {
    private final ManagedChannel _channel;
    private final AgentDialogueBlockingStub _blockingStub; // gRPC will wait for the server to
    // respond; return response or raise an exception.

    public AdCoreClient(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port));
    }

    public AdCoreClient(ManagedChannelBuilder<?> channelBuilder) {
        _channel = channelBuilder.build();
        _blockingStub = AgentDialogueGrpc.newBlockingStub(_channel);
    }


    /**
     * Shut the channel down after specified number of seconds (5 in this case).
     *
     * @throws InterruptedException
     */
    public void shutdown() throws InterruptedException {
        _channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    /**
     * Get Interaction response.
     *
     * @param interactionRequest - The request sent to the Agent Dialog Manager.
     * @return interactionResponse - The response from an Agent chosen by DialogAgentManager.
     * @throws Exception
     */
    public InteractionResponse getInteractionResponse(InteractionRequest interactionRequest)
            throws Exception {
        InteractionResponse interactionResponse;
        try {
            interactionResponse = _blockingStub.getResponseFromAgents(interactionRequest);
            return interactionResponse;
        } catch (StatusRuntimeException e) {
            e.printStackTrace();
            throw new Exception("Error occured: " + e.getStatus() + e.getMessage());
        }
    }
}