package edu.gla.kail.ad.replayer;

import com.google.protobuf.Timestamp;
import edu.gla.kail.ad.Client;
import edu.gla.kail.ad.Client.InteractionRequest;
import edu.gla.kail.ad.Client.InteractionResponse;
import edu.gla.kail.ad.core.Log.LogEntry;
import edu.gla.kail.ad.core.Log.Turn;
import edu.gla.kail.ad.service.AgentDialogueGrpc;
import edu.gla.kail.ad.service.AgentDialogueGrpc.AgentDialogueBlockingStub;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Responsible for replaying the conversation from the LogEntry file.
 * °A lot of code is repeated in AgentDialogueClientService in web-simulator client.
 * TODO(Adam): Change log replayer - reading one file with multiple conversations = multiple
 * LogEntries.
 * TODO: Break it down into smaller classes.
 */
public class LogReplayer {
    private final ManagedChannel _channel;
    private final String _AVAILABLE_COMMANDS = "\nAvailable commands:\n" +
            "\nquit - Exit the application and stop all processes." +
            "\nnew - Add a path to the log file to be processed and process it." +
            "\nwait - Wait for the application to finish processing all the requests and then " +
            "quit." +
            "\nnumber - Get the number of logReplayer threads currently running." +
            "\nhelp - Get the available commands list.";
    // RPC will wait for the server to respond; return response or raise an exception.
    private final AgentDialogueBlockingStub _blockingStub;
    // True if the user wants to wait for all the threads to finish and then quit.
    private boolean _quit = false;
    // Directory to the folder with logs.
    private String _LOG_STORAGE_DIRECTORY;
    // Maximum number of conversations (LogEntry replays) happening at any given time.
    private int _MAXIMUM_NUMBER_OF_ONGOING_CONVERSATIONS = 5;
    // The string identifying the client.
    private String _userId;
    // Queue holding conversations to be assigned to different threads.
    private ConcurrentLinkedQueue<File> _logEntryFilePathQueue = new ConcurrentLinkedQueue<>();
    // The number of threads currently processing LogEntry file and getting responses from agents.
    private AtomicInteger _numberOfThreadsRunning = new AtomicInteger(0);


    public LogReplayer(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port).usePlaintext());
    }

    public LogReplayer(ManagedChannelBuilder<?> channelBuilder) {
        _channel = channelBuilder.build();
        _blockingStub = AgentDialogueGrpc.newBlockingStub(_channel);
        _userId = generateUserId();

        // Hardcoded directory path.
        File directory = new File(Paths
                .get(LogReplayer
                        .class
                        .getProtectionDomain()
                        .getCodeSource()
                        .getLocation()
                        .getPath())
                .getParent()
                .getParent()
                .toString() + "/Logs/Replayer");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        _LOG_STORAGE_DIRECTORY = directory.toString();
    }

    /**
     * Created for testing purposes.
     */
    public static void main(String[] args) {
        LogReplayer client = new LogReplayer("localhost", 8070);

        // Thread running as 'frontend' - collect the input from user.
        Runnable userInterfaceRunnable = () -> {
            File directory;
            Scanner scanner = new Scanner(System.in);
            System.out.println("Hi, I'm the log replayer. How can I help you?\n" + client
                    ._AVAILABLE_COMMANDS);
            while (true) {
                String command = scanner.nextLine();
                switch (command) {
                    case "help":
                        System.out.println(client._AVAILABLE_COMMANDS);
                        break;
                    case "quit":
                        System.out.println("Bye bye!");
                        System.exit(0);
                    case "new":
                        System.out.println("Type the path to the logEntry file: ");
                        String providedLogEntryDirectory = scanner.nextLine();
                        directory = new File(providedLogEntryDirectory);
                        if (!directory.exists()) {
                            System.out.println("The provided path to the log file is invalid:\n" +
                                    directory.toString() + "\nTry again!\n");
                        } else {
                            client._logEntryFilePathQueue.add(directory);
                        }
                        break;
                    case "wait":
                        client._quit = true;
                        System.out.println("The application will quit once all the requests are " +
                                "processed. In the meantime you can still interact with the " +
                                "applciation.\nCurrent number of running threads: " + client
                                ._numberOfThreadsRunning.get());
                        break;
                    case "number":
                        System.out.println("The number of logReplayer threads currently running: " +
                                "" + client._numberOfThreadsRunning.get());
                        break;
                    default:
                        System.out.println("Unrecognised command, try again!\n" + client
                                ._AVAILABLE_COMMANDS);
                }
                System.out.println("Enter a command or type 'help' to get the list of commands: ");
            }
        };

        // Thread running as 'backend' - takes requests from the queue at given rate.
        Runnable queueCheckerRunnable = () -> {
            while (true) {
                if (client._quit && client._numberOfThreadsRunning.get() == 0) {
                    System.out.println("All the threads have finished running!\nBye bye!");
                    System.exit(0);
                }
                try {
                    // Sleep call makes the queueCheckerRunnable run requests at given rate (time
                    // period).
                    TimeUnit.SECONDS.sleep(1);
                    if (!client._logEntryFilePathQueue.isEmpty() && client
                            ._numberOfThreadsRunning.get() <= client
                            ._MAXIMUM_NUMBER_OF_ONGOING_CONVERSATIONS) {
                        File logEntryFilePath = client._logEntryFilePathQueue.poll();
                        client.startReplayerThread(logEntryFilePath, client);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    System.exit(0);
                }
            }
        };
        new Thread(queueCheckerRunnable).start();
        new Thread(userInterfaceRunnable).start();
    }

    /**
     * Create new threads for running requests.
     * Update the _numberOfThreadsRunning.
     *
     * @param directoryFile - the File with the LogEntry we want to replay.
     * @param client - the Instance of LogReplayer which we are currently using.
     */
    private void startReplayerThread(File directoryFile, LogReplayer client) {
        Runnable singularDialogManagerThread = () -> {
            client._numberOfThreadsRunning.incrementAndGet();
            try {
                InputStream inputStream = new FileInputStream(directoryFile);
                List<InteractionResponse> interactionResponses = client.replayConversation
                        (inputStream);
                System.out.println("The following turns were successfully stored in the Log " +
                        "directory:\n\n" + interactionResponses.toString());
            } catch (Exception exception) {
                try {
                    OutputStream outputStream = new FileOutputStream(_LOG_STORAGE_DIRECTORY +
                            "/ERROR_" + Instant.now().toString() + ".log");
                    outputStream.write(("Something went wrong for the file: " + directoryFile
                            .toString() + ". Error message:\n" + exception.getMessage() + "\n" +
                            exception.getStackTrace()).getBytes(Charset.forName("UTF-8")));
                    outputStream.close();
                } catch (Exception internalException) {
                    // TODO(Adam): Rethink this part.
                    // TODO(Jeff): What to do then?
                    System.out.println("Something went wrong for the file: " + directoryFile
                            .toString() + ". Error message:\n" + exception.getMessage() + "\n" +
                            exception.getStackTrace());
                    System.exit(0);
                }
            } finally {
                client._numberOfThreadsRunning.decrementAndGet();
            }
        };
        new Thread(singularDialogManagerThread).start();
    }


    /**
     * Return random, unique Id.
     * TODO(Jeff): What method should we implement?
     *
     * @return random clientId
     */
    private String generateUserId() {
        return "LogReplayer_" + UUID.randomUUID().toString();
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
     * Get one response from the agents
     *
     * @param interactionRequest - The request sent to the Agent Dialog Manager.
     * @return interactionResponse - The response from an Agent chosen by DialogAgentManager.
     * @throws Exception - Throw when certain time of waiting for the repsonse passes.
     */
    private InteractionResponse getInteractionResponse(InteractionRequest interactionRequest)
            throws Exception {
        InteractionResponse interactionResponse;
        try {
            interactionResponse = _blockingStub.getResponseFromAgents(interactionRequest);
            return interactionResponse;
        } catch (StatusRuntimeException e) {
            e.printStackTrace();
            throw new Exception("Error occurred: " + e.getStatus());
        }
    }

    /**
     * Replay the conversation with agents.
     * Store all the responses in Log files.
     * Return the the list of responses.
     *
     * @param inputStream - InputStream created from the log file.
     * @return OutputStream - The outputStream of the Logfile created by replaying conversation.
     * @throws Exception - Throw when the provided InputStream is invalid or
     *         getInteractionResponse throws error.
     */
    private List<InteractionResponse> replayConversation(InputStream inputStream) throws
            Exception {
        LogEntry logEntry;
        List<InteractionResponse> listOfInteractionResponses = new ArrayList<>();
        try {
            logEntry = LogEntry.parseFrom(inputStream);
        } catch (IOException iOException) {
            throw new Exception("Provided InputStream is not valid! Error message: " +
                    iOException.getMessage());
        }

        for (Turn turn : logEntry.getTurnList()) {
            InteractionRequest interactionRequest = InteractionRequest.newBuilder()
                    .setTime(Timestamp.newBuilder()
                            .setSeconds(Instant.now().getEpochSecond())
                            .setNanos(Instant.now().getNano())
                            .build())
                    .setUserId(_userId)
                    .setClientId(Client.ClientId.LOG_REPLAYER)
                    .setInteraction(turn.getRequestLog().getInteraction()).build();
            InteractionResponse interactionResponse = getInteractionResponse(interactionRequest);
            listOfInteractionResponses.add(interactionResponse);
            OutputStream outputStream = new FileOutputStream(_LOG_STORAGE_DIRECTORY + "/" +
                    logEntry.getSessionId() + "_" + Instant.now().toString() + ".log");
            interactionRequest.writeTo(outputStream);
            outputStream.close();
        }
        return listOfInteractionResponses;
    }
}
