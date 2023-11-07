import time
import multiprocessing
import argparse
import random

from conversation import Conversation


def run_conversation(start_delay, cid, user, woz, turns, address, delay, randomize):
    """
    Runs a Conversation after some initial delay period
    """
    c = Conversation(cid, user, woz, turns, address, delay, randomize)
    print(f"Conversation {cid} is waiting for {start_delay} seconds")
    time.sleep(start_delay)
    print(f"Conversation {cid} is starting")
    c.run()
    print(f"Conversation {cid} is complete")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("-n", "--num_conversations", type=int, required=True)
    parser.add_argument("-t", "--tag", type=str, required=True)
    parser.add_argument("-T", "--turns", type=int, required=True)
    parser.add_argument("-d", "--delay", type=float, required=True)

    args = parser.parse_args()

    proclist = []
    for n in range(args.num_conversations):
        cid = f"___test_{args.tag}_{n}"
        arglist = (
            random.randint(10, 40),
            cid,
            f"test_user_{args.tag}_{n}",
            f"test_agent_{args.tag}_{n}",
            args.turns,
            "taskmad-backend.grill.science",
            args.delay,
            True,
        )

        proclist.append(multiprocessing.Process(target=run_conversation, args=arglist))
        proclist[-1].start()

    print(f"Started {len(proclist)} processes, waiting for completion")
    finished = 0
    for p in proclist:
        p.join()
        finished += 1
        print(f"# finished = {finished}")
