package edu.gla.kail.ad.core;

import edu.gla.kail.ad.Client;
import edu.gla.kail.ad.Client.InteractionRequest;
import edu.gla.kail.ad.CoreConfiguration.ServiceProvider;
import edu.gla.kail.ad.core.Log.ResponseLog;
import io.grpc.stub.StreamObserver;

/**
 * Agent interface is a common interface to different dialogue framework implementations.
 */
public interface AgentInterface {
    ServiceProvider _serviceProvider = null; // Specifies the dialogue framework implementation.
    String _agentId = null; // The unique ID of a particular agent.

    ServiceProvider getServiceProvider(); // Return the service provider type of the instance.

    String getAgentId(); // Return the agentID of the instance (e.g. projectID).

    /**
     * Return a response for a request.
     *
     * @param interactionRequest - A data structure (implemented in log.proto) holding the
     *         incoming interaction that is being sent to an agent.
     * @return ResponseLog - The response from the agent, must be non-null. ResponseLog is a data
     *         structure implemented in log.proto.
     * @throws Exception
     */
    ResponseLog getResponseFromAgent(InteractionRequest interactionRequest) throws Exception;

    /**
     * Create a streaming response setup for the agent. Messages will be written to the observer.
     *
     * @param interactionRequest - Initial setup to setup a streaming response pipeline.
     * @throws Exception
     */
    void streamingResponseFromAgent(InteractionRequest interactionRequest,
                                StreamObserver<Client.InteractionResponse> responseObserver) throws Exception;
}