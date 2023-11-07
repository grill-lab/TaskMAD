import json
import http.server
import random
import socketserver
import time
from http.server import BaseHTTPRequestHandler

import grpc
from google.protobuf.json_format import MessageToDict
from google.protobuf.struct_pb2 import Struct

from client_pb2 import (
    ClientId,
    InputInteraction,
    InteractionRequest,
    InteractionRole,
    InteractionType,
)
from service_pb2_grpc import AgentDialogueStub

# Code to make a request to the LLM API from Python
def make_llm_api_request():
    with grpc.secure_channel(
        "taskmad-backend.grill.science", grpc.ssl_channel_credentials()
    ) as channel:
        stub = AgentDialogueStub(channel)

        # create an InteractionRequest proto
        int_request = InteractionRequest()
        int_request.user_id = "test_script_user"
        # tell the server we want the request to go to
        # the LLMAgent
        int_request.chosen_agents[:] = ["LLMAgent"]

        # request body struct contains the conversation ID
        body = Struct()
        body.update({"conversationID": "___test123"})
        int_request.agent_request_parameters["request_body"] = body

        response = stub.GetResponseFromAgents(int_request)
        print(f"Response: {MessageToDict(response)}")

# Basic LLM API mockup for testing
class LLMServer(BaseHTTPRequestHandler):

    def do_POST(self):
        content_length = self.headers['content-length']
        print('content_length = ', content_length)
        body = self.rfile.read(int(content_length))
        print(body)
        self.send_response(200)
        self.send_header('Content-Type', 'application/json')
        self.end_headers()
        resp = { 
                    'status': 'success', 
                    'message': 'Processsed successfully',
                    'data': {
                        'message': 'Sample LLM response', 
                        'role': 'assistant', 
                        'stepNo': 1
                    }
                }
        # random delay for testing
        time.sleep(random.randint(0, 5))
        self.wfile.write(json.dumps(resp).encode('utf-8'))

if __name__ == "__main__":
    server = socketserver.TCPServer(("", 7771), LLMServer)
    server.allow_reuse_address = True
    server.serve_forever()
