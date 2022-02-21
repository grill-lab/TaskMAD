/*
package edu.gla.kail.ad.core;

import com.google.cloud.Tuple;
import com.google.protobuf.Timestamp;
import edu.gla.kail.ad.Client;
import edu.gla.kail.ad.Client.InputInteraction;
import edu.gla.kail.ad.Client.InteractionRequest;
import edu.gla.kail.ad.Client.InteractionType;
import edu.gla.kail.ad.CoreConfiguration.Agent.ServiceProvider;;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.fail;


// TODO(Adam): Restructure the majority of the class!
@RunWith(JUnit4.class)
public class DialogAgentManagerTest {
    private static List<ConfigurationTuple> _configurationTuples;
    private DialogAgentManager _dialogAgentManager;
    private InteractionRequest _interactionRequest;

    @BeforeClass
    public static void setUpClass() {
        Path projectCoreDir = Paths
                .get(DialogAgentManagerTest
                        .class
                        .getProtectionDomain()
                        .getCodeSource()
                        .getLocation()
                        .getPath())
                .getParent()
                .getParent();
        String testTextFileDirectory = projectCoreDir +
                "/src/main/resources/TestTextFiles/ProjectIdAndJsonKeyFileLocations.txt";
        _configurationTuples = new ArrayList<>();
        Path path = Paths.get(testTextFileDirectory);
        List<String> lines = null;
        try {
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            fail("The file directiory with the file ProjectIdAndJsonKeyFileLocations is not " +
                    "valid!");
        }
        for (String line : lines) {
            String[] projectIdAndJsonKey = line.split(",");
            switch (projectIdAndJsonKey[0]) {
                case "Dialogflow":
                    List<Tuple<String, String>> dialogflowProjectIdAndJsonKeyFileList = new
                            ArrayList<>();
                    dialogflowProjectIdAndJsonKeyFileList.add(Tuple.of(projectIdAndJsonKey[1],
                            projectCoreDir + projectIdAndJsonKey[2]));
                    _configurationTuples.add(new ConfigurationTuple(ServiceProvider
                            .DIALOGFLOW, dialogflowProjectIdAndJsonKeyFileList));
                    break;
                case "DummyAgent":
                    _configurationTuples.add(new ConfigurationTuple(ServiceProvider
                            .DUMMYAGENT, null));
                    break;
                case "FailingExceptionDummyAgent":
                    _configurationTuples.add(new ConfigurationTuple(ServiceProvider
                            .FAILINGEXCEPTIONDUMMYAGENT, null));
                    break;
                case "FailingNullDummyAgent":
                    _configurationTuples.add(new ConfigurationTuple(ServiceProvider
                            .FAILINGNULLDUMMYAGENT, null));
                    break;
                case "FailingTimeDummyAgent":
                    _configurationTuples.add(new ConfigurationTuple(ServiceProvider
                            .FAILINGTIMEDUMMYAGENT, null));
                    break;
                default:
                    fail("The name of the agent is not correctly formatted or the agent type: " +
                            projectIdAndJsonKey[0] + " is not supported yet.");
            }
        }
    }

    @Before
    public void setUp() {
        _dialogAgentManager = new DialogAgentManager();
        try {
            _dialogAgentManager.setUpAgents(_configurationTuples);
        } catch (IOException e) {
            fail("Setting up agents was unsuccessful");
        }
        _interactionRequest = InteractionRequest.newBuilder()
                .setClientId(Client.ClientId.EXTERNAL_APPLICATION)
                .setTime(Timestamp.newBuilder()
                        .setSeconds(Instant.now()
                                .getEpochSecond())
                        .setNanos(Instant.now()
                                .getNano())
                        .build())
                .setInteraction(InputInteraction.newBuilder()
                        .setType(InteractionType.TEXT)
                        .setText("Sample text")
                        .setDeviceType("iPhone whatever")
                        .setLanguageCode("en-US")
                        .build())
                .build();
    }

    */
/**
     * Test if agents are being set up from the provided configurationTuple.
     *//*


    @Test
    public void testSetUpAgents() {
        _dialogAgentManager = new DialogAgentManager();
        try {
            _dialogAgentManager.setUpAgents(_configurationTuples);
        } catch (IOException e) {
            fail("Setting up agents was unsuccessful");
        }
    }

    @Test
    public void testGetResponsesFromAgents() {
        try {
            _dialogAgentManager.getResponse(_interactionRequest);
        } catch (Exception exception) {
            fail("Getting responses from Agents was unsuccessful.");
        }
    }

}

*/
