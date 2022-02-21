import * as jspb from "google-protobuf"

import * as client_pb from './client_pb';

export class UserID extends jspb.Message {
  getUserId(): string;
  setUserId(value: string): UserID;

  getActivesession(): boolean;
  setActivesession(value: boolean): UserID;

  serializeBinary(): Uint8Array;
  toObject(includeInstance?: boolean): UserID.AsObject;
  static toObject(includeInstance: boolean, msg: UserID): UserID.AsObject;
  static serializeBinaryToWriter(message: UserID, writer: jspb.BinaryWriter): void;
  static deserializeBinary(bytes: Uint8Array): UserID;
  static deserializeBinaryFromReader(message: UserID, reader: jspb.BinaryReader): UserID;
}

export namespace UserID {
  export type AsObject = {
    userId: string,
    activesession: boolean,
  }
}

