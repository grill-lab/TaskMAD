/*
package edu.gla.kail.ad.core;

import com.google.cloud.Tuple;
import com.google.protobuf.Timestamp;
import edu.gla.kail.ad.Client;
import edu.gla.kail.ad.Client.InputInteraction;
import edu.gla.kail.ad.Client.InteractionRequest;
import edu.gla.kail.ad.Client.InteractionType;
import edu.gla.kail.ad.CoreConfiguration.Agent;
import edu.gla.kail.ad.CoreConfiguration.Agent.ServiceProvider;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

*/
/**
 * Class for testing the functionality.
 *//*

public class DialogAgentManagerTestMain {
    private static List<Agent> _agents;
    private static Path _projectCoreDir = Paths
            .get(DialogAgentManagerTestMain
                    .class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .getPath())
            .getParent()
            .getParent();

    */
/**
     * Add ConfigurationTuples to the _agents. This is just for testing purposes
     *
     * @param fileDirectory - It specifies the directory of the file with data used to set
     *         up agents. Each line has one agent entry, which specified agent type
     *         parameters
     *         required by this agent separated with ",".
     * @throws Exception - It is thrown when the given type name of the agent is not
     *         correctly formatted or the agent type is not supported yet.
     *//*

    private static void readProjectIdAndKeyFileToHashMap(String fileDirectory) throws Exception {
        _agents = new ArrayList<>();
        Path path = Paths.get(fileDirectory);
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        for (String line : lines) {
            String[] projectIdAndJsonKey = line.split(",");
            switch (projectIdAndJsonKey[0]) {
                case "Dialogflow":
                    List<Tuple<String, String>> dialogflowProjectIdAndJsonKeyFileList = new
                            ArrayList<>();
                    dialogflowProjectIdAndJsonKeyFileList.add(Tuple.of(projectIdAndJsonKey[1],
                            _projectCoreDir.toString() + projectIdAndJsonKey[2]));
                    _agents.add(new <>(ServiceProvider.DIALOGFLOW,
                            dialogflowProjectIdAndJsonKeyFileList));
                    break;
                */
/*case "DummyAgent":
                    _agents.add(new ConfigurationTuple<>(ServiceProvider
                            .DUMMYAGENT, null));
                    break;
                case "FailingExceptionDummyAgent":
                    _agents.add(new ConfigurationTuple<>(ServiceProvider
                            .FAILINGEXCEPTIONDUMMYAGENT, null));
                    break;
                case "FailingNullDummyAgent":
                    _agents.add(new ConfigurationTuple<>(ServiceProvider
                            .FAILINGNULLDUMMYAGENT, null));
                    break;
                case "FailingTimeDummyAgent":
                    _agents.add(new ConfigurationTuple<>(ServiceProvider
                            .FAILINGTIMEDUMMYAGENT, null));
                    break;*//*

                default:
                    throw new IllegalArgumentException("The name of the agent is not correctly " +
                            "formatted or the agent type: " +
                            projectIdAndJsonKey[0] + " is not supported yet.");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        // Different parameters required by the agents and proto buffers, that would be passed by
        // the client.
        String languageCode = "en-US";
        String deviceType = "iPhone Google Assistant";
        String testTextFileDirectory = _projectCoreDir +
                "/src/main/resources/TestTextFiles/";
        String nameOfTestedFile = "SampleConversation.txt";
        String nameOfFileWithProjectIdAndKeysLocations = "ProjectIdAndJsonKeyFileLocations.txt";

        // Add the agents, we want to test, from text file.
        readProjectIdAndKeyFileToHashMap(testTextFileDirectory +
                nameOfFileWithProjectIdAndKeysLocations);


        DialogAgentManager dialogAgentManager = new DialogAgentManager();
        dialogAgentManager.setUpAgents(agent);
        Timestamp timestamp = Timestamp.newBuilder()
                .setSeconds(Instant.now()
                        .getEpochSecond())
                .setNanos(Instant.now()
                        .getNano())
                .build();
        // Get responses from all the agents on the text provided in a text file.
        Path path = Paths.get(testTextFileDirectory + nameOfTestedFile);
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        for (String line : lines) {
            System.out.println("TESTING CLASS OUTPUT: CURRENTLY HANDLING THE REQUEST FOR: " + line);
            InteractionRequest interactionRequest = InteractionRequest.newBuilder()
                    .setTime(timestamp)
                    .setClientId(Client.ClientId.EXTERNAL_APPLICATION)
                    .setInteraction(InputInteraction.newBuilder()
                            .setType(InteractionType.TEXT)
                            .setText(line)
                            .setDeviceType(deviceType)
                            .setLanguageCode(languageCode))
                    .build();
            dialogAgentManager.getResponse(interactionRequest);
            System.out.println("TESTING CLASS OUTPUT: FINISHED HANDLING THE REQUEST FOR: " + line
                    + "\n");
        }
    }
}
*/
