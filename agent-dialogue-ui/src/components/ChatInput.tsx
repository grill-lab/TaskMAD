import * as React from "react"
import {Icon} from "semantic-ui-react"
import css from "./ChatInput.module.css"
import {ControlledInput} from "./ValueInput"

export interface IChatInputProperties {
  onEnter: (text: string) => void
}

interface IChatInputState {
  value: string
}

export class ChatInput
    extends React.Component<IChatInputProperties, IChatInputState> {

  constructor(props: IChatInputProperties) {
    super(props)
    this.state = {value: ""}
  }

  private onCommit = () => {
    const value = this.state.value.trim()
    if (value.length !== 0) {
      this.props.onEnter(value)
    }
    this.onRevert()
  }

  private onRevert = () => {
    this.setState({value: ""})
  }

  private onChange = (text: string) => {
    this.setState({value: text})
  }

  public render(): React.ReactNode {
    return <div className={css.entry}>
      <ControlledInput
          value={this.state.value}
          fluid
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
    </div>
  }
}
