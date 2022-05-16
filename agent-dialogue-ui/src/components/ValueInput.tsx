/* tslint:disable:max-classes-per-file */
import * as React from "react"
import { Form, Input, InputProps } from "semantic-ui-react"
import { isKeyPressed } from "../common/util"
import css from "./ChatInput.module.css"

export interface IControlledComponent<Value> {
  value: Value
  onUpdate: (newValue: Value) => void
  onCommit: () => void
  onRevert: () => void,
}

export const ControlledInput
  : React.FunctionComponent<InputProps & IControlledComponent<string>>
  = (props) => {

    const { onUpdate, onCommit, onRevert, ...inherited } = props

    return <Input
      {...inherited}
      onKeyDown={(event: KeyboardEvent) => {
        if (isKeyPressed(event, "Enter")) {
          onCommit()
        } else if (isKeyPressed(event, "Escape")) {
          onRevert()
        }
      }}
      onChange={(_e, data) => {
        onUpdate(data.value)
      }}
      onPaste={(e: Event) => {
        e.preventDefault();
        return;
      }}
      className={css.chatInput}

    />
  }

export const ControlledFormInput
  : React.FunctionComponent<InputProps & IControlledComponent<string>>
  = (props) => {

    const { onUpdate, onCommit, onRevert, ...inherited } = props

    return <Form.Input
      {...inherited}
      onKeyDown={(event: KeyboardEvent) => {
        if (isKeyPressed(event, "Enter")) {
          onCommit()
        } else if (isKeyPressed(event, "Escape")) {
          onRevert()
        }
      }}
      onChange={(_e, data) => {
        onUpdate(data.value)
      }}

    />
  }

export interface IValueInputProperties extends InputProps {
  onEnter: (text: string) => void
}

interface IValueInputState {
  value: string
}

// noinspection JSUnusedGlobalSymbols
export class ValueInput
  extends React.Component<IValueInputProperties, IValueInputState> {

  constructor(props: IValueInputProperties) {
    super(props)
    this.state = { value: props.value || "" }
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

  public render(): React.ReactNode {

    // noinspection JSUnusedLocalSymbols
    const { onEnter, value, ...inherited } = this.props

    return <ControlledInput
      {...inherited}
      value={this.state.value}
      onCommit={this.onCommit}
      onRevert={this.onRevert}
      onUpdate={this.onChange}
    />
  }
}

// noinspection JSUnusedGlobalSymbols
export class ValueFormInput
  extends React.Component<IValueInputProperties, IValueInputState> {

  constructor(props: IValueInputProperties) {
    super(props)
    this.state = { value: props.value || "" }
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

  public render(): React.ReactNode {

    // noinspection JSUnusedLocalSymbols
    const { onEnter, value, ...inherited } = this.props

    return <ControlledFormInput
      {...inherited}
      value={this.state.value}
      onCommit={this.onCommit}
      onRevert={this.onRevert}
      onUpdate={this.onChange}
    />
  }
}
