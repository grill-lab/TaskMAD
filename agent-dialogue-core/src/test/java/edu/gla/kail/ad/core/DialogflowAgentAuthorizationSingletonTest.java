/*
package edu.gla.kail.ad.core;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.Tuple;
import com.google.cloud.dialogflow.v2beta1.SessionsClient;
import com.google.cloud.dialogflow.v2beta1.SessionsSettings;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

import static org.junit.Assert.fail;

@RunWith(JUnit4.class)
public class DialogflowAgentAuthorizationSingletonTest {
    private URL _jsonKeyURL;
    private String _projectId;

    */
/**
     * Set up _jsonKeyURL and projectID for the myquotemaster-13899 project.
     *//*

    @Before
    public void setUp() throws MalformedURLException {
        _jsonKeyURL = new URL
                ("file:///Users/Adam/Documents/Internship/GitHub/agent-dialogue/agent-dialogue" +
                        "-core/src/main/resources/myquotemaster-13899-04ed41718e57.json");
        _projectId = "myquotemaster-13899";
    }

    */
/**
     * Test myquotemaster-13899 DialogflowAgentAuthorizationSingleton initialization.
     *//*

    @Test
    public void testInitialization() {
        Tuple<String, URL> tupleOfProjectIdAndAuthenticationFile = Tuple.of(_projectId,
                _jsonKeyURL);
        try {
            Tuple<String, SessionsClient> tupleOfSessionIDAndSessionClient =
                    DialogflowAgentAuthorizationSingleton
                            .getProjectIdAndSessionsClient(tupleOfProjectIdAndAuthenticationFile);
        } catch (FileNotFoundException fileNotFoundException) {
            fail("The specified file directory doesn't exist or the file is missing: " +
                    _jsonKeyURL);
        } catch (IOException iOException) {
            fail("The creation of CredentialsProvider, SessionsSettings or SessionsClient " +
                    "for projectID: " + _projectId + " failed.");
        }
    }

    */
/**
     * Test myquotemaster-13899 Dialogflow's CredentialsProvider initialization.
     *//*

    @Test
    public void testDialogflowCredentialsProviderInitialization() {
        try {
            CredentialsProvider credentialsProvider = FixedCredentialsProvider.create(
                    (ServiceAccountCredentials.fromStream(new FileInputStream
                            (new Scanner(_jsonKeyURL
                                    .openStream()).useDelimiter("\\Z").next()))));
        } catch (IOException iOException) {
            fail("The creation of CredentialsProvider with given _jsonKeyURL: " +
                    _jsonKeyURL + " failed!");
        }
    }

    */
/**
     * Test myquotemaster-13899 Dialogflow's SessionsSettings initialization.
     *//*

    @Test
    public void testDialogflowSessionsSettingsInitialization() {
        try {
            CredentialsProvider credentialsProvider = FixedCredentialsProvider.create(
                    (ServiceAccountCredentials.fromStream(new FileInputStream
                            (new Scanner(_jsonKeyURL
                                    .openStream()).useDelimiter("\\Z").next()))));
            SessionsSettings sessionsSettings = SessionsSettings.newBuilder().setCredentialsProvider
                    (credentialsProvider).build();
        } catch (IOException iOException) {
            fail("The creation of SessionsSettings (or CredentialsProvider) with given " +
                    "_jsonKeyURL: " + _jsonKeyURL + " failed!");
        }
    }

    */
/**
     * Test myquotemaster-13899 Dialogflow's SessionsClient initialization.
     *//*

    @Test
    public void testDialogflowSessionsClientInitialization() {
        try {
            CredentialsProvider credentialsProvider = FixedCredentialsProvider.create(
                    (ServiceAccountCredentials.fromStream(new FileInputStream
                            (new Scanner(_jsonKeyURL
                                    .openStream()).useDelimiter("\\Z").next()))));
            SessionsSettings sessionsSettings = SessionsSettings.newBuilder().setCredentialsProvider
                    (credentialsProvider).build();
            SessionsClient sessionsClient = SessionsClient.create(sessionsSettings);
        } catch (IOException iOException) {
            fail("The creation of SessionsClient (or SessionsSettings or " +
                    "CredentialsProvider) with given _jsonKeyURL: " + _jsonKeyURL
                    + " failed!");
        }
    }

    */
/**
     * Test when a wrong _jsonKeyURL is provided.
     *//*

    @Test
    public void testHandlingNonexistentFileDirection() throws MalformedURLException {
        URL jsonKeyFileLocation = new URL("NonExisting file directory.");
        Tuple<String, URL> tupleOfProjectIdAndAuthenticationFile = Tuple.of(_projectId,
                jsonKeyFileLocation);
        try {
            Tuple<String, SessionsClient> tupleOfSessionIDAndSessionClient =
                    DialogflowAgentAuthorizationSingleton.getProjectIdAndSessionsClient
                            (tupleOfProjectIdAndAuthenticationFile);
        } catch (Exception e) {
            return;
        }
        fail("No exception was thrown!");
    }

    */
/**
     * Test when a null projectID is provided.
     *//*

    @Test(expected = NullPointerException.class)
    public void testNullProjectId() {
        String projectId = null;
        Tuple<String, URL> tupleOfProjectIdAndAuthenticationFile = Tuple.of(projectId,
                _jsonKeyURL);
        try {
            Tuple<String, SessionsClient> tupleOfSessionIDAndSessionClient =
                    DialogflowAgentAuthorizationSingleton.getProjectIdAndSessionsClient
                            (tupleOfProjectIdAndAuthenticationFile);
        } catch (IOException e) {
        }
    }

    */
/**
     * Test when an empty projectID is provided.
     *//*

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyProjectId() {
        String projectId = "";
        Tuple<String, URL> tupleOfProjectIdAndAuthenticationFile = Tuple.of(projectId,
                _jsonKeyURL);
        try {
            Tuple<String, SessionsClient> tupleOfSessionIDAndSessionClient =
                    DialogflowAgentAuthorizationSingleton.getProjectIdAndSessionsClient
                            (tupleOfProjectIdAndAuthenticationFile);
        } catch (IOException e) {
        }
    }

    */
/**
     * Test when a null _jsonKeyURL is provided.
     *//*

    @Test(expected = NullPointerException.class)
    public void testNullJsonKeyFileLocation() {
        URL jsonKeyFileLocation = null;
        Tuple<String, URL> tupleOfProjectIdAndAuthenticationFile = Tuple.of(_projectId,
                jsonKeyFileLocation);
        try {
            Tuple<String, SessionsClient> tupleOfSessionIDAndSessionClient =
                    DialogflowAgentAuthorizationSingleton.getProjectIdAndSessionsClient
                            (tupleOfProjectIdAndAuthenticationFile);
        } catch (IOException e) {
        }
    }

    */
/**
     * Test when an empty _jsonKeyURL is provided.
     *//*

    @Test
    public void testEmptyJsonKeyFileLocation() throws MalformedURLException {
        URL jsonKeyFileLocation = new URL("");
        Tuple<String, URL> tupleOfProjectIdAndAuthenticationFile = Tuple.of(_projectId,
                jsonKeyFileLocation);
        try {
            Tuple<String, SessionsClient> tupleOfSessionIDAndSessionClient =
                    DialogflowAgentAuthorizationSingleton.getProjectIdAndSessionsClient
                            (tupleOfProjectIdAndAuthenticationFile);
        } catch (Exception e) {
            return;
        }
        fail("No exception was thrown!");
    }

    */
/**
     * Test when an a null Tuple is provided.
     *//*

    @Test(expected = NullPointerException.class)
    public void testNullTuple() {
        Tuple<String, URL> tupleOfProjectIdAndAuthenticationFile = null;
        try {
            Tuple<String, SessionsClient> tupleOfSessionIDAndSessionClient =
                    DialogflowAgentAuthorizationSingleton.getProjectIdAndSessionsClient
                            (tupleOfProjectIdAndAuthenticationFile);
        } catch (IOException e) {
        }
    }
}
*/
