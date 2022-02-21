/*
package edu.gla.kail.ad.core;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.Tuple;
import com.google.cloud.dialogflow.v2beta1.SessionsClient;
import com.google.cloud.dialogflow.v2beta1.SessionsSettings;
import edu.gla.kail.ad.Client.InputInteraction;
import edu.gla.kail.ad.Client.InteractionType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.UUID;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

// TODO(Adam): Restructure the entire class!
@RunWith(JUnit4.class)
public class DialogflowAgentTest {
    URL _jsonKeyURL;
    String _projectId;
    SessionsClient _sessionsClient;
    String _sessionId; // The sessionID is set in setUp method to a random ID, generated by the
    // same function as the DialogAgentManager uses.
    DialogflowAgent _dialogFlowAgent;

    */
/**
     * Set up _jsonKeyURL and projectID for the myquotemaster-13899 project.
     *//*

    @Before
    public void setUp() throws MalformedURLException {
        String currentClassPathFile = Paths
                .get(DialogflowAgentTest
                        .class
                        .getProtectionDomain()
                        .getCodeSource()
                        .getLocation()
                        .getPath())
                .getParent()
                .getParent()
                .toString();

        URL url = new URL("file:///Users/Adam/Documents/Internship/GitHub/agent-dialogue/agent-dialogue-core/src/main/resources/myquotemaster-13899-04ed41718e57.json");
        _projectId = "myquotemaster-13899";
        _sessionId = (new java.sql.Timestamp(System.currentTimeMillis())).toString() + UUID
                .randomUUID().toString();
        try {
            CredentialsProvider credentialsProvider = FixedCredentialsProvider.create(
                    (ServiceAccountCredentials.fromStream(new FileInputStream
                            (new Scanner(_jsonKeyURL
                                    .openStream()).useDelimiter("\\Z").next()))));
            SessionsSettings sessionsSettings = SessionsSettings.newBuilder().setCredentialsProvider
                    (credentialsProvider).build();
            _sessionsClient = SessionsClient.create(sessionsSettings);
            Tuple<String, URL> setUpAgentTuple = Tuple.of(_projectId, _jsonKeyURL);
            _dialogFlowAgent = new DialogflowAgent(_sessionId, setUpAgentTuple);
        } catch (Exception exception) {
            fail("SessionClient or DialogflowAgent initialization failed");
        }
    }

    @After
    public void cleanUp() {
        try {
            _sessionsClient.shutdown();
        } catch (Exception e) {
        }
    }

    */
/**
     * Test setting up the agent with valid parameters.
     *//*

    @Test
    public void testSetUpAgent() {
        Tuple<String, URL> setUpAgentTuple = Tuple.of(_projectId, _jsonKeyURL);
        try {
            DialogflowAgent dialogflowAgent = new DialogflowAgent(_sessionId, setUpAgentTuple);
        } catch (IOException iOException) {
            fail("Setting up the agent with projectID: " + _projectId + " and " +
                    "jsonKeyFileLocation: " + _jsonKeyURL + " was unsuccessful!");
        }
    }

    */
/**
     * Test setting up the agent with invalid parameters.
     *//*

    @Test(expected = NullPointerException.class)
    public void testSetUpAgentNullProjectID() {
        Tuple<String, URL> setUpAgentTuple = Tuple.of(null, _jsonKeyURL);
        try {
            DialogflowAgent dialogflowAgent = new DialogflowAgent(_sessionId, setUpAgentTuple);
        } catch (IOException iOException) {
            return;
        }
        fail("Setting up the agent with projectID: " + _projectId + " and " +
                "jsonKeyFiletestSetUpAgentEmptySessionIDLocation: " + _jsonKeyURL + " " +
                "was unsuccessful!");
    }

    */
/**
     * Test setting up the agent with invalid parameters.
     *//*

    @Test(expected = IllegalArgumentException.class)
    public void testSetUpAgentEmptyProjectID() {
        Tuple<String, URL> setUpAgentTuple = Tuple.of("", _jsonKeyURL);
        try {
            DialogflowAgent dialogflowAgent = new DialogflowAgent(_sessionId, setUpAgentTuple);
        } catch (IOException iOException) {
            return;
        }
        fail("Setting up the agent with projectID: " + _projectId + " and " +
                "jsonKeyFileLocation: " + _jsonKeyURL + " was unsuccessful!");
    }

    */
/**
     * Test setting up the agent with invalid parameters.
     *//*

    @Test(expected = NullPointerException.class)
    public void testSetUpAgentNullJsonFileLocation() {
        Tuple<String, URL> setUpAgentTuple = Tuple.of(_projectId, null);
        try {
            DialogflowAgent dialogflowAgent = new DialogflowAgent(_sessionId, setUpAgentTuple);
        } catch (IOException iOException) {
            return;
        }
        fail("Setting up the agent with projectID: " + _projectId + " and " +
                "jsonKeyFileLocation: " + _jsonKeyURL + " was unsuccessful!");
    }

    */
/**
     * Test setting up the agent with invalid parameters.
     *//*

    @Test
    public void testSetUpAgentInvalidJsonFileLocation() throws MalformedURLException {
        Tuple<String, URL> setUpAgentTuple = Tuple.of(_projectId, new URL("dsad"));
        try {
            DialogflowAgent dialogflowAgent = new DialogflowAgent(_sessionId, setUpAgentTuple);
        } catch (Exception iOException) {
            return;
        }
        fail("Setting up the agent with projectID: " + _projectId + " and " +
                "jsonKeyFileLocation: " + _jsonKeyURL + " was unsuccessful!");
    }

    */
/**
     * Test the validation method for a text input with invalid input.
     *//*

    @Test(expected = IllegalArgumentException.class)
    public void testValidateInputInteraction() {
        InputInteraction inputInteraction = InputInteraction.newBuilder()
                .setType(InteractionType.TEXT)
                .setText("")
                .setDeviceType("iPhone whatever")
                .setLanguageCode("en-US")
                .build();
        _dialogFlowAgent.getResponseFromAgent(inputInteraction);
    }

    @Test
    public void testGetResponseFromAgent() {
        InputInteraction inputInteraction = InputInteraction.newBuilder()
                .setType(InteractionType.TEXT)
                .setText("Hi")
                .setDeviceType("iPhone whatever")
                .setLanguageCode("en-US")
                .build();
        assertTrue("The response from DialogflowAgent is invalid", _dialogFlowAgent
                .getResponseFromAgent(inputInteraction).isInitialized());
    }
}
*/
