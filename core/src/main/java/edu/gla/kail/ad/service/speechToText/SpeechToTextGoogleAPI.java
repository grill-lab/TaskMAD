package edu.gla.kail.ad.service.speechToText;

import java.io.IOException;
import java.util.List;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognizeResponse;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.SpeechRecognitionResult;
import com.google.cloud.speech.v1.SpeechSettings;
import com.google.cloud.speech.v1.RecognitionConfig.AudioEncoding;
import com.google.protobuf.ByteString;

import org.apache.commons.codec.binary.Base64;

public class SpeechToTextGoogleAPI implements SpeechToTextInterface{

    private ServiceAccountCredentials googleAPIcredentials; 
    private AudioEncoding audioEncoding;
    private int sampleRateHertz;
    private String languageCode;

    public SpeechToTextGoogleAPI(ServiceAccountCredentials credentials){
        this.googleAPIcredentials = credentials;
        this.audioEncoding = AudioEncoding.WEBM_OPUS;
        this.sampleRateHertz = 48000;
        this.languageCode = "en-US";
    }


    @Override
    public String speechToText(String base64String) throws IOException {
        
        SpeechSettings speechSettings =
                SpeechSettings.newBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(this.googleAPIcredentials))
                    .build();


        String resultString = "";
        try (SpeechClient speechClient = SpeechClient.create(speechSettings)) {
        
            // Builds the sync recognize request
            RecognitionConfig config =
                RecognitionConfig.newBuilder()
                    .setEncoding(this.audioEncoding)
                    .setSampleRateHertz(this.sampleRateHertz)
                    .setLanguageCode(this.languageCode)
                    .build();
                
            RecognitionAudio audio = RecognitionAudio.newBuilder().setContent(ByteString.copyFrom(Base64.decodeBase64(base64String))).build();
        
            // Performs speech recognition on the audio file
            RecognizeResponse response = speechClient.recognize(config, audio);
            List<SpeechRecognitionResult> results = response.getResultsList();
        
            for (SpeechRecognitionResult result : results) {
              // There can be several alternative transcripts for a given chunk of speech. Just use the
              // first (most likely) one here.
              SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
              resultString += alternative.getTranscript();
            }
          }
          
        return resultString;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public int getSampleRateHertz() {
        return sampleRateHertz;
    }

    public void setSampleRateHertz(int sampleRateHertz) {
        this.sampleRateHertz = sampleRateHertz;
    }

    public AudioEncoding getAudioEncoding() {
        return audioEncoding;
    }

    public void setAudioEncoding(AudioEncoding audioEncoding) {
        this.audioEncoding = audioEncoding;
    }
    
}
