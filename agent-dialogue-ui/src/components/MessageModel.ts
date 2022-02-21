import * as uuid from "uuid"
import {PartialBy} from "../common/util"
import { InteractionType } from "../generated/client_pb"

export interface IMessage {
  id: string
  text: string
  time: Date
  userID?: string

  loggedUserRecipePageIds?: Array<string>
  loggedUserRecipePageTitle?: Array<string>
  loggedUserRecipeSection?: Array<string>
  loggedUserRecipeSectionValue?: Array<string>
  loggedUserRecipeSelectTimestamp?: Array<number>

  // Specific type of this message
  messageType?: InteractionType
  actions?: Array<string>
}

export const ourUserID = "us"

export type IMessageArgument = PartialBy<IMessage, "time" | "id">

export class Message implements IMessage {
  constructor(argument: IMessageArgument | Date) {

    // By default each message is of type text
    this.messageType = InteractionType.TEXT
    
    if (argument instanceof Date) {
      const options = {
        day: "numeric",
        hour: "numeric",
        minute: "numeric",
        month: "numeric",
        second: "numeric",
        year: "numeric",
      }
      Object.assign(this, {
        id: uuid.v4(),
        text: new Intl.DateTimeFormat(undefined, options).format(argument),
        time: new Date(),
      })
    } else {
      Object.assign(this, {
        id: uuid.v4(),
        time: new Date(),
        ...argument,
      })
    }
  }

  // noinspection JSUnusedGlobalSymbols
  public readonly id!: string
  // noinspection JSUnusedGlobalSymbols
  public readonly text!: string
  // noinspection JSUnusedGlobalSymbols
  public readonly time!: Date
  // noinspection JSUnusedGlobalSymbols
  public readonly userID?: string

  // For each clicked checkbox we log the page Id 
  public readonly loggedUserRecipePageIds?: Array<string>
  // For each clicked checkbox we log the page title
  public readonly loggedUserRecipePageTitle?: Array<string>
  // For each clicked checkbox we log specific section where it comes from in the page
  public readonly loggedUserRecipeSection?: Array<string>
  // For each clicked checkbox we log the value associated to the checkbox
  public readonly loggedUserRecipeSectionValue?: Array<string>
  // For each clicked checkbox we log the timestamp of when the checkbox has been clicked
  public readonly loggedUserRecipeSelectTimestamp?: Array<number>

  public readonly messageType?: InteractionType
  public readonly actions?: Array<string>
}
