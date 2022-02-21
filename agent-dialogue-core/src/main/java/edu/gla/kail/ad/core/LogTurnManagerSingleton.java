package edu.gla.kail.ad.core;

import edu.gla.kail.ad.core.Log.Turn;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.ZonedDateTime;

/**
 * Manage turns and store them in the output stream.
 * TODO(Adam): Implement the use of LogStash, or maybe log4j, or make this thread safe.
 */
public final class LogTurnManagerSingleton {
    private static LogTurnManagerSingleton _instance;
    private static OutputStream _outputStream; // The stream used for writing all the log entries
    // to.
    private static String _logDailyTurnsPath; // A path for storing all folders with LogTurns for
    // each day.
    private static String _currentDailyTurnPath; // A path which the OutputStream is being
    // written to.

    /**
     * Get instance of this class.
     *
     * @return LogTurnManagerSingleton - An instance of the class itself.
     */
    public static synchronized LogTurnManagerSingleton getLogTurnManagerSingleton() throws IOException {
        if (_instance == null) {
            _instance = new LogTurnManagerSingleton();
            // Directory to the folder with logs.
            _logDailyTurnsPath = PropertiesSingleton.getCoreConfig().getLogStoragePath() +
                    "/DailyTurns/" + ZonedDateTime.now().toLocalDate().toString() + "/";
            directoryExistsOrCreate(_logDailyTurnsPath);
            _currentDailyTurnPath = _logDailyTurnsPath + ZonedDateTime.now().toLocalDateTime()
                    .toString();
            _outputStream = new FileOutputStream(_currentDailyTurnPath);

        }
        return _instance;
    }

    /**
     * Validate whether the directory exists and if not, then create it.
     */
    private static void directoryExistsOrCreate(String path) throws IOException {
        File directory = new File(path);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        if (!directory.exists()) {
            throw new IOException("Unable to create directory." + path);
        }
    }

    /**
     * Add the message (Turn) to the Output buffer.
     *
     * @param turn - The instance of Turn from proto buffer to be saved.
     * @throws IOException - Thrown when... TODO
     */
    public synchronized void addTurn(Turn turn) throws IOException {
        // TODO(Adam): Handle the exception.
        turn.writeDelimitedTo(_outputStream);
        _outputStream.flush();
    }

    /**
     * Close the stream = save the file; set the instance to null.
     *
     * @throws IOException - Thrown when... TODO
     */
    public void saveAndExit() throws IOException {
        // TODO(Adam): Handle the exception.
        // TODO(Adam): Code below is buggy - it can create issues. Resolve it!
        _outputStream.close();
        LogEntryManager.segregateFiles(_currentDailyTurnPath, _logDailyTurnsPath);
        _instance = null;
        _instance = LogTurnManagerSingleton.getLogTurnManagerSingleton();
    }
}
