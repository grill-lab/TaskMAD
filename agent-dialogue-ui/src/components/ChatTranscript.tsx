import * as React from "react"
import { Embed } from "semantic-ui-react"
import { diffSecondsBetweenDates, isStringImagePath, isStringVideoPath, playTextToAudio } from "../common/util"
import { InteractionType } from "../generated/client_pb"
import css from "./ChatTranscript.module.css"
import { IDialogue } from "./DialogueModel"

export interface IChatTranscriptProperties {
  dialogue: IDialogue
  us: string
  them: string[]
}

export class ChatTranscript
  extends React.Component<IChatTranscriptProperties, {}> {

  private messageList?: HTMLDivElement

  private scrollToBottom() {
    if (this.messageList === undefined) { return }
    const scrollHeight = this.messageList.scrollHeight
    const height = this.messageList.clientHeight
    const maxScrollTop = scrollHeight - height
    this.messageList.scrollTop = maxScrollTop > 0 ? maxScrollTop : 0
  }

  // noinspection JSUnusedGlobalSymbols
  public componentDidUpdate = () => {
    this.scrollToBottom();

    // Play the last message
    var last_message = this.props.dialogue.messages[this.props.dialogue.messages.length - 1];
    if (last_message !== undefined && last_message?.userID !== this.props.us && last_message?.messageType === InteractionType.TEXT) {
      // Play the last message only if it has been sent less than 5 seconds ago. 

      if (last_message?.time.getTime() !== undefined && diffSecondsBetweenDates(last_message?.time, new Date()) <= 5) {
        if (last_message?.text !== undefined && !isStringImagePath(last_message?.text) && !isStringVideoPath(last_message?.text)) {
          playTextToAudio(last_message?.text);
        }
      }

    }


  }

  // noinspection JSUnusedGlobalSymbols
  public componentDidMount = () => {
    this.scrollToBottom()
  }

  public render(): React.ReactNode {

    const rows = this.props.dialogue.messages.map((message, index) => {
      const cellClass = message.userID === undefined
        ? css.systemCell
        : message.userID === this.props.us
          ? css.ourCell
          : css.theirCell
      const rowClass = message.userID === undefined
        ? css.systemRow
        : message.userID === this.props.us
          ? css.ourRow
          : css.theirRow
      const visibleUserID = message.userID !== undefined
        && message.userID !== this.props.us
        && this.props.them.find(
          (id) => (id === message.userID)) === undefined
        ? <span className={css.them}>{message.userID}: </span>
        : ""

      if (message.messageType === InteractionType.TEXT) {
        // We do not show the message if the message is a status message (useful only for the wizard)
        if (!isStringImagePath(message.text)) {
          // The text here could be a video. In that case we just display it. 
          // Otherwise we just proceed with showing all the other different cases
          if (isStringVideoPath(message.text)) {

            // Display video
            return <div className={css.row + " " + rowClass} key={index}>
              <div className={css.videoCell + " " + cellClass}>
                <Embed placeholder={message.text.split('<video_separator>')[0]} url={message.text.split('<video_separator>')[1]}
                  className={css.videoCellEmbed}
                  autoplay={true}
                  iframe={{
                    allowFullScreen: true
                  }}></Embed>
              </div>
            </div>

          } else {
            return <div className={css.row + " " + rowClass} key={index}>
              <div className={css.cell + " " + cellClass}>{visibleUserID}{message.text}</div>
            </div>
          }

        } else {
          return <div className={css.row + " " + rowClass} key={index}>
            <div className={css.imageCell + " " + cellClass}>
              <img src={message.text} className={css.imageCellSrc} alt={message.text} />
            </div>
          </div>
        }
      }

      return undefined;
    })

    // Filter out the undefined elements from the list
    const filtered_rows = rows.filter((el) => el !== undefined);

    return <div className={css.transcript}>
      <div
        className={css.scrollable}
        ref={(div) => { this.messageList = div || undefined }}>{filtered_rows}</div>
    </div>
  }
}

