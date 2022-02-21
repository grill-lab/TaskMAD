import React from "react"
import {Form} from "semantic-ui-react"
import {Grid} from "semantic-ui-react"
import {ChatComponent} from "../components/ChatComponent"
import {IDialogue, sampleDialogue, US} from "../components/DialogueModel"
import {Message} from "../components/MessageModel"
import css from "../woz/WoZPanel.module.css"

interface IHomePanelState {
  dialogue: IDialogue
}

interface IHomePanelProperties {
  dialogue?: IDialogue
}

export class HomePanel extends React.Component<IHomePanelProperties, IHomePanelState> {

  constructor(props: IHomePanelProperties) {
    super(props)

    this.state = {
      dialogue: props.dialogue === undefined
                ? sampleDialogue() : props.dialogue,
    }
  }

  private onEnter = (text: string) => {
    this.setState((prev) => {
      const d = prev.dialogue
      d.messages.push(new Message({userID: US, text}))
      return {dialogue: d}
    })
  }

  public render(): React.ReactNode {

    const userType: string = "user"

    const handleChange = (): void => {}

    return <Grid className={css.mainGrid}>
      <Grid.Row>
        <Grid.Column width={13}>
          <ChatComponent
              dialogue={this.state.dialogue}
              us={US}
              them={[]}
              onEnter={this.onEnter}
          />
        </Grid.Column>

        <Grid.Column width={3}>
          <Form>
            <Form.Checkbox label="Rating is enabled" />
            <Form.Group inline>
              <label>Language</label>
              <Form.Radio
                  label="Wizard"
                  value="wizard"
                  checked={userType === "wizard"}
                  onChange={handleChange}
              />
              <Form.Radio
                  label="User"
                  value="user"
                  checked={userType === "user"}
                  onChange={handleChange}
              />
            </Form.Group>
          </Form>
        </Grid.Column>
      </Grid.Row>
    </Grid>
  }
}
