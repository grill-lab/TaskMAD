/**
 * @fileoverview gRPC-Web generated client stub for edu.gla.kail.ad.service
 * @enhanceable
 * @public
 */

// GENERATED CODE -- DO NOT EDIT!



const grpc = {};
grpc.web = require('grpc-web');


var client_pb = require('./client_pb.js')
const proto = {};
proto.edu = {};
proto.edu.gla = {};
proto.edu.gla.kail = {};
proto.edu.gla.kail.ad = {};
proto.edu.gla.kail.ad.service = require('./service_pb.js');

/**
 * @param {string} hostname
 * @param {?Object} credentials
 * @param {?Object} options
 * @constructor
 * @struct
 * @final
 */
proto.edu.gla.kail.ad.service.AgentDialogueClient =
    function(hostname, credentials, options) {
  if (!options) options = {};
  options['format'] = 'text';

  /**
   * @private @const {!grpc.web.GrpcWebClientBase} The client
   */
  this.client_ = new grpc.web.GrpcWebClientBase(options);

  /**
   * @private @const {string} The hostname
   */
  this.hostname_ = hostname;

  /**
   * @private @const {?Object} The credentials to be used to connect
   *    to the server
   */
  this.credentials_ = credentials;

  /**
   * @private @const {?Object} Options for the client
   */
  this.options_ = options;
};


/**
 * @param {string} hostname
 * @param {?Object} credentials
 * @param {?Object} options
 * @constructor
 * @struct
 * @final
 */
proto.edu.gla.kail.ad.service.AgentDialoguePromiseClient =
    function(hostname, credentials, options) {
  if (!options) options = {};
  options['format'] = 'text';

  /**
   * @private @const {!proto.edu.gla.kail.ad.service.AgentDialogueClient} The delegate callback based client
   */
  this.delegateClient_ = new proto.edu.gla.kail.ad.service.AgentDialogueClient(
      hostname, credentials, options);

};


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.edu.gla.kail.ad.InteractionRequest,
 *   !proto.edu.gla.kail.ad.InteractionResponse>}
 */
const methodInfo_AgentDialogue_GetResponseFromAgents = new grpc.web.AbstractClientBase.MethodInfo(
  client_pb.InteractionResponse,
  /** @param {!proto.edu.gla.kail.ad.InteractionRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  client_pb.InteractionResponse.deserializeBinary
);


/**
 * @param {!proto.edu.gla.kail.ad.InteractionRequest} request The
 *     request proto
 * @param {!Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.edu.gla.kail.ad.InteractionResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.edu.gla.kail.ad.InteractionResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.edu.gla.kail.ad.service.AgentDialogueClient.prototype.getResponseFromAgents =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/edu.gla.kail.ad.service.AgentDialogue/GetResponseFromAgents',
      request,
      metadata,
      methodInfo_AgentDialogue_GetResponseFromAgents,
      callback);
};


/**
 * @param {!proto.edu.gla.kail.ad.InteractionRequest} request The
 *     request proto
 * @param {!Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.edu.gla.kail.ad.InteractionResponse>}
 *     The XHR Node Readable Stream
 */
proto.edu.gla.kail.ad.service.AgentDialoguePromiseClient.prototype.getResponseFromAgents =
    function(request, metadata) {
  return new Promise((resolve, reject) => {
    this.delegateClient_.getResponseFromAgents(
      request, metadata, (error, response) => {
        error ? reject(error) : resolve(response);
      });
  });
};


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.edu.gla.kail.ad.InteractionRequest,
 *   !proto.edu.gla.kail.ad.InteractionResponse>}
 */
const methodInfo_AgentDialogue_ListResponses = new grpc.web.AbstractClientBase.MethodInfo(
  client_pb.InteractionResponse,
  /** @param {!proto.edu.gla.kail.ad.InteractionRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  client_pb.InteractionResponse.deserializeBinary
);


/**
 * @param {!proto.edu.gla.kail.ad.InteractionRequest} request The request proto
 * @param {!Object<string, string>} metadata User defined
 *     call metadata
 * @return {!grpc.web.ClientReadableStream<!proto.edu.gla.kail.ad.InteractionResponse>}
 *     The XHR Node Readable Stream
 */
proto.edu.gla.kail.ad.service.AgentDialogueClient.prototype.listResponses =
    function(request, metadata) {
  return this.client_.serverStreaming(this.hostname_ +
      '/edu.gla.kail.ad.service.AgentDialogue/ListResponses',
      request,
      metadata,
      methodInfo_AgentDialogue_ListResponses);
};


/**
 * @param {!proto.edu.gla.kail.ad.InteractionRequest} request The request proto
 * @param {!Object<string, string>} metadata User defined
 *     call metadata
 * @return {!grpc.web.ClientReadableStream<!proto.edu.gla.kail.ad.InteractionResponse>}
 *     The XHR Node Readable Stream
 */
proto.edu.gla.kail.ad.service.AgentDialoguePromiseClient.prototype.listResponses =
    function(request, metadata) {
  return this.delegateClient_.client_.serverStreaming(this.delegateClient_.hostname_ +
      '/edu.gla.kail.ad.service.AgentDialogue/ListResponses',
      request,
      metadata,
      methodInfo_AgentDialogue_ListResponses);
};


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.edu.gla.kail.ad.service.UserID,
 *   !proto.edu.gla.kail.ad.service.UserID>}
 */
const methodInfo_AgentDialogue_EndSession = new grpc.web.AbstractClientBase.MethodInfo(
  proto.edu.gla.kail.ad.service.UserID,
  /** @param {!proto.edu.gla.kail.ad.service.UserID} request */
  function(request) {
    return request.serializeBinary();
  },
  proto.edu.gla.kail.ad.service.UserID.deserializeBinary
);


/**
 * @param {!proto.edu.gla.kail.ad.service.UserID} request The
 *     request proto
 * @param {!Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.edu.gla.kail.ad.service.UserID)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.edu.gla.kail.ad.service.UserID>|undefined}
 *     The XHR Node Readable Stream
 */
proto.edu.gla.kail.ad.service.AgentDialogueClient.prototype.endSession =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/edu.gla.kail.ad.service.AgentDialogue/EndSession',
      request,
      metadata,
      methodInfo_AgentDialogue_EndSession,
      callback);
};


/**
 * @param {!proto.edu.gla.kail.ad.service.UserID} request The
 *     request proto
 * @param {!Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.edu.gla.kail.ad.service.UserID>}
 *     The XHR Node Readable Stream
 */
proto.edu.gla.kail.ad.service.AgentDialoguePromiseClient.prototype.endSession =
    function(request, metadata) {
  return new Promise((resolve, reject) => {
    this.delegateClient_.endSession(
      request, metadata, (error, response) => {
        error ? reject(error) : resolve(response);
      });
  });
};


module.exports = proto.edu.gla.kail.ad.service;

