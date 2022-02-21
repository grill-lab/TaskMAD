/**
 * @fileoverview gRPC-Web generated client stub for edu.gla.kail.ad.service
 * @enhanceable
 * @public
 */

// GENERATED CODE -- DO NOT EDIT!


/* eslint-disable */
// @ts-nocheck


import * as grpcWeb from 'grpc-web';

import * as client_pb from './client_pb';

import {UserID} from './service_pb';

export class AgentDialogueClient {
  client_: grpcWeb.AbstractClientBase;
  hostname_: string;
  credentials_: null | { [index: string]: string; };
  options_: null | { [index: string]: string; };

  constructor (hostname: string,
               credentials?: null | { [index: string]: string; },
               options?: null | { [index: string]: string; }) {
    if (!options) options = {};
    if (!credentials) credentials = {};
    options['format'] = 'text';

    this.client_ = new grpcWeb.GrpcWebClientBase(options);
    this.hostname_ = hostname;
    this.credentials_ = credentials;
    this.options_ = options;
  }

  methodInfoGetResponseFromAgents = new grpcWeb.AbstractClientBase.MethodInfo(
    client_pb.InteractionResponse,
    (request: client_pb.InteractionRequest) => {
      return request.serializeBinary();
    },
    client_pb.InteractionResponse.deserializeBinary
  );

  getResponseFromAgents(
    request: client_pb.InteractionRequest,
    metadata: grpcWeb.Metadata | null): Promise<client_pb.InteractionResponse>;

  getResponseFromAgents(
    request: client_pb.InteractionRequest,
    metadata: grpcWeb.Metadata | null,
    callback: (err: grpcWeb.Error,
               response: client_pb.InteractionResponse) => void): grpcWeb.ClientReadableStream<client_pb.InteractionResponse>;

  getResponseFromAgents(
    request: client_pb.InteractionRequest,
    metadata: grpcWeb.Metadata | null,
    callback?: (err: grpcWeb.Error,
               response: client_pb.InteractionResponse) => void) {
    if (callback !== undefined) {
      return this.client_.rpcCall(
        new URL('/edu.gla.kail.ad.service.AgentDialogue/GetResponseFromAgents', this.hostname_).toString(),
        request,
        metadata || {},
        this.methodInfoGetResponseFromAgents,
        callback);
    }
    return this.client_.unaryCall(
    this.hostname_ +
      '/edu.gla.kail.ad.service.AgentDialogue/GetResponseFromAgents',
    request,
    metadata || {},
    this.methodInfoGetResponseFromAgents);
  }

  methodInfoListResponses = new grpcWeb.AbstractClientBase.MethodInfo(
    client_pb.InteractionResponse,
    (request: client_pb.InteractionRequest) => {
      return request.serializeBinary();
    },
    client_pb.InteractionResponse.deserializeBinary
  );

  listResponses(
    request: client_pb.InteractionRequest,
    metadata?: grpcWeb.Metadata) {
    return this.client_.serverStreaming(
      new URL('/edu.gla.kail.ad.service.AgentDialogue/ListResponses', this.hostname_).toString(),
      request,
      metadata || {},
      this.methodInfoListResponses);
  }

  methodInfoEndSession = new grpcWeb.AbstractClientBase.MethodInfo(
    UserID,
    (request: UserID) => {
      return request.serializeBinary();
    },
    UserID.deserializeBinary
  );

  endSession(
    request: UserID,
    metadata: grpcWeb.Metadata | null): Promise<UserID>;

  endSession(
    request: UserID,
    metadata: grpcWeb.Metadata | null,
    callback: (err: grpcWeb.Error,
               response: UserID) => void): grpcWeb.ClientReadableStream<UserID>;

  endSession(
    request: UserID,
    metadata: grpcWeb.Metadata | null,
    callback?: (err: grpcWeb.Error,
               response: UserID) => void) {
    if (callback !== undefined) {
      return this.client_.rpcCall(
        new URL('/edu.gla.kail.ad.service.AgentDialogue/EndSession', this.hostname_).toString(),
        request,
        metadata || {},
        this.methodInfoEndSession,
        callback);
    }
    return this.client_.unaryCall(
    this.hostname_ +
      '/edu.gla.kail.ad.service.AgentDialogue/EndSession',
    request,
    metadata || {},
    this.methodInfoEndSession);
  }

}

