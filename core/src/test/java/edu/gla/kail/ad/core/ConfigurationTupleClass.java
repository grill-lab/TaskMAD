/*
package edu.gla.kail.ad.core;

import edu.gla.kail.ad.CoreConfiguration.Agent.ServiceProvider;;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
TODO: rewrite all the tests.

@RunWith(JUnit4.class)
public class ConfigurationTupleClass {
    @Test(expected = NullPointerException.class)
    public void testAgentTypeNullCase() {
        ConfigurationTuple configurationTuple = new ConfigurationTuple(null, null);
    }

    @Test
    public void testDialogflowTupleWithNullList() {
        ConfigurationTuple configurationTuple = new ConfigurationTuple(ServiceProvider
                .DIALOGFLOW, null);
    }

    @Test
    public void testGetTypeDialogflow() {
        ConfigurationTuple configurationTuple = new ConfigurationTuple(ServiceProvider
                .DIALOGFLOW, null);
        assertSame("ConfigurationTuple DIALOGFLOW type specified during the initialization is not" +
                        " the same as the type returned by get_agentType() method.",
                ServiceProvider.DIALOGFLOW, configurationTuple.get_agentType());

    }

    @Test
    public void testGetListDialogflow() {
        List list = new ArrayList();
        ConfigurationTuple configurationTuple = new ConfigurationTuple(ServiceProvider
                .DIALOGFLOW, list);
        assertTrue("ConfigurationTuple's agentSpecificData (empty ArrayList), for DIALOGFLOW " +
                        "type, is not equal to the one returned by get_agentSpecificData() method.",
                list.equals(configurationTuple.get_agentType()));
        list = new LinkedList();
        configurationTuple = new ConfigurationTuple(ServiceProvider.DIALOGFLOW, list);
        assertTrue("ConfigurationTuple's agentSpecificData (empty LinkedList), for DIALOGFLOW " +
                        "type, is not equal to the one returned by get_agentSpecificData() method.",
                list.equals(configurationTuple.get_agentType()));
        list.add("Testing text");
        configurationTuple = new ConfigurationTuple(ServiceProvider.DIALOGFLOW, list);
        assertTrue("ConfigurationTuple's agentSpecificData (LinkedList with one String entry), " +
                        "for DIALOGFLOW type, is not equal to the one returned by " +
                        "get_agentSpecificData() method.",
                list.equals(configurationTuple.get_agentType()));
    }

    @Test
    public void testEqualsMethod() {
        List list = new ArrayList();
        list.add("Testing text");
        ConfigurationTuple configurationTuple1 = new ConfigurationTuple(ServiceProvider
                .DIALOGFLOW, list);
        ConfigurationTuple configurationTuple2 = new ConfigurationTuple(ServiceProvider
                .DIALOGFLOW, list);
        assertTrue("The ConfigurationTuple's equals method doesn't work correctly.",
                configurationTuple1.equals(configurationTuple2));
        assertTrue("The ConfigurationTuple's equals method doesn't work correctly.",
                configurationTuple2.equals(configurationTuple1));
    }
}
*/
