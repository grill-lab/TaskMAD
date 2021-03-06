/**
 * @fileoverview
 * @enhanceable
 * @suppress {messageConventions} JS Compiler reports an error if a variable or
 *     field starts with 'MSG_' and isn't a translatable message.
 * @public
 */
// GENERATED CODE -- DO NOT EDIT!

var jspb = require('google-protobuf');
var goog = jspb;
var global = Function('return this')();

goog.exportSymbol('proto.edu.gla.kail.ad.AgentConfig', null, global);
goog.exportSymbol('proto.edu.gla.kail.ad.CoreConfig', null, global);
goog.exportSymbol('proto.edu.gla.kail.ad.ServiceProvider', null, global);

/**
 * Generated by JsPbCodeGenerator.
 * @param {Array=} opt_data Optional initial data array, typically from a
 * server response, or constructed directly in Javascript. The array is used
 * in place and becomes part of the constructed object. It is not cloned.
 * If no data is provided, the constructed object will be empty, but still
 * valid.
 * @extends {jspb.Message}
 * @constructor
 */
proto.edu.gla.kail.ad.CoreConfig = function(opt_data) {
  jspb.Message.initialize(this, opt_data, 0, -1, proto.edu.gla.kail.ad.CoreConfig.repeatedFields_, null);
};
goog.inherits(proto.edu.gla.kail.ad.CoreConfig, jspb.Message);
if (goog.DEBUG && !COMPILED) {
  proto.edu.gla.kail.ad.CoreConfig.displayName = 'proto.edu.gla.kail.ad.CoreConfig';
}
/**
 * List of repeated fields within this message type.
 * @private {!Array<number>}
 * @const
 */
proto.edu.gla.kail.ad.CoreConfig.repeatedFields_ = [5];



if (jspb.Message.GENERATE_TO_OBJECT) {
/**
 * Creates an object representation of this proto suitable for use in Soy templates.
 * Field names that are reserved in JavaScript and will be renamed to pb_name.
 * To access a reserved field use, foo.pb_<name>, eg, foo.pb_default.
 * For the list of reserved names please see:
 *     com.google.apps.jspb.JsClassTemplate.JS_RESERVED_WORDS.
 * @param {boolean=} opt_includeInstance Whether to include the JSPB instance
 *     for transitional soy proto support: http://goto/soy-param-migration
 * @return {!Object}
 */
proto.edu.gla.kail.ad.CoreConfig.prototype.toObject = function(opt_includeInstance) {
  return proto.edu.gla.kail.ad.CoreConfig.toObject(opt_includeInstance, this);
};


/**
 * Static version of the {@see toObject} method.
 * @param {boolean|undefined} includeInstance Whether to include the JSPB
 *     instance for transitional soy proto support:
 *     http://goto/soy-param-migration
 * @param {!proto.edu.gla.kail.ad.CoreConfig} msg The msg instance to transform.
 * @return {!Object}
 * @suppress {unusedLocalVariables} f is only used for nested messages
 */
proto.edu.gla.kail.ad.CoreConfig.toObject = function(includeInstance, msg) {
  var f, obj = {
    grpcServerPort: jspb.Message.getFieldWithDefault(msg, 1, 0),
    logStoragePath: jspb.Message.getFieldWithDefault(msg, 2, ""),
    maxNumberOfSimultaneousConversations: jspb.Message.getFieldWithDefault(msg, 3, 0),
    sessionTimeoutMinutes: jspb.Message.getFieldWithDefault(msg, 4, 0),
    agentsList: jspb.Message.toObjectList(msg.getAgentsList(),
    proto.edu.gla.kail.ad.AgentConfig.toObject, includeInstance)
  };

  if (includeInstance) {
    obj.$jspbMessageInstance = msg;
  }
  return obj;
};
}


/**
 * Deserializes binary data (in protobuf wire format).
 * @param {jspb.ByteSource} bytes The bytes to deserialize.
 * @return {!proto.edu.gla.kail.ad.CoreConfig}
 */
proto.edu.gla.kail.ad.CoreConfig.deserializeBinary = function(bytes) {
  var reader = new jspb.BinaryReader(bytes);
  var msg = new proto.edu.gla.kail.ad.CoreConfig;
  return proto.edu.gla.kail.ad.CoreConfig.deserializeBinaryFromReader(msg, reader);
};


/**
 * Deserializes binary data (in protobuf wire format) from the
 * given reader into the given message object.
 * @param {!proto.edu.gla.kail.ad.CoreConfig} msg The message object to deserialize into.
 * @param {!jspb.BinaryReader} reader The BinaryReader to use.
 * @return {!proto.edu.gla.kail.ad.CoreConfig}
 */
proto.edu.gla.kail.ad.CoreConfig.deserializeBinaryFromReader = function(msg, reader) {
  while (reader.nextField()) {
    if (reader.isEndGroup()) {
      break;
    }
    var field = reader.getFieldNumber();
    switch (field) {
    case 1:
      var value = /** @type {number} */ (reader.readInt32());
      msg.setGrpcServerPort(value);
      break;
    case 2:
      var value = /** @type {string} */ (reader.readString());
      msg.setLogStoragePath(value);
      break;
    case 3:
      var value = /** @type {number} */ (reader.readInt32());
      msg.setMaxNumberOfSimultaneousConversations(value);
      break;
    case 4:
      var value = /** @type {number} */ (reader.readInt32());
      msg.setSessionTimeoutMinutes(value);
      break;
    case 5:
      var value = new proto.edu.gla.kail.ad.AgentConfig;
      reader.readMessage(value,proto.edu.gla.kail.ad.AgentConfig.deserializeBinaryFromReader);
      msg.addAgents(value);
      break;
    default:
      reader.skipField();
      break;
    }
  }
  return msg;
};


/**
 * Serializes the message to binary data (in protobuf wire format).
 * @return {!Uint8Array}
 */
proto.edu.gla.kail.ad.CoreConfig.prototype.serializeBinary = function() {
  var writer = new jspb.BinaryWriter();
  proto.edu.gla.kail.ad.CoreConfig.serializeBinaryToWriter(this, writer);
  return writer.getResultBuffer();
};


/**
 * Serializes the given message to binary data (in protobuf wire
 * format), writing to the given BinaryWriter.
 * @param {!proto.edu.gla.kail.ad.CoreConfig} message
 * @param {!jspb.BinaryWriter} writer
 * @suppress {unusedLocalVariables} f is only used for nested messages
 */
proto.edu.gla.kail.ad.CoreConfig.serializeBinaryToWriter = function(message, writer) {
  var f = undefined;
  f = message.getGrpcServerPort();
  if (f !== 0) {
    writer.writeInt32(
      1,
      f
    );
  }
  f = message.getLogStoragePath();
  if (f.length > 0) {
    writer.writeString(
      2,
      f
    );
  }
  f = message.getMaxNumberOfSimultaneousConversations();
  if (f !== 0) {
    writer.writeInt32(
      3,
      f
    );
  }
  f = message.getSessionTimeoutMinutes();
  if (f !== 0) {
    writer.writeInt32(
      4,
      f
    );
  }
  f = message.getAgentsList();
  if (f.length > 0) {
    writer.writeRepeatedMessage(
      5,
      f,
      proto.edu.gla.kail.ad.AgentConfig.serializeBinaryToWriter
    );
  }
};


/**
 * optional int32 grpc_server_port = 1;
 * @return {number}
 */
proto.edu.gla.kail.ad.CoreConfig.prototype.getGrpcServerPort = function() {
  return /** @type {number} */ (jspb.Message.getFieldWithDefault(this, 1, 0));
};


/** @param {number} value */
proto.edu.gla.kail.ad.CoreConfig.prototype.setGrpcServerPort = function(value) {
  jspb.Message.setProto3IntField(this, 1, value);
};


/**
 * optional string log_storage_path = 2;
 * @return {string}
 */
proto.edu.gla.kail.ad.CoreConfig.prototype.getLogStoragePath = function() {
  return /** @type {string} */ (jspb.Message.getFieldWithDefault(this, 2, ""));
};


/** @param {string} value */
proto.edu.gla.kail.ad.CoreConfig.prototype.setLogStoragePath = function(value) {
  jspb.Message.setProto3StringField(this, 2, value);
};


/**
 * optional int32 max_number_of_simultaneous_conversations = 3;
 * @return {number}
 */
proto.edu.gla.kail.ad.CoreConfig.prototype.getMaxNumberOfSimultaneousConversations = function() {
  return /** @type {number} */ (jspb.Message.getFieldWithDefault(this, 3, 0));
};


/** @param {number} value */
proto.edu.gla.kail.ad.CoreConfig.prototype.setMaxNumberOfSimultaneousConversations = function(value) {
  jspb.Message.setProto3IntField(this, 3, value);
};


/**
 * optional int32 session_timeout_minutes = 4;
 * @return {number}
 */
proto.edu.gla.kail.ad.CoreConfig.prototype.getSessionTimeoutMinutes = function() {
  return /** @type {number} */ (jspb.Message.getFieldWithDefault(this, 4, 0));
};


/** @param {number} value */
proto.edu.gla.kail.ad.CoreConfig.prototype.setSessionTimeoutMinutes = function(value) {
  jspb.Message.setProto3IntField(this, 4, value);
};


/**
 * repeated AgentConfig agents = 5;
 * @return {!Array<!proto.edu.gla.kail.ad.AgentConfig>}
 */
proto.edu.gla.kail.ad.CoreConfig.prototype.getAgentsList = function() {
  return /** @type{!Array<!proto.edu.gla.kail.ad.AgentConfig>} */ (
    jspb.Message.getRepeatedWrapperField(this, proto.edu.gla.kail.ad.AgentConfig, 5));
};


/** @param {!Array<!proto.edu.gla.kail.ad.AgentConfig>} value */
proto.edu.gla.kail.ad.CoreConfig.prototype.setAgentsList = function(value) {
  jspb.Message.setRepeatedWrapperField(this, 5, value);
};


/**
 * @param {!proto.edu.gla.kail.ad.AgentConfig=} opt_value
 * @param {number=} opt_index
 * @return {!proto.edu.gla.kail.ad.AgentConfig}
 */
proto.edu.gla.kail.ad.CoreConfig.prototype.addAgents = function(opt_value, opt_index) {
  return jspb.Message.addToRepeatedWrapperField(this, 5, opt_value, proto.edu.gla.kail.ad.AgentConfig, opt_index);
};


proto.edu.gla.kail.ad.CoreConfig.prototype.clearAgentsList = function() {
  this.setAgentsList([]);
};



/**
 * Generated by JsPbCodeGenerator.
 * @param {Array=} opt_data Optional initial data array, typically from a
 * server response, or constructed directly in Javascript. The array is used
 * in place and becomes part of the constructed object. It is not cloned.
 * If no data is provided, the constructed object will be empty, but still
 * valid.
 * @extends {jspb.Message}
 * @constructor
 */
proto.edu.gla.kail.ad.AgentConfig = function(opt_data) {
  jspb.Message.initialize(this, opt_data, 0, -1, null, null);
};
goog.inherits(proto.edu.gla.kail.ad.AgentConfig, jspb.Message);
if (goog.DEBUG && !COMPILED) {
  proto.edu.gla.kail.ad.AgentConfig.displayName = 'proto.edu.gla.kail.ad.AgentConfig';
}


if (jspb.Message.GENERATE_TO_OBJECT) {
/**
 * Creates an object representation of this proto suitable for use in Soy templates.
 * Field names that are reserved in JavaScript and will be renamed to pb_name.
 * To access a reserved field use, foo.pb_<name>, eg, foo.pb_default.
 * For the list of reserved names please see:
 *     com.google.apps.jspb.JsClassTemplate.JS_RESERVED_WORDS.
 * @param {boolean=} opt_includeInstance Whether to include the JSPB instance
 *     for transitional soy proto support: http://goto/soy-param-migration
 * @return {!Object}
 */
proto.edu.gla.kail.ad.AgentConfig.prototype.toObject = function(opt_includeInstance) {
  return proto.edu.gla.kail.ad.AgentConfig.toObject(opt_includeInstance, this);
};


/**
 * Static version of the {@see toObject} method.
 * @param {boolean|undefined} includeInstance Whether to include the JSPB
 *     instance for transitional soy proto support:
 *     http://goto/soy-param-migration
 * @param {!proto.edu.gla.kail.ad.AgentConfig} msg The msg instance to transform.
 * @return {!Object}
 * @suppress {unusedLocalVariables} f is only used for nested messages
 */
proto.edu.gla.kail.ad.AgentConfig.toObject = function(includeInstance, msg) {
  var f, obj = {
    serviceProvider: jspb.Message.getFieldWithDefault(msg, 1, 0),
    projectId: jspb.Message.getFieldWithDefault(msg, 2, ""),
    configurationFileUrl: jspb.Message.getFieldWithDefault(msg, 3, "")
  };

  if (includeInstance) {
    obj.$jspbMessageInstance = msg;
  }
  return obj;
};
}


/**
 * Deserializes binary data (in protobuf wire format).
 * @param {jspb.ByteSource} bytes The bytes to deserialize.
 * @return {!proto.edu.gla.kail.ad.AgentConfig}
 */
proto.edu.gla.kail.ad.AgentConfig.deserializeBinary = function(bytes) {
  var reader = new jspb.BinaryReader(bytes);
  var msg = new proto.edu.gla.kail.ad.AgentConfig;
  return proto.edu.gla.kail.ad.AgentConfig.deserializeBinaryFromReader(msg, reader);
};


/**
 * Deserializes binary data (in protobuf wire format) from the
 * given reader into the given message object.
 * @param {!proto.edu.gla.kail.ad.AgentConfig} msg The message object to deserialize into.
 * @param {!jspb.BinaryReader} reader The BinaryReader to use.
 * @return {!proto.edu.gla.kail.ad.AgentConfig}
 */
proto.edu.gla.kail.ad.AgentConfig.deserializeBinaryFromReader = function(msg, reader) {
  while (reader.nextField()) {
    if (reader.isEndGroup()) {
      break;
    }
    var field = reader.getFieldNumber();
    switch (field) {
    case 1:
      var value = /** @type {!proto.edu.gla.kail.ad.ServiceProvider} */ (reader.readEnum());
      msg.setServiceProvider(value);
      break;
    case 2:
      var value = /** @type {string} */ (reader.readString());
      msg.setProjectId(value);
      break;
    case 3:
      var value = /** @type {string} */ (reader.readString());
      msg.setConfigurationFileUrl(value);
      break;
    default:
      reader.skipField();
      break;
    }
  }
  return msg;
};


/**
 * Serializes the message to binary data (in protobuf wire format).
 * @return {!Uint8Array}
 */
proto.edu.gla.kail.ad.AgentConfig.prototype.serializeBinary = function() {
  var writer = new jspb.BinaryWriter();
  proto.edu.gla.kail.ad.AgentConfig.serializeBinaryToWriter(this, writer);
  return writer.getResultBuffer();
};


/**
 * Serializes the given message to binary data (in protobuf wire
 * format), writing to the given BinaryWriter.
 * @param {!proto.edu.gla.kail.ad.AgentConfig} message
 * @param {!jspb.BinaryWriter} writer
 * @suppress {unusedLocalVariables} f is only used for nested messages
 */
proto.edu.gla.kail.ad.AgentConfig.serializeBinaryToWriter = function(message, writer) {
  var f = undefined;
  f = message.getServiceProvider();
  if (f !== 0.0) {
    writer.writeEnum(
      1,
      f
    );
  }
  f = message.getProjectId();
  if (f.length > 0) {
    writer.writeString(
      2,
      f
    );
  }
  f = message.getConfigurationFileUrl();
  if (f.length > 0) {
    writer.writeString(
      3,
      f
    );
  }
};


/**
 * optional ServiceProvider service_provider = 1;
 * @return {!proto.edu.gla.kail.ad.ServiceProvider}
 */
proto.edu.gla.kail.ad.AgentConfig.prototype.getServiceProvider = function() {
  return /** @type {!proto.edu.gla.kail.ad.ServiceProvider} */ (jspb.Message.getFieldWithDefault(this, 1, 0));
};


/** @param {!proto.edu.gla.kail.ad.ServiceProvider} value */
proto.edu.gla.kail.ad.AgentConfig.prototype.setServiceProvider = function(value) {
  jspb.Message.setProto3EnumField(this, 1, value);
};


/**
 * optional string project_id = 2;
 * @return {string}
 */
proto.edu.gla.kail.ad.AgentConfig.prototype.getProjectId = function() {
  return /** @type {string} */ (jspb.Message.getFieldWithDefault(this, 2, ""));
};


/** @param {string} value */
proto.edu.gla.kail.ad.AgentConfig.prototype.setProjectId = function(value) {
  jspb.Message.setProto3StringField(this, 2, value);
};


/**
 * optional string configuration_file_URL = 3;
 * @return {string}
 */
proto.edu.gla.kail.ad.AgentConfig.prototype.getConfigurationFileUrl = function() {
  return /** @type {string} */ (jspb.Message.getFieldWithDefault(this, 3, ""));
};


/** @param {string} value */
proto.edu.gla.kail.ad.AgentConfig.prototype.setConfigurationFileUrl = function(value) {
  jspb.Message.setProto3StringField(this, 3, value);
};


/**
 * @enum {number}
 */
proto.edu.gla.kail.ad.ServiceProvider = {
  UNRECOGNISED: 0,
  DIALOGFLOW: 1,
  WIZARD: 2,
  SEARCH: 3,
  SPEECH_TO_TEXT: 5
};

goog.object.extend(exports, proto.edu.gla.kail.ad);
