import * as jspb from 'google-protobuf'

import * as google_protobuf_timestamp_pb from 'google-protobuf/google/protobuf/timestamp_pb';
import * as google_protobuf_struct_pb from 'google-protobuf/google/protobuf/struct_pb';


export class ClientConversation extends jspb.Message {
  getTurnList(): Array<ClientTurn>;
  setTurnList(value: Array<ClientTurn>): ClientConversation;
  clearTurnList(): ClientConversation;
  addTurn(value?: ClientTurn, index?: number): ClientTurn;

  serializeBinary(): Uint8Array;
  toObject(includeInstance?: boolean): ClientConversation.AsObject;
  static toObject(includeInstance: boolean, msg: ClientConversation): ClientConversation.AsObject;
  static serializeBinaryToWriter(message: ClientConversation, writer: jspb.BinaryWriter): void;
  static deserializeBinary(bytes: Uint8Array): ClientConversation;
  static deserializeBinaryFromReader(message: ClientConversation, reader: jspb.BinaryReader): ClientConversation;
}

export namespace ClientConversation {
  export type AsObject = {
    turnList: Array<ClientTurn.AsObject>,
  }
}

export class ClientTurn extends jspb.Message {
  getInteractionRequest(): InteractionRequest | undefined;
  setInteractionRequest(value?: InteractionRequest): ClientTurn;
  hasInteractionRequest(): boolean;
  clearInteractionRequest(): ClientTurn;

  getInteractionResponse(): InteractionResponse | undefined;
  setInteractionResponse(value?: InteractionResponse): ClientTurn;
  hasInteractionResponse(): boolean;
  clearInteractionResponse(): ClientTurn;

  serializeBinary(): Uint8Array;
  toObject(includeInstance?: boolean): ClientTurn.AsObject;
  static toObject(includeInstance: boolean, msg: ClientTurn): ClientTurn.AsObject;
  static serializeBinaryToWriter(message: ClientTurn, writer: jspb.BinaryWriter): void;
  static deserializeBinary(bytes: Uint8Array): ClientTurn;
  static deserializeBinaryFromReader(message: ClientTurn, reader: jspb.BinaryReader): ClientTurn;
}

export namespace ClientTurn {
  export type AsObject = {
    interactionRequest?: InteractionRequest.AsObject,
    interactionResponse?: InteractionResponse.AsObject,
  }
}

export class InteractionRequest extends jspb.Message {
  getTime(): google_protobuf_timestamp_pb.Timestamp | undefined;
  setTime(value?: google_protobuf_timestamp_pb.Timestamp): InteractionRequest;
  hasTime(): boolean;
  clearTime(): InteractionRequest;

  getClientId(): ClientId;
  setClientId(value: ClientId): InteractionRequest;

  getInteraction(): InputInteraction | undefined;
  setInteraction(value?: InputInteraction): InteractionRequest;
  hasInteraction(): boolean;
  clearInteraction(): InteractionRequest;

  getUserId(): string;
  setUserId(value: string): InteractionRequest;

  getAgentRequestParameters(): google_protobuf_struct_pb.Struct | undefined;
  setAgentRequestParameters(value?: google_protobuf_struct_pb.Struct): InteractionRequest;
  hasAgentRequestParameters(): boolean;
  clearAgentRequestParameters(): InteractionRequest;

  getChosenAgentsList(): Array<string>;
  setChosenAgentsList(value: Array<string>): InteractionRequest;
  clearChosenAgentsList(): InteractionRequest;
  addChosenAgents(value: string, index?: number): InteractionRequest;

  serializeBinary(): Uint8Array;
  toObject(includeInstance?: boolean): InteractionRequest.AsObject;
  static toObject(includeInstance: boolean, msg: InteractionRequest): InteractionRequest.AsObject;
  static serializeBinaryToWriter(message: InteractionRequest, writer: jspb.BinaryWriter): void;
  static deserializeBinary(bytes: Uint8Array): InteractionRequest;
  static deserializeBinaryFromReader(message: InteractionRequest, reader: jspb.BinaryReader): InteractionRequest;
}

export namespace InteractionRequest {
  export type AsObject = {
    time?: google_protobuf_timestamp_pb.Timestamp.AsObject,
    clientId: ClientId,
    interaction?: InputInteraction.AsObject,
    userId: string,
    agentRequestParameters?: google_protobuf_struct_pb.Struct.AsObject,
    chosenAgentsList: Array<string>,
  }
}

export class InteractionResponse extends jspb.Message {
  getResponseId(): string;
  setResponseId(value: string): InteractionResponse;

  getTime(): google_protobuf_timestamp_pb.Timestamp | undefined;
  setTime(value?: google_protobuf_timestamp_pb.Timestamp): InteractionResponse;
  hasTime(): boolean;
  clearTime(): InteractionResponse;

  getClientId(): ClientId;
  setClientId(value: ClientId): InteractionResponse;

  getInteractionList(): Array<OutputInteraction>;
  setInteractionList(value: Array<OutputInteraction>): InteractionResponse;
  clearInteractionList(): InteractionResponse;
  addInteraction(value?: OutputInteraction, index?: number): OutputInteraction;

  getMessageStatus(): InteractionResponse.ClientMessageStatus;
  setMessageStatus(value: InteractionResponse.ClientMessageStatus): InteractionResponse;

  getErrorMessage(): string;
  setErrorMessage(value: string): InteractionResponse;

  getUserId(): string;
  setUserId(value: string): InteractionResponse;

  getSessionId(): string;
  setSessionId(value: string): InteractionResponse;

  serializeBinary(): Uint8Array;
  toObject(includeInstance?: boolean): InteractionResponse.AsObject;
  static toObject(includeInstance: boolean, msg: InteractionResponse): InteractionResponse.AsObject;
  static serializeBinaryToWriter(message: InteractionResponse, writer: jspb.BinaryWriter): void;
  static deserializeBinary(bytes: Uint8Array): InteractionResponse;
  static deserializeBinaryFromReader(message: InteractionResponse, reader: jspb.BinaryReader): InteractionResponse;
}

export namespace InteractionResponse {
  export type AsObject = {
    responseId: string,
    time?: google_protobuf_timestamp_pb.Timestamp.AsObject,
    clientId: ClientId,
    interactionList: Array<OutputInteraction.AsObject>,
    messageStatus: InteractionResponse.ClientMessageStatus,
    errorMessage: string,
    userId: string,
    sessionId: string,
  }

  export enum ClientMessageStatus { 
    NONSET = 0,
    SUCCESSFUL = 1,
    ERROR = 2,
  }
}

export class InputInteraction extends jspb.Message {
  getText(): string;
  setText(value: string): InputInteraction;

  getAudioBytes(): string;
  setAudioBytes(value: string): InputInteraction;

  getType(): InteractionType;
  setType(value: InteractionType): InputInteraction;

  getActionTypeList(): Array<InteractionAction>;
  setActionTypeList(value: Array<InteractionAction>): InputInteraction;
  clearActionTypeList(): InputInteraction;
  addActionType(value: InteractionAction, index?: number): InputInteraction;

  getDeviceType(): string;
  setDeviceType(value: string): InputInteraction;

  getLanguageCode(): string;
  setLanguageCode(value: string): InputInteraction;

  getInteractionLogs(): InteractionLogs | undefined;
  setInteractionLogs(value?: InteractionLogs): InputInteraction;
  hasInteractionLogs(): boolean;
  clearInteractionLogs(): InputInteraction;

  getAudioBase64(): string;
  setAudioBase64(value: string): InputInteraction;

  serializeBinary(): Uint8Array;
  toObject(includeInstance?: boolean): InputInteraction.AsObject;
  static toObject(includeInstance: boolean, msg: InputInteraction): InputInteraction.AsObject;
  static serializeBinaryToWriter(message: InputInteraction, writer: jspb.BinaryWriter): void;
  static deserializeBinary(bytes: Uint8Array): InputInteraction;
  static deserializeBinaryFromReader(message: InputInteraction, reader: jspb.BinaryReader): InputInteraction;
}

export namespace InputInteraction {
  export type AsObject = {
    text: string,
    audioBytes: string,
    type: InteractionType,
    actionTypeList: Array<InteractionAction>,
    deviceType: string,
    languageCode: string,
    interactionLogs?: InteractionLogs.AsObject,
    audioBase64: string,
  }
}

export class OutputInteraction extends jspb.Message {
  getText(): string;
  setText(value: string): OutputInteraction;

  getAudioBytes(): string;
  setAudioBytes(value: string): OutputInteraction;

  getActionTypeList(): Array<InteractionAction>;
  setActionTypeList(value: Array<InteractionAction>): OutputInteraction;
  clearActionTypeList(): OutputInteraction;
  addActionType(value: InteractionAction, index?: number): OutputInteraction;

  getType(): InteractionType;
  setType(value: InteractionType): OutputInteraction;

  getResultList(): Array<Result>;
  setResultList(value: Array<Result>): OutputInteraction;
  clearResultList(): OutputInteraction;
  addResult(value?: Result, index?: number): Result;

  getUnstructuredResult(): google_protobuf_struct_pb.Struct | undefined;
  setUnstructuredResult(value?: google_protobuf_struct_pb.Struct): OutputInteraction;
  hasUnstructuredResult(): boolean;
  clearUnstructuredResult(): OutputInteraction;

  getInteractionTime(): google_protobuf_timestamp_pb.Timestamp | undefined;
  setInteractionTime(value?: google_protobuf_timestamp_pb.Timestamp): OutputInteraction;
  hasInteractionTime(): boolean;
  clearInteractionTime(): OutputInteraction;

  serializeBinary(): Uint8Array;
  toObject(includeInstance?: boolean): OutputInteraction.AsObject;
  static toObject(includeInstance: boolean, msg: OutputInteraction): OutputInteraction.AsObject;
  static serializeBinaryToWriter(message: OutputInteraction, writer: jspb.BinaryWriter): void;
  static deserializeBinary(bytes: Uint8Array): OutputInteraction;
  static deserializeBinaryFromReader(message: OutputInteraction, reader: jspb.BinaryReader): OutputInteraction;
}

export namespace OutputInteraction {
  export type AsObject = {
    text: string,
    audioBytes: string,
    actionTypeList: Array<InteractionAction>,
    type: InteractionType,
    resultList: Array<Result.AsObject>,
    unstructuredResult?: google_protobuf_struct_pb.Struct.AsObject,
    interactionTime?: google_protobuf_timestamp_pb.Timestamp.AsObject,
  }
}

export class Result extends jspb.Message {
  getId(): string;
  setId(value: string): Result;

  getScore(): number;
  setScore(value: number): Result;

  getRank(): number;
  setRank(value: number): Result;

  getTitle(): string;
  setTitle(value: string): Result;

  getShortDescription(): string;
  setShortDescription(value: string): Result;

  getFullText(): string;
  setFullText(value: string): Result;

  serializeBinary(): Uint8Array;
  toObject(includeInstance?: boolean): Result.AsObject;
  static toObject(includeInstance: boolean, msg: Result): Result.AsObject;
  static serializeBinaryToWriter(message: Result, writer: jspb.BinaryWriter): void;
  static deserializeBinary(bytes: Uint8Array): Result;
  static deserializeBinaryFromReader(message: Result, reader: jspb.BinaryReader): Result;
}

export namespace Result {
  export type AsObject = {
    id: string,
    score: number,
    rank: number,
    title: string,
    shortDescription: string,
    fullText: string,
  }
}

export class InteractionLogs extends jspb.Message {
  getSearchQueriesList(): Array<InteractionLogs.SearchQuery>;
  setSearchQueriesList(value: Array<InteractionLogs.SearchQuery>): InteractionLogs;
  clearSearchQueriesList(): InteractionLogs;
  addSearchQueries(value?: InteractionLogs.SearchQuery, index?: number): InteractionLogs.SearchQuery;

  getInteractionSourcesList(): Array<InteractionLogs.InteractionSource>;
  setInteractionSourcesList(value: Array<InteractionLogs.InteractionSource>): InteractionLogs;
  clearInteractionSourcesList(): InteractionLogs;
  addInteractionSources(value?: InteractionLogs.InteractionSource, index?: number): InteractionLogs.InteractionSource;

  getUserInteractionSelectionsList(): Array<InteractionLogs.UserInteractionSelection>;
  setUserInteractionSelectionsList(value: Array<InteractionLogs.UserInteractionSelection>): InteractionLogs;
  clearUserInteractionSelectionsList(): InteractionLogs;
  addUserInteractionSelections(value?: InteractionLogs.UserInteractionSelection, index?: number): InteractionLogs.UserInteractionSelection;

  serializeBinary(): Uint8Array;
  toObject(includeInstance?: boolean): InteractionLogs.AsObject;
  static toObject(includeInstance: boolean, msg: InteractionLogs): InteractionLogs.AsObject;
  static serializeBinaryToWriter(message: InteractionLogs, writer: jspb.BinaryWriter): void;
  static deserializeBinary(bytes: Uint8Array): InteractionLogs;
  static deserializeBinaryFromReader(message: InteractionLogs, reader: jspb.BinaryReader): InteractionLogs;
}

export namespace InteractionLogs {
  export type AsObject = {
    searchQueriesList: Array<InteractionLogs.SearchQuery.AsObject>,
    interactionSourcesList: Array<InteractionLogs.InteractionSource.AsObject>,
    userInteractionSelectionsList: Array<InteractionLogs.UserInteractionSelection.AsObject>,
  }

  export class SearchQuery extends jspb.Message {
    getQuery(): string;
    setQuery(value: string): SearchQuery;

    getEventTimestamp(): number;
    setEventTimestamp(value: number): SearchQuery;

    serializeBinary(): Uint8Array;
    toObject(includeInstance?: boolean): SearchQuery.AsObject;
    static toObject(includeInstance: boolean, msg: SearchQuery): SearchQuery.AsObject;
    static serializeBinaryToWriter(message: SearchQuery, writer: jspb.BinaryWriter): void;
    static deserializeBinary(bytes: Uint8Array): SearchQuery;
    static deserializeBinaryFromReader(message: SearchQuery, reader: jspb.BinaryReader): SearchQuery;
  }

  export namespace SearchQuery {
    export type AsObject = {
      query: string,
      eventTimestamp: number,
    }
  }


  export class InteractionSource extends jspb.Message {
    getPageId(): string;
    setPageId(value: string): InteractionSource;

    getPageOrigin(): string;
    setPageOrigin(value: string): InteractionSource;

    getPageTitle(): string;
    setPageTitle(value: string): InteractionSource;

    getSectionTitle(): string;
    setSectionTitle(value: string): InteractionSource;

    getParagraphId(): string;
    setParagraphId(value: string): InteractionSource;

    getParagraphText(): string;
    setParagraphText(value: string): InteractionSource;

    getEventTimestamp(): number;
    setEventTimestamp(value: number): InteractionSource;

    serializeBinary(): Uint8Array;
    toObject(includeInstance?: boolean): InteractionSource.AsObject;
    static toObject(includeInstance: boolean, msg: InteractionSource): InteractionSource.AsObject;
    static serializeBinaryToWriter(message: InteractionSource, writer: jspb.BinaryWriter): void;
    static deserializeBinary(bytes: Uint8Array): InteractionSource;
    static deserializeBinaryFromReader(message: InteractionSource, reader: jspb.BinaryReader): InteractionSource;
  }

  export namespace InteractionSource {
    export type AsObject = {
      pageId: string,
      pageOrigin: string,
      pageTitle: string,
      sectionTitle: string,
      paragraphId: string,
      paragraphText: string,
      eventTimestamp: number,
    }
  }


  export class UserInteractionSelection extends jspb.Message {
    getPageId(): string;
    setPageId(value: string): UserInteractionSelection;

    getPageTitle(): string;
    setPageTitle(value: string): UserInteractionSelection;

    getSectionTitle(): string;
    setSectionTitle(value: string): UserInteractionSelection;

    getParagraphText(): string;
    setParagraphText(value: string): UserInteractionSelection;

    getEventTimestamp(): number;
    setEventTimestamp(value: number): UserInteractionSelection;

    serializeBinary(): Uint8Array;
    toObject(includeInstance?: boolean): UserInteractionSelection.AsObject;
    static toObject(includeInstance: boolean, msg: UserInteractionSelection): UserInteractionSelection.AsObject;
    static serializeBinaryToWriter(message: UserInteractionSelection, writer: jspb.BinaryWriter): void;
    static deserializeBinary(bytes: Uint8Array): UserInteractionSelection;
    static deserializeBinaryFromReader(message: UserInteractionSelection, reader: jspb.BinaryReader): UserInteractionSelection;
  }

  export namespace UserInteractionSelection {
    export type AsObject = {
      pageId: string,
      pageTitle: string,
      sectionTitle: string,
      paragraphText: string,
      eventTimestamp: number,
    }
  }

}

export enum ClientId { 
  NONSET = 0,
  EXTERNAL_APPLICATION = 1,
  LOG_REPLAYER = 2,
  WEB_SIMULATOR = 3,
}
export enum InteractionType { 
  NOTSET = 0,
  TEXT = 1,
  AUDIO = 2,
  ACTION = 3,
  STATUS = 4,
}
export enum InteractionAction { 
  NEXT_STEP = 0,
  PREVIOUS_STEP = 1,
}
