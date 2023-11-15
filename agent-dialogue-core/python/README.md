# Python scripts for load-testing/debugging

The `run_conversations.py` script can be used to simulate user/agent conversations without the web applications by making gRPCs directly to an instance of the TaskMAD backend (`agent-dialogue-core`). For example, to start 10 conversations with 5 turns each, using conversation ID `conversation_1`:

```sh
python run_conversations.py -n 10 -T 5 -t conversation_1
```

**NOTE**: to prevent unnecssary queries to the LLM API, all conversation IDs will have a `___test` prefix added automatically. This prefix will cause the backend to return a hardcoded string from the `LLMAgent` class instead of making the usual HTTP API call. The script will generate errors if this prefix is not present for some reason. 

## Setup

```sh
virtualenv pyenv
. ./pyenv/bin/activate
pip install -r requirements.txt
# generate the Python protobuf files
./gen_protos.sh

# run a conversation 
python run_conversations.py -n 10 -T 5 -t conversation_1
```

## Simulated conversation details

Conversations are actually generated by the code in `conversation.py`. For each conversation requested by `run_conversations.py`, an instance of the `Conversation` class is created. This sets up gRPC stream listeners for both sides of the conversation and then starts a thread to read messages from each of them. 

The conversation then begins after a random delay, each turn consisting of an agent message and a user reply. The script awaits the receipt of each outgoing message through the gRPC stream for the other side of the conversation before proceeding to the next step to emulate the intended behaviour of real users in the current branch.

The IDs of the agent and user will be of the form `test_agent_{conversation_id}_{n}` and `test_user_{conversation_id}_{n}` where `n` is the index of the conversation in the overall batch.

The delay between messages and turns can be controlled by the `-d`/`--delay` parameter to `run_conversations.py`.