package edu.gla.kail.ad.offlineExperiment;


import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import edu.gla.kail.ad.service.LogManagerSingleton;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * Servlet for handling Offline Experiment requests.
 */
@WebServlet("/offline-mt-ranking-servlet")
public class RatingServlet extends HttpServlet {

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws
            IOException {
        String userId = request.getParameter("userId");
        switch (request.getHeader("operation")) {
            case "validateUser":
                response.getWriter().write(verifyUser(userId).toString());
                break;
            case "loadTasks":
                Integer maxTasksAssigned = Integer.valueOf(request.getParameter
                        ("maxTasksAssigned"));
                TaskLoader taskLoader = new
                        TaskLoader();
                response.getWriter().write(taskLoader.loadTasks(LogManagerSingleton
                                .returnDatabase(),
                        userId, maxTasksAssigned, request.getParameter
                                ("experimentId")));
                break;
            case "rateTask":
                Integer ratingScore = Integer.valueOf(request.getParameter("ratingScore"));
                String taskId = request.getParameter("taskId");
                Long startTime_seconds = Long.valueOf(request.getParameter
                        ("startTime_seconds"));
                Long endTime_seconds = Long.valueOf(request.getParameter("endTime_seconds"));
                TaskRater taskRater = new
                        TaskRater();
                taskRater.rateTask(LogManagerSingleton.returnDatabase(), userId, ratingScore,
                        taskId,
                        startTime_seconds, endTime_seconds);
                break;
            default:
                response.getWriter().write("false");
        }
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        doPost(request, response);
    }

    /**
     * Verify if the user ID is stored in Firestore database.
     */
    private Boolean verifyUser(String userId) throws IOException {
        if (userId == null || userId.equals("")) {
            return false;
        }
        DocumentReference docRef = LogManagerSingleton.returnDatabase().collection
                ("clientWebSimulator")
                .document("agent-dialogue-experiments").collection("users")
                .document(userId);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        try {
            if (future.get().exists()) {
                return true;
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return false;
    }
}
