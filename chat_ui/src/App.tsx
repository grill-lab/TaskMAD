import React, { Component } from "react"
import css from "./App.module.css"
import { ChatConfigs } from "./common/chat_ui_configs"
import logo from "./resources/img/agent_logo.png"
import { WoZPanel } from "./woz/WoZPanel"

// tslint:disable-next-line:interface-name
export interface StringMap { [index: string]: string }

interface IAppState {
  readonly params: StringMap
}

class App extends Component<{}, IAppState> {

  constructor(props: {}) {
    super(props)

    const params: StringMap = {
      conversationID: ChatConfigs.conversation_id,
      url: ChatConfigs.url,
      userID: ChatConfigs.userID,
      isAudioRecordingEnabled: ChatConfigs.isAudioRecordingEnabled,
      isTextToSpeechEnabled: ChatConfigs.isTextToSpeechEnabled,
      isSequentialNavigationEnabled: ChatConfigs.isSequentialNavigationEnabled,
      isSequentialComponentFullPage: ChatConfigs.isSequentialComponentFullPage,
      showSequentialPageCheckboxes: ChatConfigs.showSequentialPageCheckboxes
    }
    new URL(window.location.href)
      .searchParams.forEach((value, key) => {
        params[key] = value
      })

    this.state = {
      params,
    }

  }

  public render() {

    return (
      <div className={css.app} style={{ backgroundImage: `url(${ChatConfigs.chat_ui_background_image})` }}>
        <div className={css.overlayLayer}>
          <header className={css.appHeader} style={{ backgroundColor: ChatConfigs.chat_ui_header_color }}>
            <img src={logo} className={css.appLogo} alt="logo" />
            <h1 className={css.appTitle}>{ChatConfigs.chat_ui_title}</h1>
          </header>
          <WoZPanel params={this.state.params} />
        </div>
      </div>
    )
  }
}

export default App
