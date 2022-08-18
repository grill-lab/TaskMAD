import * as jspb from 'google-protobuf'



export class CoreConfig extends jspb.Message {
  getGrpcServerPort(): number;
  setGrpcServerPort(value: number): CoreConfig;

  getLogStoragePath(): string;
  setLogStoragePath(value: string): CoreConfig;

  getMaxNumberOfSimultaneousConversations(): number;
  setMaxNumberOfSimultaneousConversations(value: number): CoreConfig;

  getSessionTimeoutMinutes(): number;
  setSessionTimeoutMinutes(value: number): CoreConfig;

  getAgentsList(): Array<AgentConfig>;
  setAgentsList(value: Array<AgentConfig>): CoreConfig;
  clearAgentsList(): CoreConfig;
  addAgents(value?: AgentConfig, index?: number): AgentConfig;

  serializeBinary(): Uint8Array;
  toObject(includeInstance?: boolean): CoreConfig.AsObject;
  static toObject(includeInstance: boolean, msg: CoreConfig): CoreConfig.AsObject;
  static serializeBinaryToWriter(message: CoreConfig, writer: jspb.BinaryWriter): void;
  static deserializeBinary(bytes: Uint8Array): CoreConfig;
  static deserializeBinaryFromReader(message: CoreConfig, reader: jspb.BinaryReader): CoreConfig;
}

export namespace CoreConfig {
  export type AsObject = {
    grpcServerPort: number,
    logStoragePath: string,
    maxNumberOfSimultaneousConversations: number,
    sessionTimeoutMinutes: number,
    agentsList: Array<AgentConfig.AsObject>,
  }
}

export class AgentConfig extends jspb.Message {
  getServiceProvider(): ServiceProvider;
  setServiceProvider(value: ServiceProvider): AgentConfig;

  getProjectId(): string;
  setProjectId(value: string): AgentConfig;

  getConfigurationFileUrl(): string;
  setConfigurationFileUrl(value: string): AgentConfig;

  getDatabaseReference(): string;
  setDatabaseReference(value: string): AgentConfig;

  serializeBinary(): Uint8Array;
  toObject(includeInstance?: boolean): AgentConfig.AsObject;
  static toObject(includeInstance: boolean, msg: AgentConfig): AgentConfig.AsObject;
  static serializeBinaryToWriter(message: AgentConfig, writer: jspb.BinaryWriter): void;
  static deserializeBinary(bytes: Uint8Array): AgentConfig;
  static deserializeBinaryFromReader(message: AgentConfig, reader: jspb.BinaryReader): AgentConfig;
}

export namespace AgentConfig {
  export type AsObject = {
    serviceProvider: ServiceProvider,
    projectId: string,
    configurationFileUrl: string,
    databaseReference: string,
  }
}

export enum ServiceProvider { 
  UNRECOGNISED = 0,
  DIALOGFLOW = 1,
  WIZARD = 2,
  EXTERNAL_SERVICES = 3,
  SPEECH_TO_TEXT = 5,
}
