/* tslint:disable:max-classes-per-file */
import React from "react"
import { Button, Dropdown, DropdownItemProps, DropdownProps, Form, Grid, InputOnChangeData, Segment } from "semantic-ui-react"
import { StringMap } from "../App"
import { ADConnection } from "../common/ADConnection"
import { convertDateToTimestamp, convertTimestampToDate } from "../common/util"
import { ChatComponent } from "../components/ChatComponent"
import { Dialogue } from "../components/DialogueModel"
import { Message } from "../components/MessageModel"
import { RecipePageComponent } from "../components/RecipePageComponent"
import { InteractionType } from "../generated/client_pb"
import { RecipeCheckboxModel } from "../models/RecipeCheckboxModel"
import { RecipeModel } from "../models/RecipeModel"
import { RecipeService } from "../services/RecipeService"
import css from "./WoZPanel.module.css"

interface IWozPanelState {
  connection?: ADConnection
  params: IWozParams
}

interface IWozPanelProperties {
  params: StringMap
  dialogue?: Dialogue
}

interface IWozParams {
  url: string
  userID: string
  conversationID: string,
  selectedRecipeId: string,
  dropdownRecipes?: object[],
}

interface IWoZParamFormProperties {
  params: IWozParams
  onSubmit: (params: IWozParams) => void
}

const areParamsValid = (params: IWozParams) => {
  return params.url !== ""
    && params.conversationID !== ""
    && params.userID !== ""
    && params.selectedRecipeId !== ""
}

export class WoZPanel
  extends React.Component<IWozPanelProperties, IWozPanelState> {

  constructor(props: IWozPanelProperties) {
    super(props)

    // console.log(this.props)

    const params: IWozParams = {
      conversationID: (props.params.conversationID || "").trim(),
      url: (props.params.url || "").trim(),
      userID: (props.params.userID || "").trim(),
      selectedRecipeId: (props.params.selectedRecipeId || "").trim(),
    }

    this.state = {
      connection: areParamsValid(params)
        ? new ADConnection(params.url) : undefined,
      params
    }
  }

  public async componentDidMount() {
    // Retrieve all the selected recipes the user can choose from 
    var recipes = await RecipeService.getAllRecipes()
    this.setState({
      params: {
        dropdownRecipes: recipes === undefined ? [] : recipes,
        url: this.state.params.url,
        userID: this.state.params.userID,
        conversationID: this.state.params.conversationID,
        selectedRecipeId: this.state.params.selectedRecipeId,
      }
    })
  }

  private onSubmit = (params: IWozParams) => {
    if (areParamsValid(params)) {
      this.setState({
        connection: new ADConnection(params.url),
        params,
      })
    }
  }

  public render(): React.ReactNode {
    return this.state.connection === undefined
      ? <WoZParamForm
        params={this.state.params}
        onSubmit={this.onSubmit}
      />
      : <WoZDialogue
        connection={this.state.connection}
        dialogue={this.props.dialogue}
        params={this.state.params}
      />
  }
}

class WoZParamForm extends React.Component<IWoZParamFormProperties, IWozParams> {

  constructor(props: IWoZParamFormProperties) {
    super(props)
    this.state = props.params
  }

  public handleChange = (_e: any, data: InputOnChangeData) => {
    this.setState((prev) => (
      { ...prev, [data.name]: data.value.trim() }
    ))
  }

  public handleChangeDropdown = (_e: any, data: DropdownProps) => {

    this.setState((prev) => (
      { ...prev, [data.name]: data.value }
    ))
  }

  public handleSubmit = async () => {
    if (areParamsValid(this.state)) {
      this.props.onSubmit(this.state)
    }
  }

  // Function used in order to conver recipes into elements
  // that can be shown in the dropdown 
  private convertToDropdownOptions = (): DropdownItemProps[] => {

    var counter = 0
    let options = this.props.params.dropdownRecipes?.map((recipe: object) => {
      var recipe_obj = JSON.parse(JSON.stringify(recipe))
      counter += 1;
      return {
        key: counter,
        text: recipe_obj['page_title'],
        value: recipe_obj['id'],
      }
    });

    // Sort the list alphabetically
    options = options?.sort((a, b) => {
      if (a.text > b.text) return 1;
      if (a.text < b.text) return -1;
      return 0;
    });

    return options === undefined ? [] : options;

  }

  public render(): React.ReactNode {
    const { conversationID, url, userID, selectedRecipeId } = this.state
    return <Segment>
      <Form onSubmit={this.handleSubmit}>
        <Form.Input label="Host URL" name="url" value={url}
          onChange={this.handleChange} />
        <Form.Input label="User ID" name="userID" value={userID}
          onChange={this.handleChange} />
        <Form.Input label="Conversation ID" name="conversationID"
          value={conversationID} onChange={this.handleChange} />
        <Form.Field>
          <label>Select Recipe</label>
          <Dropdown label="" name="selectedRecipeId" options={this.convertToDropdownOptions()}
            value={selectedRecipeId} onChange={this.handleChangeDropdown} placeholder='Select Recipe' selection />
        </Form.Field>
        <Button type="submit">Submit</Button>

      </Form>
    </Segment>
  }
}

interface IWoZDialogueProperties {
  dialogue?: Dialogue
  connection: ADConnection
  params: IWozParams,
}

interface IWoZDialogueState {
  dialogue: Dialogue,
  selectedCheckboxList: RecipeCheckboxModel[]
  selectedRecipe?: RecipeModel
}

class WoZDialogue
  extends React.Component<IWoZDialogueProperties, IWoZDialogueState> {

  constructor(props: IWoZDialogueProperties) {
    super(props)

    // console.log(this.props)

    this.state = {
      dialogue: props.dialogue === undefined
        ? new Dialogue({ messages: [] }) : props.dialogue,
      selectedCheckboxList: []
    }

    this.props.connection.subscribe({
      conversationID: this.props.params.conversationID,
      onResponse: ((response) => {
        // console.log("response: ", response)
        const reply = response.asTextResponse()
        const message = new Message({ ...reply, id: reply.responseID, messageType: response.getInteractionList()[0].getType(), actions: response.getInteractionList()[0].getActionList(), time: convertTimestampToDate(response.getInteractionList()[0].getInteractionTime()) })
        this._append(message)
      }),
      userID: this.props.params.userID,
    })
  }

  public async componentDidMount() {

    var recipe = await RecipeService.getRecipeById(this.props.params.selectedRecipeId, this.props.connection);

    if (recipe !== undefined) {
      this.setState({
        selectedRecipe: recipe
      });
    }

  }

  private onEnter = (text: string, messageTypeParam = InteractionType.TEXT) => {
    // We need to create the message object with all the relevant and required properties
    const message = new Message({
      userID: this.props.params.userID,
      text,
      messageType: messageTypeParam,
      loggedUserRecipePageIds: this.state.selectedCheckboxList !== undefined ? this.state.selectedCheckboxList.map((selectedCheckbox) => {
        return selectedCheckbox.pageId !== undefined ? selectedCheckbox.pageId : ''
      }).filter((el) => {
        return el !== ''
      }) : [],
      loggedUserRecipePageTitle: this.state.selectedCheckboxList !== undefined ? this.state.selectedCheckboxList.map((selectedCheckbox) => {
        return selectedCheckbox.pageTitle !== undefined ? selectedCheckbox.pageTitle : ''
      }).filter((el) => {
        return el !== ''
      }) : [],
      loggedUserRecipeSection: this.state.selectedCheckboxList !== undefined ? this.state.selectedCheckboxList.map((selectedCheckbox) => {
        return selectedCheckbox.section !== undefined ? selectedCheckbox.section : ''
      }).filter((el) => {
        return el !== ''
      }) : [],
      loggedUserRecipeSectionValue: this.state.selectedCheckboxList !== undefined ? this.state.selectedCheckboxList.map((selectedCheckbox) => {
        return selectedCheckbox.sectionValue !== undefined ? selectedCheckbox.sectionValue : ''
      }).filter((el) => {
        return el !== ''
      }) : [],
      loggedUserRecipeSelectTimestamp: this.state.selectedCheckboxList !== undefined ? this.state.selectedCheckboxList.map((selectedCheckbox) => {
        return selectedCheckbox.clickedTimestamp !== undefined ? convertDateToTimestamp(selectedCheckbox.clickedTimestamp) : convertDateToTimestamp(new Date())
      }).filter((el) => {
        return el !== null
      }) : [],

    })


    this.props.connection.send(message, {
      conversationID: this.props.params.conversationID,
    })
    this._append(message)

    // When sending the message we need to empty all the selected recipe checkboxes 
    // as the selected ones are associated to the message that we just sent
    // However we have to reset the list only when we send a text message
    if (messageTypeParam === InteractionType.TEXT) {
      this.setState({
        selectedCheckboxList: []
      })
    }
  }

  private _append = (message: Message) => {
    //if (message.text.trim().length === 0) { return }

    this.setState((prev) => {
      return { dialogue: prev.dialogue.appending(message, 300) }
    })
  }

  // Method used in order to keep track of the pressed buttons
  private onSelectCheckbox = (selectedCheckbox: RecipeCheckboxModel) => {

    // Flag used in order to detect whether we are checking or unchecking a recipe
    var flagCheckboxRemoved = false;

    // Check if the checkbox is already in the list 
    if ((this.state.selectedCheckboxList.filter(checkbox => checkbox.isEqual(selectedCheckbox))).length === 1) {
      // If the checkbox has already been selected we unselect it. 
      this.setState({
        selectedCheckboxList: this.state.selectedCheckboxList.filter(checkbox => !checkbox.isEqual(selectedCheckbox))
      })

      flagCheckboxRemoved = true;

    } else {
      // Otherwise we simply add it. 
      // Get the specific time the button has been clicked
      selectedCheckbox.clickedTimestamp = new Date();

      this.state.selectedCheckboxList.push(selectedCheckbox);
      this.setState({
        selectedCheckboxList: this.state.selectedCheckboxList
      })
    }

    // We need now to send a message to the wizad. This message will be of type status and the user won't be able to see it. 
    // It will only be used by the wizard to know which parts of the interface the user is looking at. 
    // However we first need to generate the specific message text for the wizar 
    var labelRemoveOrAdded = flagCheckboxRemoved ? 'removed' : 'added';
    var messageText = 'User ' + labelRemoveOrAdded + ' "' + selectedCheckbox.sectionValue + '" from section "' + selectedCheckbox.section + '".'

    this.onEnter(messageText, InteractionType.STATUS);
  }


  // Method used to send a message to the Wizard when the user navigates to a new
  // section 
  private onRecipeSectionButtonClick = (sectionKey: string) => {
    // We need now to send a message to the wizad. This message will be of type status and the user won't be able to see it. 
    // It will only be used by the wizard to know which parts of the interface the user is looking at. 
    // However we first need to generate the specific message text for the wizard

    if (sectionKey !== undefined && sectionKey.trim().length !== 0) {
      var messageText = 'User moved to section "' + sectionKey + '".'
      this.onEnter(messageText, InteractionType.STATUS);
    }

  }



  public render(): React.ReactNode {

    return (<Grid id={css.appGroupId}>
      <Grid.Column width={8} className={css.gridColumn}>
        <RecipePageComponent
          recipeObj={this.state.selectedRecipe}
          onSelectCheckbox={this.onSelectCheckbox}
          selectedCheckboxList={this.state.selectedCheckboxList}
          onRecipeSectionButtonClick={this.onRecipeSectionButtonClick}
          dialogue={this.state.dialogue}
          us={this.props.params.userID}></RecipePageComponent>
      </Grid.Column>
      <Grid.Column width={8}>
        <ChatComponent
          dialogue={this.state.dialogue}
          us={this.props.params.userID}
          them={[]}
          onEnter={this.onEnter}
        />
      </Grid.Column>
    </Grid>);
  }
}
