import * as React from "react"
import { Icon } from "semantic-ui-react"
import { ADConnection } from "../common/ADConnection"
import { MediaRecorderAPIService } from "../services/MediaRecorderService"
import { GoogleAPISpeechToTextService } from "../services/SpeechToTextService"
import css from "./ChatInput.module.css"
import { ControlledInput } from "./ValueInput"

export interface IChatInputProperties {
  onEnter: (text: string) => void,
  connection?: ADConnection
}

interface IChatInputState {
  value: string,
  isRecording: boolean
}

export class ChatInput
  extends React.Component<IChatInputProperties, IChatInputState> {

  private defaultProps = {
    hideRecordingButton: false

  }

  private mediaService: MediaRecorderAPIService;

  constructor(props: IChatInputProperties) {
    super(props)
    this.state = { value: "", isRecording: false }
    this.mediaService = new MediaRecorderAPIService(undefined, undefined, undefined, undefined, this.stopCallBack);
  }

  private onCommit = () => {
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

  public render(): React.ReactNode {
    return <div className={css.entry}>
      <ControlledInput
        value={this.state.value}
        onCommit={this.onCommit}
        onRevert={this.onRevert}
        onUpdate={this.onChange}
        icon={<Icon
          name="arrow up" inverted circular link
          className={css.enterButton}
          disabled={this.state.value.trim().length === 0}
          onClick={this.onCommit}
        />}
      />
      <div hidden={this.defaultProps.hideRecordingButton}><Icon
        name={this.state.isRecording ? 'stop circle' : "microphone"} inverted circular link red
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
