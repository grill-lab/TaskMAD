import * as React from "react"
import { Button, Checkbox, Container, Header, Icon, Message } from "semantic-ui-react"
import { diffSecondsBetweenDates, isStringImagePath, playTextToAudio } from "../common/util";
import { InteractionAction, InteractionType } from "../generated/client_pb";
import { RecipeModel } from "../models/RecipeModel";
import { SequentialPageCheckboxModel } from "../models/SequentialPageCheckboxModel";
import { ISequentialPageModel } from "../models/SequentialPageModel";
import { IDialogue } from "./DialogueModel";
import css from "./SequentialPageComponent.module.css"



interface ISequentialPageComponentProperties {
  sequentialPageObj?: ISequentialPageModel,
  selectedCheckboxList: SequentialPageCheckboxModel[];
  onSelectCheckbox: (model: SequentialPageCheckboxModel) => void,
  onSectionButtonClick: (sectionDescription: string) => void,
  isSequentialNavigationEnabled?: boolean
  isTextToSpeechEnabled?: boolean
  isSequentialComponentFullPage?: boolean
  showSequentialPageCheckboxes?: boolean
  dialogue?: IDialogue,
  us?: string
}

interface ISequentialPageComponent {
  generateSequentialPageCheckboxBody(): Map<string, SequentialPageCheckboxModel[]>;
}


export class SequentialPageComponent
  extends React.Component<ISequentialPageComponentProperties> implements ISequentialPageComponent {

  private pageSections: string[] = [];
  // Specific recipe section to display if the view is single section rather than full page
  private pageSectionIndex: number = 0;


  constructor(props: ISequentialPageComponentProperties) {
    super(props);


    this.handleSectionsNavigation = this.handleSectionsNavigation.bind(this);
  }

  private wizardNavigationController(): void {

    // Here we handle the automatic navigation to different sections based on the last message sent by the Wizard. 
    // More precisely if the last message sent by the user is of type IteractionType.ACTION then we execute the specific action. 
    // However we execute the operation only in the case the message has been sent no more than 5 seconds (otherwise we would
    // trigger the behaviour when not required)

    // We first check if the navigation is controlled by the wizard 
    if (!this.props.isSequentialNavigationEnabled) {
      // If that is the case we get the last message
      let last_message = undefined;
      if (this.props.dialogue?.messages !== undefined && this.props.dialogue.messages.length !== 0) {
        last_message = this.props.dialogue.messages[this.props.dialogue.messages.length - 1];
      }
      console.log(last_message);


      // Here we check if the last message has been sent by the wizard and if the message is of type action 
      if (last_message !== undefined && last_message?.userID !== this.props.us && last_message?.messageType === InteractionType.ACTION) {

        // If that is the case we check whether the message has been sent no more than 5 seconds ago 
        if (last_message?.time.getTime() !== undefined && diffSecondsBetweenDates(last_message?.time, new Date()) <= 5) {

          // We extract the actions
          let actions = last_message.actions;
          if (actions !== undefined && actions.length !== 0) {
            // Here we handle the page reload based on the current step we are in 
            if (actions[0] === InteractionAction.PREVIOUS_STEP) {
              // Update the recipeSectionIndex only if we are not at step 0
              if (this.pageSectionIndex !== 0) {
                this.pageSectionIndex -= 1;
              }
            }
            if (actions[0] === InteractionAction.NEXT_STEP) {
              // Update the recipeSectionIndex only if we are not at the end of the list
              if (this.pageSectionIndex < this.pageSections.length - 1) {
                this.pageSectionIndex += 1;
              }
            }

            // Once we move the a new section we also need to read section title and provide prompt 
            // to the user 
            if (this.props.isTextToSpeechEnabled) {
              let textString = "Here we are in section " + this.pageSections[this.pageSectionIndex] + "! Have a read and feel free to ask me questions!";
              playTextToAudio(textString);
            }

            // At the end of the interaction we notify the Wizard of the actual change happening 
            this.props.onSectionButtonClick(this.pageSections[this.pageSectionIndex])
          }
        }

      }
    }
  }


  // Method used in order to generate the page body associated to this recipe
  generateSequentialPageCheckboxBody(): Map<string, SequentialPageCheckboxModel[]> {
    let tempRecipeModel = this.props.sequentialPageObj as RecipeModel;
    let resultMap = new Map<string, SequentialPageCheckboxModel[]>()
    if (tempRecipeModel !== undefined) {

      // Generate the time sections
      let prepTimeRecipeCheckboxModel = tempRecipeModel.sequentialPageModelToSequentialPageCheckboxModel('Time', 'Preparation Time: ' + tempRecipeModel.durationMinutesPrep + ' minutes');
      let cookingTimeRecipeCheckboxModel = tempRecipeModel.sequentialPageModelToSequentialPageCheckboxModel('Time', 'Cooking Time: ' + tempRecipeModel.durationMinutesCooking + ' minutes');
      let totalTimeRecipeCheckboxModel = tempRecipeModel.sequentialPageModelToSequentialPageCheckboxModel('Time', 'Total Time: ' + tempRecipeModel.durationMinutesTotal + ' minutes');


      // Create checkbox objects for the required equipment
      let requiredEquipmentList = tempRecipeModel.requiredEquipment.map((el: string) => {
        return tempRecipeModel.sequentialPageModelToSequentialPageCheckboxModel('Required equipment', el);
      })

      // Create checkbox objects for the required ingredients
      let requiredIngredientsList = tempRecipeModel.requiredIngredients.map((el: string) => {
        return tempRecipeModel.sequentialPageModelToSequentialPageCheckboxModel('Required Ingredients', el);
      })

      resultMap.set('Time, Equipment & Ingredients', [prepTimeRecipeCheckboxModel, cookingTimeRecipeCheckboxModel, totalTimeRecipeCheckboxModel].concat(requiredEquipmentList).concat(requiredIngredientsList));

      // Generate the steps checkbox models
      if (tempRecipeModel?.stepsSentencesWithImages !== undefined) {
        for (let i = 0; i < tempRecipeModel?.stepsSentencesWithImages.length; i++) {
          for (let j = 0; j < tempRecipeModel?.stepsSentencesWithImages[i].length; j++) {
            let tempRecipeCheckboxModel = tempRecipeModel?.sequentialPageModelToSequentialPageCheckboxModel('Step ' + (i + 1), tempRecipeModel?.stepsSentencesWithImages[i][j]);
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
    this.pageSections = Array.from(resultMap.keys()) !== undefined ? Array.from(resultMap.keys()) : [];

    return resultMap;


  }

  // Method used to generate the page body
  private generatePageBodyFullPage(): JSX.Element[] {
    let sequentialPageMap = this.generateSequentialPageCheckboxBody();
    let pageBody: JSX.Element[] = []
    let counter = 0

    if (sequentialPageMap.size > 0) {
      sequentialPageMap.forEach((value, key) => {

        let sectionCheckboxes = value.map((el: SequentialPageCheckboxModel, index: number) => {

          let checkboxJsxElement = this.props.showSequentialPageCheckboxes ? <Checkbox key={index} onChange={() => this.props.onSelectCheckbox(el)} checked={this.props.selectedCheckboxList.filter(checkbox => checkbox.isEqual(el)).length === 1} className={css.sequentialCheckbox}></Checkbox> : undefined;
          // Here we show either an image or the associated checkboxes
          return isStringImagePath(el.sectionValue) ? <img src={el.sectionValue} className={css.stepImage}></img> : <div>{checkboxJsxElement}<div className={css.checkBoxLabel}>{this.props.showSequentialPageCheckboxes ? undefined : "- "}{el.sectionValue}</div></div>
        });
        if (sectionCheckboxes.length !== 0) {
          let section = (<div className={css.sectionDiv} key={counter}><Header as='h3'>{key}</Header>{sectionCheckboxes}</div>)
          pageBody.push(section);
        }

        counter += 1;

      });
    }


    return pageBody;

  }

  // Method used to generate the page body
  private generatePageBodySingleSection(displaySection: number = 0): JSX.Element[] {
    let sequentialPageMap = this.generateSequentialPageCheckboxBody();
    let pageBody: JSX.Element[] = []
    let counter = 0;


    if (sequentialPageMap.size > 0) {
      // Get the key of the section we want to display 
      let currentSectionKey = Array.from(sequentialPageMap.keys())[displaySection];
      let currentSectionValues = sequentialPageMap.get(currentSectionKey)
      if (currentSectionValues !== undefined) {
        let sectionCheckboxes = currentSectionValues.map((el: SequentialPageCheckboxModel, index: number) => {

          let checkboxJsxElement = this.props.showSequentialPageCheckboxes ? <Checkbox key={index} onChange={() => this.props.onSelectCheckbox(el)} checked={this.props.selectedCheckboxList.filter(checkbox => checkbox.isEqual(el)).length === 1} className={css.sequentialCheckbox}></Checkbox> : undefined;

          // Here we show either an image or the associated checkboxes
          return isStringImagePath(el.sectionValue) ? <img src={el.sectionValue} className={css.stepImage}></img> : <div>{checkboxJsxElement}<div className={css.checkBoxLabel}>{this.props.showSequentialPageCheckboxes ? undefined : "- "}{el.sectionValue}</div></div>
        });
        if (sectionCheckboxes.length !== 0) {
          let section = (<div className={css.sectionDiv} key={counter}><Header as='h3'>{currentSectionKey}</Header>{sectionCheckboxes}</div>)
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
    let result: JSX.Element[] = []
    // We show the buttons only if we don't have the full page flag enabled
    if (!this.props.isSequentialComponentFullPage) {
      // We show the previous button only if we are not at the first section
      if (displaySection > 0) {
        result.push(<Button type="submit" floated="left" key={displaySection} onClick={() => { this.handleSectionsNavigation(-1) }} color='grey'><Icon name='arrow left' />Previous</Button>)
      }
      // We show the next button only if we are not at the last section
      if (displaySection < this.pageSections.length - 1) {
        result.push(<Button type="submit" floated="right" key={displaySection + 1} onClick={() => { this.handleSectionsNavigation(1) }} className={css.nextButton}>Next<Icon name='arrow right' /></Button>)
      }

    }

    return result;

  }

  // Method used in order to handle the navigation between sections
  private handleSectionsNavigation(move: number) {

    this.pageSectionIndex += move
    this.setState({
    })


    if (this.pageSections !== undefined && this.pageSections.length !== 0) {
      this.props.onSectionButtonClick(this.pageSections[this.pageSectionIndex]);
    }
  }



  public render(): React.ReactNode {

    // Right before rendering check whether we should update the recipe index or not
    this.wizardNavigationController();

    let pageBody: JSX.Element[] = this.props.isSequentialComponentFullPage ? this.generatePageBodyFullPage() : this.generatePageBodySingleSection(this.pageSectionIndex);
    let errorMessage = <Message color='red' key='0'>An error occurred when retrieving the recipe. Try again later.</Message>
    return <div className={css.root}>
      <Container className={css.componentContainer} textAlign='left'>
        <Header as='h2'>{this.props.sequentialPageObj?.pageTitle}</Header>
        {pageBody.length > 0 ? pageBody : errorMessage}
        {this.props.isSequentialNavigationEnabled ? this.generateButtonsMenu(this.pageSectionIndex) : undefined}

      </Container>
    </div>
  }
}
