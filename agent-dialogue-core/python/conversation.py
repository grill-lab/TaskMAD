import argparse
import datetime
import random
import threading
import time

import colorama
import grpc
from google.protobuf.struct_pb2 import Struct

from client_pb2 import (
    ClientId,
    InteractionRequest,
    InteractionRole,
    InteractionType,
)
from service_pb2_grpc import AgentDialogueStub

colorama.init()


# copying the structure of these from the webapps
def makeInputInteraction(ii, args):
    ii.text = args.get("text", "")
    ii.language_code = "en-GB"
    ii.type = args.get("interaction_type", InteractionType.TEXT)
    ii.action[:] = args.get("action", [])
    # ignore all the "logged_" fields
    ii.role = args.get("role", InteractionRole.NOROLE)
    return ii


def makeInteractionRequest(args):
    intr = InteractionRequest()
    intr.client_id = args.get("client_id", ClientId.EXTERNAL_APPLICATION)
    makeInputInteraction(intr.interaction, args)
    intr.user_id = args["user_id"]
    intr.time.GetCurrentTime()  # lol @ Google
    intr.chosen_agents[:] = args.get("chosen_agents", ["WizardOfOz"])
    # this is a Struct
    intr.agent_request_parameters.update({"conversationId": args["conversation_id"]})
    return intr


def makeLLMRequest(args):
    args["chosen_agents"] = ["LLMAgent"]
    args["type"] = InteractionType.TEXT
    assert len(args["text"]) > 0
    assert args["conversation_id"].startswith("___test")
    return makeInteractionRequest(args)


def get_stub(addr):
    channel = grpc.secure_channel(addr, grpc.ssl_channel_credentials())
    stub = AgentDialogueStub(channel)
    return stub


class StreamReader(threading.Thread):
    """
    A threaded class to read the streaming responses from the gRPC server
    """

    def __init__(self, name, iterator):
        super().__init__()
        self.iterator = iterator
        self.messages = []
        self.message_ids = set()
        self.daemon = True

    def run(self):
        for resp in self.iterator:
            # print(f'StreamReader({self.name}) received {resp}')
            self.messages.append(resp)


class Conversation(threading.Thread):
    """
    Simulates a basic conversation between agent and user
    """

    def __init__(self, conv_id, chat_id, woz_id, num_turns, addr, delay, randomize):
        self.conv_id = conv_id
        self.num_turns = num_turns
        self.chat_id = chat_id
        self.woz_id = woz_id
        self.turns = 0
        self.addr = addr
        self.active = False
        self.message_count = 0
        self.delay = delay
        self.randomize = randomize
        self.messages = []

    def log(self, m):
        print(
            f"{datetime.datetime.now().isoformat()}|{self.conv_id}: {m}{colorama.Style.RESET_ALL}"
        )

    def run(self):
        self.log("Starting conversation")

        # create gRPC connections on behalf of both participants
        self.woz_stub = get_stub(self.addr)
        self.chat_stub = get_stub(self.addr)

        # set up listeners for new messages (have to do this for
        # both sides to be realistic even though it means locally
        # we receive every message twice)
        subscribe_args = {
            "conversation_id": self.conv_id,
            "user_id": self.woz_id,
        }
        self.woz_resp_iter = self.woz_stub.ListResponses(
            makeInteractionRequest(subscribe_args)
        )
        subscribe_args = {
            "conversation_id": self.conv_id,
            "user_id": self.chat_id,
        }
        self.chat_resp_iter = self.chat_stub.ListResponses(
            makeInteractionRequest(subscribe_args)
        )

        self.woz_reader = StreamReader("WOZ", self.woz_resp_iter)
        self.chat_reader = StreamReader("CHAT", self.chat_resp_iter)

        self.woz_reader.start()
        self.chat_reader.start()

        self.run_conversation()

    def run_conversation(self):
        self.log(f"run_conversation: {threading.get_ident()}")

        # wait for any existing messages to be streamed through after
        # the connections are initially made
        time.sleep(3)

        self.active = True
        self.log(f"Received {self.message_count} initial messages")
        self.log(f"Starting {self.num_turns} turns")

        for ti in range(self.num_turns):
            self.log(
                f"\n\n{colorama.Back.MAGENTA + colorama.Fore.BLACK}________({self.conv_id}:TURN {ti}/{self.num_turns})________"
            )
            self.do_turn(ti)
            self._delay()

    def _delay(self):
        period = self.delay
        if self.randomize:
            period += random.random() * 5
        self.log(f"Delay for {period} secs...")
        time.sleep(period)

    def send_woz_to_chat(self, args):
        """
        Send an agent message to the user
        """
        self.log(f"{colorama.Fore.YELLOW}send_woz_to_chat: {args}")
        self.woz_stub.GetResponseFromAgents(makeInteractionRequest(args))
        self._delay()

    def send_chat_to_woz(self, args):
        """
        Send a user message to the agent
        """
        self.log(f"{colorama.Fore.GREEN}send_chat_to_woz: {args}")
        self.chat_stub.GetResponseFromAgents(makeInteractionRequest(args))
        self._delay()

    def send_woz_to_llm(self, args):
        """
        Send a request from the agent to the LLM agent

        Unlike in the real conversations, this will cause the LLMAgent to
        return a fixed response without querying the real API
        """
        intr = makeInteractionRequest(args)

        # LLMAgent requires the conversationId in the request parameters
        body = Struct()
        body.update({"conversationId": self.conv_id})
        intr.agent_request_parameters["request_body"] = body

        self.log(f"{colorama.Fore.CYAN}LLM request being sent")
        # note that this response is NOT a streaming one!
        resp = self.woz_stub.GetResponseFromAgents(intr)
        return resp

    def do_turn(self, ti):
        # TODO tidy this up
        if ti == 0:
            # handle first turn slightly differently in that there's no
            # call to the LLM involved and the agent just initates immediately
            self.log("Doing first turn")

            # send a woz message
            args = {
                "text": "Hello there, I'm here to assist you in your culinary adventures",
                "type": InteractionType.TEXT,
                "role": InteractionRole.ASSISTANT,
                "user_id": self.woz_id,
                "session_id": "blah",
                "conversation_id": self.conv_id,
            }

            self.send_woz_to_chat(args)

            # "user" waits for the message
            self.log("Initial WoZ message sent, waiting for reply")
            while (
                len(self.chat_reader.messages) == 0
                or not self.chat_reader.messages[-1].user_id == self.woz_id
            ):
                self.log(">>> waiting on wm")
                time.sleep(0.5)

            # type is still TEXT, text/role/user_id need to change
            args["text"] = "chat reply to turn 0"
            args["role"] = InteractionRole.NOROLE
            args["user_id"] = self.chat_id
            self.log("User received message, sending reply")

            self.send_chat_to_woz(args)

        else:
            # at the start of each turn from 2-n, we begin by waiting for a
            # message from the user (which completed the previous turn)
            self.log("Awaiting incoming message")
            while (
                len(self.woz_reader.messages) == 0
                or not self.woz_reader.messages[-1].user_id == self.chat_id
            ):
                self.log(">>> waiting on cm")
                time.sleep(0.5)

            # to emulate real behaviour, the arrival of this message should
            # result in an automatic call to the LLM API
            self.log("WoZ requesting LLM response")
            args = {
                "user_id": self.woz_id,
                "chosen_agents": ["LLMAgent"],
                "conversation_id": self.conv_id,
            }
            llm_resp = self.send_woz_to_llm(args)

            # this response can just be forwarded to the user
            llm_text = llm_resp.interaction[0].unstructured_result["data"]["message"]
            self.log(f"Forwarding LLM response [{llm_text}] to chat")

            args = {
                "text": f"WoZ #{ti}, {llm_text}",
                "type": InteractionType.TEXT,
                "role": InteractionRole.ASSISTANT,
                "user_id": self.woz_id,
                "session_id": "blah",
                "conversation_id": self.conv_id,
            }
            self.send_woz_to_chat(args)
            while (
                len(self.chat_reader.messages) == 0
                or not self.chat_reader.messages[-1].user_id == self.woz_id
            ):
                self.log(">>> waiting on wm")
                time.sleep(0.5)

            # type is still TEXT, text/role/user_id need to change
            args["text"] = f"chat reply to turn {ti}"
            args["role"] = InteractionRole.NOROLE
            args["user_id"] = self.chat_id

            # send the user reply back to complete the turn
            self.log("User received message, sending reply")
            self.send_chat_to_woz(args)


if __name__ == "__main__":
    parser = argparse.ArgumentParser("Test TaskMAD gRPC backend")
    parser.add_argument(
        "-a",
        "--address",
        help="Remote address, defaults to to taskmad-backend.grill.science",
        default="taskmad-backend.grill.science",
        required=False,
        type=str,
    )
    parser.add_argument(
        "-c",
        "--conversation_id",
        help="Conversation ID. Should start with ___test",
        required=True,
        type=str,
    )
    parser.add_argument(
        "-t",
        "--turns",
        help="Number of turns to simulate in each conversation",
        required=True,
        type=int,
    )
    parser.add_argument("-u", "--user", help="Chat user ID", required=True, type=str)
    parser.add_argument("-w", "--woz", help="WoZ user ID", required=True, type=str)
    parser.add_argument(
        "-d",
        "--delay",
        help="Delay in seconds between messages in a turn",
        required=False,
        type=float,
        default=1,
    )
    parser.add_argument(
        "-r",
        "--randomize",
        help="Randomize delays between messages by up to 10 seconds on top of --delay value",
        required=False,
        action="store_true",
    )
    args = parser.parse_args()

    c = Conversation(
        args.conversation_id,
        args.user,
        args.woz,
        args.turns,
        args.address,
        args.delay,
        args.randomize,
    )

    c.run()
    print(f"Conversation {args.conversation_id} completed!")
