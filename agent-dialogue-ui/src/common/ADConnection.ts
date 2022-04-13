/* tslint:disable:max-classes-per-file */
import {JavaScriptValue, Struct} from "google-protobuf/google/protobuf/struct_pb"
import {Timestamp} from "google-protobuf/google/protobuf/timestamp_pb"
import * as grpcWeb from "grpc-web"
import {Omit} from "./util"
import {IMessage} from "../components/MessageModel"
import {
  ClientId, InputInteraction, InteractionRequest,
  InteractionResponse,
  InteractionType,
} from "../generated/client_pb"
import {AgentDialogueClient} from "../generated/ServiceServiceClientPb"

export interface IInputInteractionArguments {
  languageCode?: string
  text?: string
  messageType?: InteractionType
  actions?: Array<string>
  loggedUserRecipePageIds?: Array<string>
  loggedUserRecipePageTitle?: Array<string>
  loggedUserRecipeSection?: Array<string>
  loggedUserRecipeSectionValue?: Array<string>
  loggedUserRecipeSelectTimestamp?: Array<number>

}

export interface IRequestArguments extends IInputInteractionArguments {
  chosenAgentList?: string[]
  clientID?: ClientId
  conversationID?: string
  time?: Date
  userID: string
}

export type ISendOptions = Omit<IRequestArguments, "text" | "time" | "userID">

export interface ISubscribeArguments extends IRequestArguments {
  onResponse: (response: InteractionResponse) => void
  onError?: (error: grpcWeb.Error) => void
  onStatus?: (error: grpcWeb.Status) => void
  onEnd?: () => void
}

export interface ISubscription {
  invalidate: () => void
}

interface IConcreteSubscription {
  readonly call: grpcWeb.ClientReadableStream<InteractionResponse>
  readonly client: ADConnection
  readonly request: ISubscribeArguments
}

class ConcreteSubscription implements IConcreteSubscription, ISubscription {
  constructor(args: IConcreteSubscription) {
    Object.assign(this, args)
  }

  public readonly client!: ADConnection
  public readonly call!: grpcWeb.ClientReadableStream<InteractionResponse>
  public readonly request!: ISubscribeArguments

  // noinspection JSUnusedGlobalSymbols
  public invalidate = () => {
    this.call.cancel()
    this.client.remove(this)
  }
}

interface IADTextResponse {
  responseID: string
  text: string
  userID: string
  time: Date
  messageType: InteractionType
}

declare module "../generated/client_pb" {
  // tslint:disable-next-line:interface-name
  interface InteractionResponse {
    asTextResponse(): IADTextResponse
  }
}

// noinspection JSUnusedGlobalSymbols
proto.edu.gla.kail.ad.InteractionResponse.prototype.asTextResponse =
    function(): IADTextResponse {      
  return {
    responseID: this.getResponseId(),
    text: this.getInteractionList()[0].getText(),
    time: new Date(this.getTime().getSeconds() * 1000
                   + this.getTime().getNanos() / 1e+6),
    userID: this.getUserId(),
    messageType: this.getInteractionList()[0].getType()
  }
}

export class ADConnection {

  // noinspection JSUnusedGlobalSymbols
  public get hostURL(): string {
    return this._hostURL
  }

  // noinspection JSUnusedGlobalSymbols
  public set hostURL(url: string) {
    if (url === this._hostURL) { return }
    this._subscriptions.forEach((sub) => {sub.call.cancel()})
    this._hostURL = url.replace(/\/+$/, "")
    this._client = undefined
    this._subscriptions = this._subscriptions.map(
        (sub) => this._subscribe(sub.request))
  }

  constructor(host: string) {    
    this._hostURL = host.replace(/\/+$/, "")
    this._subscriptions = []
  }

  private _hostURL: string
  private _client?: AgentDialogueClient
  private _subscriptions: ConcreteSubscription[]

  private _makeInputInteraction = (args: IInputInteractionArguments)
      : InputInteraction => {
      
    // tslint:disable-next-line:new-parens
    const input = new InputInteraction()
    input.setText(args.text || "")
    input.setLanguageCode(args.languageCode || "en-US")    
    input.setType(args.messageType || InteractionType.TEXT)
    input.setActionList(args.actions || []);
    // Set the fields for the loggin functionalities
    input.setLoggedUserRecipePageIdsList(args.loggedUserRecipePageIds || []);
    input.setLoggedUserRecipePageTitleList(args.loggedUserRecipePageTitle || []);
    input.setLoggedUserRecipeSectionList(args.loggedUserRecipeSection || []);
    input.setLoggedUserRecipeSectionValueList(args.loggedUserRecipeSectionValue || []);
    input.setLoggedUserRecipeSelectTimestampList(args.loggedUserRecipeSelectTimestamp || []);
    
    return input
  }

  // noinspection SpellCheckingInspection
  private _makeInteractionRequest = (args: IRequestArguments)
      : InteractionRequest => {

    const input = this._makeInputInteraction(args)

    // tslint:disable-next-line:new-parens
    const request = new InteractionRequest()
    request.setClientId(args.clientID || ClientId.WEB_SIMULATOR)
    request.setInteraction(input)
    request.setUserId(args.userID)

    const time = args.time || new Date()
    const timestamp = new Timestamp()
    timestamp.setSeconds(Math.floor(time.getTime() / 1000))
    timestamp.setNanos((Math.floor(time.getTime()) % 1000) * 1e+6)
    request.setTime((timestamp as any)) // fuck Typescript,
    // fuck JavaScript, fuck half-assed grpc-web, and fuck spell-checking
    request.setChosenAgentsList(args.chosenAgentList || ["WizardOfOz"])
    if (args.conversationID !== undefined) {
      request.setAgentRequestParameters(Struct.fromJavaScript({
        conversationId: args.conversationID,
      }) as any)
    }

    return request
  }

  // noinspection JSUnusedGlobalSymbols
  private _subscribe = (args: ISubscribeArguments): ConcreteSubscription => {
    const request = this._makeInteractionRequest(args)

    const call = this.getClient().listResponses(
        request, {})

    call.on("data", args.onResponse)

    call.on("error", args.onError || ((error: grpcWeb.Error) => {
      console.error(error)
    }))

    call.on("status", args.onStatus || ((status: grpcWeb.Status) => {
      // tslint:disable-next-line:no-console
      console.debug(status)
    }))

    call.on("end", args.onEnd || (() => {
      // tslint:disable-next-line:no-console
      console.debug("stream closed connection")
    }))

    return new ConcreteSubscription({request: args, call, client: this})
  }

  private getClient = (): AgentDialogueClient => {
    if (this._client !== undefined) { return this._client }
    // noinspection SpellCheckingInspection
    return this._client = new AgentDialogueClient(
        this._hostURL, null)
  }


  public remove = (sub: ConcreteSubscription) => {
    const index = this._subscriptions.indexOf(sub)
    if (index < 0) { return }
    this._subscriptions.splice(index, 1)
  }

  // noinspection JSUnusedGlobalSymbols
  public send = (message: IMessage, options?: ISendOptions) => {
    const userID = message.userID
    if (userID === undefined) { return }

    // Create the interaction request by also setting the message type
    const request = this._makeInteractionRequest(
        { ...(options || {}), ...message, userID, messageType: message.messageType})
    // console.log("request: ", request)

    // noinspection JSUnusedLocalSymbols
    this.getClient().getResponseFromAgents(
        request, {},
        (_err: grpcWeb.Error,
         _response: InteractionResponse) => {
          // console.log("echo", _response)
          message.id = _response.asTextResponse().responseID
        })
  }

  // noinspection JSUnusedGlobalSymbols
  public subscribe = (args: ISubscribeArguments): ISubscription => {
    const sub = this._subscribe(args)
    this._subscriptions.push(sub)
    return sub
  }

  // noinspection JSUnusedGlobalSymbols
  public terminate = () => {
    this._subscriptions.forEach((sub) => {sub.call.cancel()})
    this._subscriptions = []
  }

  // Method used in order to perform a generic call to the SearchAPI 
  public agentInteractionApi = async (requestBody: Struct, agent: string): Promise<{ [key: string]: JavaScriptValue; }> => {

    // Return a new promise
    return new Promise((resolve) => {

      const input = new InputInteraction();

      // Populate the InteractionRequest object
      const request = new InteractionRequest()
      request.setClientId(ClientId.WEB_SIMULATOR)
      request.setInteraction(input)

      // Specify that the request is for the SearchAPI agent
      request.setChosenAgentsList([agent])
      request.setAgentRequestParameters(requestBody);

      var response: { [key: string]: JavaScriptValue; };
      var callback = (_err: grpcWeb.Error,
        _response: InteractionResponse,) => {

        // If the response is successfull        
        if (_err === null || _err.code === 0) {
          if (_response !== undefined
            && _response.getMessageStatus() === InteractionResponse.ClientMessageStatus.SUCCESSFUL
            && _response.getInteractionList().length !== 0) {

            // Retrieve the response object and resolve the promise
            let responseObj = _response.getInteractionList()[0].getUnstructuredResult();
            if (responseObj !== undefined) {
              let responseStruct: { [key: string]: JavaScriptValue; } = responseObj.toJavaScript();
              resolve(responseStruct);
              return;
            }
          }
        }

        resolve(response);

      }

      // Perform the call 
      this.getClient().getResponseFromAgents(
        request, {},
        callback)

    });


  }
}
