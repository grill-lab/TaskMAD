import * as uuid from "uuid"
import {PartialBy} from "../common/util"
import { InteractionAction, InteractionLogs, InteractionType } from "../generated/client_pb"

export interface IMessage {
  id: string
  text: string
  time: Date
  userID?: string
  interactionLogs?: InteractionLogs
  // Specific type of this message
  messageType?: InteractionType
  actions?: Array<InteractionAction>
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

  public readonly interactionLogs?: InteractionLogs
  public readonly messageType?: InteractionType
  public readonly actions?: Array<InteractionAction>
}
