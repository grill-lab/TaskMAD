import React, { Component } from "react"
import css from "./App.module.css"
// import {HomePanel} from "./home/HomePanel"
// import {RatingsPanel} from "./ratings/RatingsPanel"
import logo from "./resources/img/uog_logo.png"
import { WoZPanel } from "./woz/WoZPanel"

// tslint:disable-next-line:interface-name
export interface StringMap { [index: string]: string }

interface IAppState {
  readonly params: StringMap
}

class App extends Component<{}, IAppState> {

  constructor(props: {}) {
    super(props)

    const params: StringMap = {}
    //   conversationID: "test",
    //   url: "http://localhost:8080",
    //   userID: "test",
    // }
    new URL(window.location.href)
      .searchParams.forEach((value, key) => {
        params[key] = value
      })

    this.state = {
      params,
    }

  }

  public render() {

    // const panes = [
    //   {
    //     menuItem: "Home",
    //     render: () => <Tab.Pane
    //         className={css.mainTabPane} attached><HomePanel/></Tab.Pane>,
    //   },
    //   { menuItem: "Offline MT Ratings",
    //     render: () => <Tab.Pane
    //         className={css.mainTabPane} attached><RatingsPanel/></Tab.Pane>,
    //   },
    //   { menuItem: "TaskMAD",
    //     render: () => <Tab.Pane
    //         className={css.mainTabPane} attached>
    //       <WoZPanel params={this.state.params}/></Tab.Pane>,
    //   },
    // ]

    return (
      <div className={css.app}>
        <div className={css.overlayLayer}>
          <header className={css.appHeader}>
            <img src={logo} className={css.appLogo} alt="logo" />
            <h1 className={css.appTitle}>Cooking Masterclass</h1>
          </header>
          {/* <Tab className={css.mainTab}
              menu={{
                attached: true,
                color: "orange",
                inverted: true,
                tabular: false }}
              panes={panes} /> */}
          <WoZPanel params={this.state.params} />
          <a className={css.appFooter} href="https://www.flaticon.com/free-icons/chatbot" title="chatbot icons"></a>
        </div>
      </div>
    )
  }
}

export default App
