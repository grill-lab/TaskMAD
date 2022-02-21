package edu.gla.kail.ad.service;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.JsonFormat;
import edu.gla.kail.ad.Client.InteractionRequest;
import edu.gla.kail.ad.Client.InteractionResponse;
import edu.gla.kail.ad.Client.InteractionResponse.ClientMessageStatus;
import edu.gla.kail.ad.core.DialogAgentManager;
import edu.gla.kail.ad.core.Log.ResponseLog;
import edu.gla.kail.ad.core.LogTurnManagerSingleton;
import edu.gla.kail.ad.core.PropertiesSingleton;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The server which handles gRPC calls.
 */
public class AgentDialogueServer {
    private final Server _server;

    private static final Logger logger = LoggerFactory.getLogger( AgentDialogueServer.class);

    /**
     * Create a localhost server listening on specified port.
     *
     * @param port - the integer specifying the port.
     */
    public AgentDialogueServer(int port) {
        this(ServerBuilder.forPort(port));
    }

    /**
     * Create a localhost server listening on specified port.
     *
     * @param serverBuilder - the builder created for a particular port.
     */
    private AgentDialogueServer(ServerBuilder<?> serverBuilder) {
        _server = serverBuilder.addService(new AgentDialogueService()).build();
    }

    public static void main(String[] args) throws Exception {
        if (args == null || args.length == 0) {
            throw new Exception("Please specify the URL to the configuration file.");
        }
        logger.info("Loading config file from:" + args[0]);
        PropertiesSingleton.getPropertiesSingleton(new URL(args[0]));
        logger.info("Configuration loaded: " + PropertiesSingleton.getCoreConfig().toString());
        AgentDialogueServer server = new AgentDialogueServer(PropertiesSingleton.getCoreConfig()
                .getGrpcServerPort());
        server.start();
        server.blockUntilShutdown();
    }

    /**
     * Start the server.
     *
     * @throws IOException - Thrown when the server cannot start properly.
     */
    public void start() throws IOException {
        logger.info("Starting server");

        _server.start();
        logger.info("Started server.");
        Runtime.getRuntime().addShutdownHook(new Thread() {
            // In case the JVM is being shut down
            @Override
            public void run() {
                System.err.println("Server shut down due to JVM being shut down.");
                shutDown();
            }
        });
    }

    /**
     * Shut down the server.
     */
    public void shutDown() {
        logger.info("Shutting down server");

        if (_server != null) {
            try {
                LogTurnManagerSingleton.getLogTurnManagerSingleton().saveAndExit();
            } catch (IOException exception) {
                System.err.println("Unable to close output stream for log storing.");
            }
            _server.shutdown();
        }
    }

    /**
     * Wait until main thread is terminated. (gRPC is based on daemon threads)
     *
     * @throws InterruptedException
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (_server != null) {
            _server.awaitTermination();
        }
    }

    /**
     * Serves the requests from clients/users.
     */
    static class AgentDialogueService extends AgentDialogueGrpc.AgentDialogueImplBase {
        @Override
        public void endSession(UserID userId, StreamObserver<UserID> responseObserver) {
            if (checkNotNull(userId.getUserId(), "The UserID that have " +
                    "been sent is null!").isEmpty()) {
                throw new IllegalArgumentException("The provided userID is empty!");
            }
            boolean deletingWasSuccessful = DialogAgentManagerSingleton.deleteDialogAgentManager
                    (userId.getUserId());
            responseObserver.onNext(userId.toBuilder().setActiveSession(!deletingWasSuccessful)
                    .build());
            responseObserver.onCompleted();
        }

        /**
         * Sends the request to the agents and retrieves the chosen response.
         * TODO(Adam): This method may cause the calls not to be asynchronous. Check it!
         *
         * @param interactionRequest - The instance of InteractionRequest passed by the
         *                           user/client to the agents.
         * @param responseObserver   - The instance, which is used to pass the instance of
         *                           InteractionResponse with the response from the agents.
         */
        @Override
        public void getResponseFromAgents(InteractionRequest interactionRequest,
                                          StreamObserver<InteractionResponse> responseObserver) {
            try {
                String jsonString = JsonFormat.printer()
                        .preservingProtoFieldNames()
                        .print(interactionRequest);

                logger.info("Processing request:" + jsonString);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid protobuffer request!");
            }
            checkNotNull(interactionRequest.getUserId(), "The InteractionRequest that have " +
                    "been sent doesn't have userID!");
            DialogAgentManager dialogAgentManager;
            try {
                dialogAgentManager = DialogAgentManagerSingleton
                        .getDialogAgentManager(interactionRequest.getUserId());
            } catch (Exception exception) {
                exception.printStackTrace();
                dialogAgentManager = null;
            }
            checkNotNull(dialogAgentManager, "The initialization of the DialogAgentManager " +
                    "failed!");
            ResponseLog response;
            InteractionResponse interactionResponse;
            Timestamp timestamp = Timestamp.newBuilder()
                    .setSeconds(Instant.now()
                            .getEpochSecond())
                    .setNanos(Instant.now()
                            .getNano())
                    .build();
            try {
                response = dialogAgentManager.getResponse(interactionRequest);
                interactionResponse = InteractionResponse.newBuilder()
                        .setResponseId(response.getResponseId())
                        .setSessionId(dialogAgentManager.getSessionId())
                        .setTime(timestamp)
                        .setClientId(response.getClientId())
                        .setUserId(interactionRequest.getUserId())
                        .setMessageStatus(ClientMessageStatus.SUCCESSFUL)
                        .addAllInteraction(response.getActionList().stream()
                                .map(action -> action.getInteraction())
                                .collect(Collectors.toList()))
                        .build();
            } catch (Exception exception) {
                logger.warn("Error processing request :" + exception.getMessage() + " " + exception.getMessage());

                interactionResponse = InteractionResponse.newBuilder()
                        .setMessageStatus(InteractionResponse.ClientMessageStatus.ERROR)
                        .setErrorMessage(exception.getMessage())
                        .setTime(timestamp)
                        .build();
            }
            responseObserver.onNext(interactionResponse);
            responseObserver.onCompleted();
        }

        /**
         * Sends the request to the agents and retrieves the chosen response.
         *
         * @param interactionRequest - The instance of InteractionRequest passed by the
         *                           user/client to the agents.
         * @param responseObserver   - The instance, which is used to pass the instance of
         *                           InteractionResponse with the response from the agents.
         */
        @Override
        public void listResponses(InteractionRequest interactionRequest,
                                  StreamObserver<InteractionResponse> responseObserver) {
            try {
                String jsonString = JsonFormat.printer()
                        .preservingProtoFieldNames()
                        .print(interactionRequest);

                logger.info("Processing request:" + jsonString);
                checkNotNull(interactionRequest.getUserId(), "The InteractionRequest that have " +
                        "been sent doesn't have userID!");
                DialogAgentManager dialogAgentManager;
                try {
                    dialogAgentManager = DialogAgentManagerSingleton
                            .getDialogAgentManager(interactionRequest.getUserId());
                } catch (Exception exception) {
                    exception.printStackTrace();
                    dialogAgentManager = null;
                }
                checkNotNull(dialogAgentManager, "The initialization of the DialogAgentManager " +
                        "failed!");


                dialogAgentManager.listResponse(interactionRequest, responseObserver);
            } catch (Exception exception) {
                logger.warn("Error processing request :" + exception.getMessage() + " " + exception.getMessage());
                responseObserver.onError(exception);
            }
        }
    }
}