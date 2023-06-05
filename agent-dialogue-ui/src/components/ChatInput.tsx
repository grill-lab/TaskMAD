import * as React from "react"
import { InteractionType } from "../generated/client_pb"
import { Icon } from "semantic-ui-react"
import { ADConnection } from "../common/ADConnection"
import { MediaRecorderAPIService } from "../services/MediaRecorderService"
import { GoogleAPISpeechToTextService } from "../services/SpeechToTextService"
import css from "./ChatInput.module.css"
import { ControlledInput } from "./ValueInput"
import { IDialogue } from "./DialogueModel"

export interface IChatInputProperties {
  onEnter: (text: string) => void,
  connection?: ADConnection
  isAudioRecordingEnabled?: boolean
  dialogue: IDialogue
  us: string
}

interface IChatInputState {
  value: string,
  isRecording: boolean
}

export class ChatInput
  extends React.Component<IChatInputProperties, IChatInputState> {

  public static defaultProps = {
    isAudioRecordingEnabled: false,
  }

  private mediaService: MediaRecorderAPIService;

  constructor(props: IChatInputProperties) {
    super(props)
    this.state = { value: "", isRecording: false }
    this.mediaService = new MediaRecorderAPIService(undefined, undefined, undefined, undefined, this.stopCallBack);
  }

  private onCommit = () => {
    // this is called if the user hits Enter/Return with the input field focused
    if(!this.canSendMessage(true)) {
        console.log("Blocking message sending!")
        return
    }
    const value = this.state.value.trim()
    if (value.length !== 0) {
      this.props.onEnter(value)
    }
    this.onRevert()
  }

  private onRevert = () => {
    this.setState({ value: "" })
  }

  private onChange = (text: string) => {
    this.setState({ value: text })
  }

  private stopCallBack = (b: Blob) => {
    let reader = new FileReader();

    reader.readAsDataURL(b);
    reader.onloadend = async () => {
      var googleService: GoogleAPISpeechToTextService = new GoogleAPISpeechToTextService();
      var response: string = await googleService.base64StringToText(reader.result!.toString().replace('data:audio/ogg; codecs=opus;base64,', ''), this.props.connection!)

      if (response.trim() !== "") {
        this.setState({
          value: response
        });
      }

    }
  }

  public canSendMessage(allow_empty: boolean): boolean {
    // used to selectively enable the "send message" button. this should only be enabled
    // at certain times:
    //  - the wizard has sent us a message in the current turn (the last message received
    //      should be part of the current step, not a previous one)
    //  - the user has typed at least one character into the textbox
    let buttonEnabled = false;

    // check to see if the most recent TEXT message was sent by us or not
    if(this.props.dialogue.messages.length > 0) {
        const messages = this.props.dialogue.messages
        let messageIndex = messages.length - 1
        while(messageIndex >= 0) {
            const message = messages[messageIndex]

            // ignore any non-TEXT messages
            if(message.messageType !== InteractionType.TEXT) {
                messageIndex -= 1
                continue
            }

            // check the message sender and break out
            if(message.userID !== this.props.us) {
                // message wasn't sent by us, so we are able to reply
                buttonEnabled = true
            }
            break
        }
    }

    if(allow_empty) {
        // allow_empty is used so this method can act as a toggle
        // for enabling/disabling both the send button and the HTML
        // <input> element. The button stays disabled until text has
        // been typed, but the input field must become enabled when a 
        // message is able to be sent otherwise you can't type anything!
        return buttonEnabled
    }

    // also need to enable the button only if some text is typed
    return buttonEnabled && this.state.value.trim().length > 0
  }

  public render(): React.ReactNode {
    return <div className={css.entry}>
      <ControlledInput
        value={this.state.value}
        onCommit={this.onCommit}
        onRevert={this.onRevert}
        onUpdate={this.onChange}
        disabled={!this.canSendMessage(true)}
        icon={<Icon
          name="arrow up" inverted circular link
          className={css.enterButton}
          disabled={!this.canSendMessage(false)}
          onClick={this.onCommit}
        />}
      />
      <div hidden={!this.props.isAudioRecordingEnabled}><Icon
        name={this.state.isRecording ? 'stop circle' : "microphone"} inverted circular link
        color={this.state.isRecording ? 'red' : 'green'}
        className={css.recordingIcon}
        onClick={async () => {
          if (!this.state.isRecording) {
            this.setState({
              isRecording: !this.state.isRecording
            });
            await this.mediaService.startMedia();
          } else {
            this.setState({
              isRecording: !this.state.isRecording
            });
            this.mediaService.stopMedia();

          }

        }
        }
      /></div>
    </div>
  }
}
