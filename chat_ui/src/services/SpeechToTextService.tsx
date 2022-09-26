import { ADConnection } from "../common/ADConnection";

interface ISpeechToTextService {
    base64StringToText: (base64String: string, connection: ADConnection) => Promise<string>
}

export class GoogleAPISpeechToTextService implements ISpeechToTextService {
    public async base64StringToText(base64String: string, connection: ADConnection): Promise<string> {

        let speechToText: string = await connection.agentSpeechToTextInteractionApi(base64String);

        return speechToText;
    }

}