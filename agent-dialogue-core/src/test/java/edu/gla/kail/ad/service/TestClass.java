package edu.gla.kail.ad.service;

import com.google.protobuf.Timestamp;
import com.google.protobuf.util.JsonFormat;
import edu.gla.kail.ad.Client;
import edu.gla.kail.ad.Client.InputInteraction;
import edu.gla.kail.ad.Client.InteractionRequest;
import edu.gla.kail.ad.Client.InteractionResponse;
import edu.gla.kail.ad.Client.InteractionType;

import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;

/**
 * This class is created for testing purposes. Currently not working well.
 */
public class TestClass {


    public static void main(String[] args) throws Exception {
        URL url = new URL("https://localhost:8080/test");
        HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
        urlc.setDoInput(true);
        urlc.setDoOutput(true);
        urlc.setRequestMethod("POST");
        urlc.setRequestProperty("Accept", "application/x-protobuf");
        urlc.setRequestProperty("Content-Type", "application/x-protobuf");

        InteractionRequest interactionRequest = InteractionRequest.newBuilder()
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
        System.out.println(JsonFormat.printer().print(interactionRequest));
        interactionRequest.writeTo(urlc.getOutputStream());
        InteractionResponse interactionResponse = InteractionResponse.newBuilder().mergeFrom(urlc
                .getInputStream()).build();
        System.out.println(interactionResponse.toString());
    }
}


// testing the server:
/* curl -H "Content-Type: text/plain" -X POST -d '{ "time": "2018-06-19T10:47:05.932Z",
"clientId": "Random Client ID", "interaction": { "text": "Sample text", "type": "TEXT",
"deviceType": "Iphone whatever", "languageCode": "en-US" } }'
http://localhost:8080/rest_api/agent_interaction
 * */