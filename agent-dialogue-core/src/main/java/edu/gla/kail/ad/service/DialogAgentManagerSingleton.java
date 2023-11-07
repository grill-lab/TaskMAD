package edu.gla.kail.ad.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import edu.gla.kail.ad.CoreConfiguration.AgentConfig;
import edu.gla.kail.ad.core.DialogAgentManager;
import edu.gla.kail.ad.core.PropertiesSingleton;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class used to hold the instances of DialogAgentManager for each session.
 */
final class DialogAgentManagerSingleton {

    private static final Logger logger = LoggerFactory.getLogger(DialogAgentManagerSingleton.class);

    private static DialogAgentManagerSingleton _instance;
    // The maximum number of sessions that can be active at the same time.
    private static int _MAX_NUMBER_OF_SIMULTANEOUS_CONVERSATIONS = PropertiesSingleton
            .getCoreConfig().getMaxNumberOfSimultaneousConversations();
    // The time, after which the session becomes inactive (times out) if there are no requests
    // coming in from the user.
    private static int _SESSION_TIMEOUT_IN_MINUTES = PropertiesSingleton.getCoreConfig()
            .getSessionTimeoutMinutes();

    private static ScheduledExecutorService cacheSweeper = Executors.newScheduledThreadPool(1);
    private static ScheduledFuture<?> future = null;

    // The cache mapping userID and the DialogAgentManager instances assigned to each user.
    private static LoadingCache<String, DialogAgentManager> _initializedManagers = CacheBuilder
            .newBuilder()
            .maximumSize(_MAX_NUMBER_OF_SIMULTANEOUS_CONVERSATIONS)
            .expireAfterAccess(_SESSION_TIMEOUT_IN_MINUTES, TimeUnit.MINUTES)
            .removalListener(new RemovalListener<String, DialogAgentManager>() {
                public void onRemoval(RemovalNotification<String, DialogAgentManager> removal) {
                    removal.getValue().endSession();
		    logger.info("CACHE DROP: " + removal.getKey() + " removed due to " + removal.getCause());
                }
            })
            .build(
                    new CacheLoader<String, DialogAgentManager>() {
                        public DialogAgentManager load(String key) throws IOException {
                            logger.info("CACHE ADD: " + key);
                            // Execute if the active session for the user doesn't exist.
                            DialogAgentManager dialogAgentManager = new DialogAgentManager();
                            dialogAgentManager.setUpAgents((List<AgentConfig>) PropertiesSingleton
                                    .getCoreConfig()
                                    .getAgentsList());
                            return dialogAgentManager;
                        }
                    });


    /**
     * Get the DialogAgentManager instance for a particular user. If the user has active session,
     * the DialogAgentManager for that session will be returned. Otherwise, a new session will be
     * created, provided the limit of maximum number of sessions hasn't been reached.
     *
     * @param userId - The identification String userID, which is sent by each user
     *         with every request.
     * @return DialogAgentManager - The instance of DialogAgentManager used for particular session.
     * @throws ExecutionException - Thrown when setting up the agents is unsuccessful or max
     *         number of ongoing conversations has been reached.
     */
    static synchronized DialogAgentManager getDialogAgentManager(String userId) throws Exception {
        if (_instance == null) {
            _instance = new DialogAgentManagerSingleton();

            Runnable task = new Runnable() {
                public void run() {
                    logger.info("Running cleanUp on the cache, current size is " + _instance._initializedManagers.size());
                    _instance._initializedManagers.cleanUp();
                    logger.info("After cache cleanUp current size is " + _instance._initializedManagers.size());
                }
            };
            logger.info("Starting cache sweeper");
            future = cacheSweeper.scheduleAtFixedRate(task, 0, 1, TimeUnit.MINUTES);
        }
        if (_initializedManagers.size() == _MAX_NUMBER_OF_SIMULTANEOUS_CONVERSATIONS) {
            throw new Exception("The maximum number of conversations have been reached - wait " +
                    "some time or quit coversations on other user accounts.");
        }
        return _initializedManagers.get(userId);
    }

    /**
     * Delete the instance of DialogAgentManager for the session corresponding to passed userID.
     *
     * @param userId - The identification String userID, which is sent by each user
     *         with every request.
     */
    static synchronized boolean deleteDialogAgentManager(String userId) {
        logger.info("CACHE DROP: " + userId);
        _initializedManagers.invalidate(userId);
        return true;
    }
}
