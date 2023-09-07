/**
 * @fileoverview gRPC-Web generated client stub for edu.gla.kail.ad.service
 * @enhanceable
 * @public
 */

// Code generated by protoc-gen-grpc-web. DO NOT EDIT.
// versions:
// 	protoc-gen-grpc-web v1.4.2
// 	protoc              v3.19.1
// source: service.proto


/* eslint-disable */
// @ts-nocheck


import * as grpcWeb from 'grpc-web';

import * as client_pb from './client_pb';
import * as service_pb from './service_pb';


export class AgentDialogueClient {
  client_: grpcWeb.AbstractClientBase;
  hostname_: string;
  credentials_: null | { [index: string]: string; };
  options_: null | { [index: string]: any; };

  constructor (hostname: string,
               credentials?: null | { [index: string]: string; },
               options?: null | { [index: string]: any; }) {
    if (!options) options = {};
    if (!credentials) credentials = {};
    options['format'] = 'text';

    this.client_ = new grpcWeb.GrpcWebClientBase(options);
    this.hostname_ = hostname.replace(/\/+$/, '');
    this.credentials_ = credentials;
    this.options_ = options;
  }

  methodDescriptorGetResponseFromAgents = new grpcWeb.MethodDescriptor(
    '/edu.gla.kail.ad.service.AgentDialogue/GetResponseFromAgents',
    grpcWeb.MethodType.UNARY,
    client_pb.InteractionRequest,
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
    callback: (err: grpcWeb.RpcError,
               response: client_pb.InteractionResponse) => void): grpcWeb.ClientReadableStream<client_pb.InteractionResponse>;

  getResponseFromAgents(
    request: client_pb.InteractionRequest,
    metadata: grpcWeb.Metadata | null,
    callback?: (err: grpcWeb.RpcError,
               response: client_pb.InteractionResponse) => void) {
    if (callback !== undefined) {
      return this.client_.rpcCall(
        this.hostname_ +
          '/edu.gla.kail.ad.service.AgentDialogue/GetResponseFromAgents',
        request,
        metadata || {},
        this.methodDescriptorGetResponseFromAgents,
        callback);
    }
    return this.client_.unaryCall(
    this.hostname_ +
      '/edu.gla.kail.ad.service.AgentDialogue/GetResponseFromAgents',
    request,
    metadata || {},
    this.methodDescriptorGetResponseFromAgents);
  }

  methodDescriptorListResponses = new grpcWeb.MethodDescriptor(
    '/edu.gla.kail.ad.service.AgentDialogue/ListResponses',
    grpcWeb.MethodType.SERVER_STREAMING,
    client_pb.InteractionRequest,
    client_pb.InteractionResponse,
    (request: client_pb.InteractionRequest) => {
      return request.serializeBinary();
    },
    client_pb.InteractionResponse.deserializeBinary
  );

  listResponses(
    request: client_pb.InteractionRequest,
    metadata?: grpcWeb.Metadata): grpcWeb.ClientReadableStream<client_pb.InteractionResponse> {
    return this.client_.serverStreaming(
      this.hostname_ +
        '/edu.gla.kail.ad.service.AgentDialogue/ListResponses',
      request,
      metadata || {},
      this.methodDescriptorListResponses);
  }

  methodDescriptorEndSession = new grpcWeb.MethodDescriptor(
    '/edu.gla.kail.ad.service.AgentDialogue/EndSession',
    grpcWeb.MethodType.UNARY,
    service_pb.UserID,
    service_pb.UserID,
    (request: service_pb.UserID) => {
      return request.serializeBinary();
    },
    service_pb.UserID.deserializeBinary
  );

  endSession(
    request: service_pb.UserID,
    metadata: grpcWeb.Metadata | null): Promise<service_pb.UserID>;

  endSession(
    request: service_pb.UserID,
    metadata: grpcWeb.Metadata | null,
    callback: (err: grpcWeb.RpcError,
               response: service_pb.UserID) => void): grpcWeb.ClientReadableStream<service_pb.UserID>;

  endSession(
    request: service_pb.UserID,
    metadata: grpcWeb.Metadata | null,
    callback?: (err: grpcWeb.RpcError,
               response: service_pb.UserID) => void) {
    if (callback !== undefined) {
      return this.client_.rpcCall(
        this.hostname_ +
          '/edu.gla.kail.ad.service.AgentDialogue/EndSession',
        request,
        metadata || {},
        this.methodDescriptorEndSession,
        callback);
    }
    return this.client_.unaryCall(
    this.hostname_ +
      '/edu.gla.kail.ad.service.AgentDialogue/EndSession',
    request,
    metadata || {},
    this.methodDescriptorEndSession);
  }

}

