import * as React from "react"
import { Button, Checkbox, Container, Header, Icon, Message } from "semantic-ui-react"
import { diffSecondsBetweenDates, isStringImagePath, playTextToAudio } from "../common/util";
import { InteractionType } from "../generated/client_pb";
import { RecipeCheckboxModel } from "../models/RecipeCheckboxModel";
import { RecipeModel } from "../models/RecipeModel";
import { IDialogue } from "./DialogueModel";
import css from "./RecipePageComponent.module.css"



interface IRecipePageComponentProperties {
  recipeObj?: RecipeModel,
  selectedCheckboxList: RecipeCheckboxModel[];
  onSelectCheckbox: (model: RecipeCheckboxModel) => void,
  onRecipeSectionButtonClick: (sectionDescription: string) => void,
  showFullPageCheckList?: boolean
  showCheckBoxes?: boolean,
  isSequentialNavigationEnabled?: boolean
  isTextToSpeechEnabled: boolean
  dialogue?: IDialogue,
  us?: string
}


export class RecipePageComponent extends React.Component<IRecipePageComponentProperties> {

    private recipeSections: string[] = [];
    // Specific recipe section to display if the view is single section rather than full page
    private recipeSectionIndex: number = 0;

    public static defaultProps = {
        showFullPageCheckList: false,
        showCheckBoxes: false,
        isSequentialNavigationEnabled: false,
        isTextToSpeechEnabled: false
    };


    constructor(props: IRecipePageComponentProperties) {
        super(props);
        this.handleRecipeNavigation = this.handleRecipeNavigation.bind(this);
    }

    private wizardNavigationController(): void {
        // Here we handle the automatic navigation to different sections based on the last message sent by the Wizard. 
        // More precisely if the last message sent by the user is of type IteractionType.ACTION then we execute the specific action. 
        // However we execute the operation only in the case the message has been sent no more than 5 seconds (otherwise we would
        // trigger the behaviour when not required)

        // We first check if the navigation is controlled by the wizard 
        if (!this.props.isSequentialNavigationEnabled) {
            // If that is the case we get the last message
            var last_message = undefined;
            if (this.props.dialogue?.messages !== undefined && this.props.dialogue.messages.length !== 0) {
                last_message = this.props.dialogue.messages[this.props.dialogue.messages.length - 1];
            }

            // Here we check if the last message has been sent by the wizard and if the message is of type action 
            if (last_message !== undefined && last_message?.userID !== this.props.us && last_message?.messageType === InteractionType.ACTION) {
                if(true) { // always want to "replay" step changes if resuming a conversation
                   // We extract the actions
                    var actions = last_message.actions;
                    let did_change = true
                    if (actions !== undefined && actions.length !== 0) {
                        // the "next" and "prev" actions won't be used in the current version, since the 
                        // navigation is controlled by the LLM API responses
                        if (actions[0] === 'prev') {
                            // Update the recipeSectionIndex only if we are not at step 0
                            if (this.recipeSectionIndex !== 0) {
                                this.recipeSectionIndex -= 1;
                            }
                        }
                        if (actions[0] === 'next') {
                            // Update the recipeSectionIndex only if we are not at the end of the list
                            if (this.recipeSectionIndex < this.recipeSections.length - 1) {
                                this.recipeSectionIndex += 1;
                            }
                        }

                        // this section is where the step changes are currently triggered. the WoZ
                        // app will send actions of the form "stepX" where X is the step number to
                        // jump to. NOTE: these numbers will be 0-based!
                        if (actions[0].startsWith('step')) {
                            let step = parseInt(actions[0].replace("step", ""))
                            console.log("received action %o, jumping from step %o to step %o", actions[0], this.recipeSectionIndex, step)
                            console.log("from message %o", last_message)
                            did_change = (this.recipeSectionIndex !== step)
                            if(did_change) {
                                this.recipeSectionIndex = step
                            }
                        }

                        // Once we move the a new section we also need to read section title and provide prompt 
                        // to the user 
                        if (this.props.isTextToSpeechEnabled) {
                            var textString = "Here we are in section " + this.recipeSections[this.recipeSectionIndex] + "! Have a read and feel free to ask me questions!";
                            playTextToAudio(textString);
                        }

                        // At the end of the interaction we notify the Wizard of the actual change happening 
                        if(did_change) {
                            this.props.onRecipeSectionButtonClick(this.recipeSections[this.recipeSectionIndex])
                        }
                    }
                }
            }
        }
    }

    // Method used in order to generate the page body associated to this recipe
    private generatePageRecipeCheckboxModel(): Map<string, RecipeCheckboxModel[]> {
        let tempRecipeModel = this.props.recipeObj;
        var resultMap = new Map<string, RecipeCheckboxModel[]>()
            if (tempRecipeModel !== undefined) {
                // Generate the steps checkbox models
                if (tempRecipeModel?.stepsSentencesWithImages !== undefined) {
                    for (let i = 0; i < tempRecipeModel?.stepsSentencesWithImages.length; i++) {
                        for (let j = 0; j < tempRecipeModel?.stepsSentencesWithImages[i].length; j++) {
                            let tempRecipeCheckboxModel = tempRecipeModel?.recipeModelToRecipeCheckboxModel('Step ' + (i + 1), tempRecipeModel?.stepsSentencesWithImages[i][j]);
                            if (resultMap.has('Step ' + (i + 1))) {
                                let innerRecipeStepsList = resultMap.get('Step ' + (i + 1))
                                    if (innerRecipeStepsList !== undefined)
                                        resultMap.set('Step ' + (i + 1), innerRecipeStepsList?.concat([tempRecipeCheckboxModel]));
                            } else {
                                resultMap.set('Step ' + (i + 1), [tempRecipeCheckboxModel]);
                            }
                        }
                    }
                }
            }

        // Get the array of all the sections
        this.recipeSections = Array.from(resultMap.keys()) !== undefined ? Array.from(resultMap.keys()) : [];

        return resultMap
    }

    // Method used to generate the page body
    private generatePageBodyFullPage(): JSX.Element[] {
        let recipeMap = this.generatePageRecipeCheckboxModel();
        var pageBody: JSX.Element[] = []
        var counter = 0

        if (recipeMap.size > 0) {
            recipeMap.forEach((value, key) => {

            let sectionCheckboxes = value.map((el: RecipeCheckboxModel, index: number) => {
                let checkboxJsxElement = this.props.showCheckBoxes ? <Checkbox key={index} 
                                                                            onChange={() => this.props.onSelectCheckbox(el)} 
                                                                            checked={this.props.selectedCheckboxList.filter(checkbox => checkbox.isEqual(el)).length === 1} 
                                                                            className={css.recipeCheckbox}>
                                                                </Checkbox> 
                                                                : 
                                                                undefined;
                    // Here we show either an image or the associated checkboxes
                    return isStringImagePath(el.sectionValue) ? 
                        <img src={el.sectionValue} className={css.recipeStepImage}></img> 
                        : 
                        <div>{checkboxJsxElement}
                            <div className={css.checkBoxLabel}>{
                                this.props.showCheckBoxes ? undefined : "- "}{el.sectionValue}
                            </div>
                        </div>
                });
                if (sectionCheckboxes.length !== 0) {
                    var section = (<div className={css.recipeSectionDiv} key={counter}><Header as='h3'>{key}</Header>{sectionCheckboxes}</div>)
                    pageBody.push(section);
                }

                counter += 1;

            });
        }
        return pageBody;

    }

    // Method used to generate the page body
    private generatePageBodySingleSection(displaySection: number = 0): JSX.Element[] {
        let recipeMap = this.generatePageRecipeCheckboxModel();
        var pageBody: JSX.Element[] = []
        var counter = 0

        if (recipeMap.size > 0) {
            // Get the key of the section we want to display 
            let currentSectionKey = Array.from(recipeMap.keys())[displaySection];
            let currentSectionValues = recipeMap.get(currentSectionKey)
            if (currentSectionValues !== undefined) {
                let sectionCheckboxes = currentSectionValues.map((el: RecipeCheckboxModel, index: number) => {
                    let checkboxJsxElement = this.props.showCheckBoxes ? 
                        <Checkbox key={index} 
                            onChange={() => this.props.onSelectCheckbox(el)} 
                            checked={this.props.selectedCheckboxList.filter(checkbox => checkbox.isEqual(el)).length === 1} 
                            className={css.recipeCheckbox}></Checkbox> 
                        : 
                        undefined;

                    // this is a bit hacky but uses __dangerouslySetInnerHTML to allow the supplied text from the topic JSON data to be 
                    // formatted as HTML
                    // return <div>{checkboxJsxElement}<div className={css.checkBoxLabel} key={index} dangerouslySetInnerHTML={{__html: foo}}></div></div>
                    return <div className={css.checkBoxLabel} key={index} dangerouslySetInnerHTML={{__html: el.sectionValue}}></div>
                });

                if (sectionCheckboxes.length !== 0) {
                    var section = (<div className={css.recipeSectionDiv} key={counter}>{sectionCheckboxes}</div>)
                    pageBody.push(section);
                }

                counter += 1;
            }
        }
        return pageBody;
    }

  // Generate the button interface used to navigate between sections
  // The method also takes the current section to displat as input 
  // So that we know exaclty if we need to show the previous and next buttons
  private generateButtonsMenu(displaySection: number = 0): JSX.Element[] {
    var result: JSX.Element[] = []
    // We show the buttons only if we don't have the full page flag enabled
    if (!this.props.showFullPageCheckList) {
      // We show the previous button only if we are not at the first section
      if (displaySection > 0) {
        result.push(<Button type="submit" floated="left" key={displaySection} onClick={() => { this.handleRecipeNavigation(-1) }} color='grey'><Icon name='arrow left' />Previous</Button>)
      }
      // We show the next button only if we are not at the last section
      if (displaySection < this.recipeSections.length - 1) {
        result.push(<Button type="submit" floated="right" key={displaySection + 1} onClick={() => { this.handleRecipeNavigation(1) }} className={css.nextButton}>Next<Icon name='arrow right' /></Button>)
      }

    }

    return result;

  }

  // Method used in order to handle the navigation between sections
  private handleRecipeNavigation(move: number) {

    this.recipeSectionIndex += move
    this.setState({
    })


    if (this.recipeSections !== undefined && this.recipeSections.length !== 0) {
      this.props.onRecipeSectionButtonClick(this.recipeSections[this.recipeSectionIndex]);
    }
  }

  componentDidUpdate() {
    // Right before rendering check whether we should update the recipe index or not
    this.wizardNavigationController();
  }

  public render(): React.ReactNode {

    // NOTE: this seems to generate a React warning/error because it mutates state/props
    // during a render(), so I've moved it to componentDidUpdate() for now..
    //
    // Right before rendering check whether we should update the recipe index or not
    // this.wizardNavigationController();

    let pageBody: JSX.Element[] = this.props.showFullPageCheckList ? this.generatePageBodyFullPage() : this.generatePageBodySingleSection(this.recipeSectionIndex);
    let errorMessage = <Message color='red' key='0'>An error occurred when retrieving the recipe. Try again later.</Message>
    return <div className={css.root}>
      <Container className={css.recipeComponentContainer} textAlign='left'>
        <Header as='h2'>{this.props.recipeObj?.pageTitle}</Header>
        {pageBody.length > 0 ? pageBody : errorMessage}
        {this.props.isSequentialNavigationEnabled ? this.generateButtonsMenu(this.recipeSectionIndex) : undefined}

      </Container>
    </div>
  }
}
