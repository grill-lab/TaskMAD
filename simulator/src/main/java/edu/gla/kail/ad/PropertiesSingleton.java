package edu.gla.kail.ad;

import com.google.protobuf.util.JsonFormat;
import edu.gla.kail.ad.SimulatorConfiguration.SimulatorConfig;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Scanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PropertiesSingleton is a class which holds the data read from the configuration file.
 * The configuration may be reloaded, however some of the settings (such as server port) will not be
 * affected.
 */
public final class PropertiesSingleton {
    private static PropertiesSingleton _instance;
    private static SimulatorConfig _simulatorConfig; // Protocol Buffer data structure holding

    private static final Logger logger = LoggerFactory.getLogger( PropertiesSingleton.class);

    // all the data
    // read from the configuration file.

    public static synchronized SimulatorConfig getSimulatorConfig() throws IOException {
        if (_instance == null) {
            getPropertiesSingleton(null);
        }
        return _simulatorConfig;
    } // Get the SimulatorConfig instance with the data from config file.

    /**
     * Reload properties from a given configuration file.
     *
     * @param url - URL of the configuration file.
     * @throws IOException - Thrown, when there are problems with the URL or the file under
     *         the URL.
     */
    public static synchronized void reloadProperties(URL url) throws IOException {
        _instance = null;
        getPropertiesSingleton(url);
    }

    /**
     * Return the instance of this singleton.
     *
     * @param url - URL of the configuration file.
     * @throws IOException - Thrown, when there are problems with the URL or the file under
     *         the URL.
     */
    public static synchronized PropertiesSingleton getPropertiesSingleton(@Nullable URL url) throws
            IOException {
        if (_instance == null) {
            _instance = new PropertiesSingleton();
            if (url == null) {
                // Nasty but works...
                setProperties(new URL("file:///" + new File("s/").getAbsolutePath() +
                        "rc/main/resources/config.json"));
            } else {
                setProperties(url);
            }
        }
        return _instance;
    }


    /**
     * Create an instance of SimulatorConfig class, which holds the data of a JSON configuration
     * file stored under the specified URL.
     *
     * @param url - URL of the configuration file.
     * @throws IOException - Thrown, when there are problems with the URL or the file under
     *         the URL.
     */
    private static void setProperties(URL url) throws IOException {
        logger.info("Loading properties from URL: " + url.toString());
        SimulatorConfig.Builder simulatorConfigBuilder = SimulatorConfig.newBuilder();
        String jsonText = readPropertiesFromUrl(url);
        JsonFormat.parser().merge(jsonText, simulatorConfigBuilder);
        _simulatorConfig = simulatorConfigBuilder.build();
        logger.info("Loaded properties " + _simulatorConfig.toString());

    }

    /**
     * Return a String of a JSON file stored under specified URL.
     *
     * @param url - URL of the configuration file.
     * @return - String holding the data from the JSON file specified by the provided URL.
     * @throws IOException - Thrown, when there are problems with the URL or the file under
     *         the URL.
     */
    private static String readPropertiesFromUrl(URL url) throws IOException {
        return new Scanner(url.openStream()).useDelimiter("\\Z").next();
    }
}
