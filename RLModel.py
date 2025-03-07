from ray.rllib.env.multi_agent_env import MultiAgentEnv
import gymnasium as gym
import numpy as np

import subprocess

java_process = subprocess.Popen(
    ["java", "-jar", "Game.jar"],  # Replace with your Java command
    stdin=subprocess.PIPE,
    stdout=subprocess.PIPE,
    stderr=subprocess.PIPE,
    text=True
)

# Read the game state from the Java program's console output
def read_game_state():
    return java_process.stdout.readline().strip()

import json



# Send an action to the Java program's console input
def send_action(action):
    game_state = read_game_state()
    parsed_state = json.loads(game_state)
    if(int(action) > len(parsed_state["valid_tiles"])):
        action = str(len(parsed_state["valid_tiles"]))
    '''print("Board:", parsed_state["board"])
    print("Player Hand:", parsed_state["player_hand"])
    print("Valid Tiles:", parsed_state["valid_tiles"])
    print("Current Player:", parsed_state["current_player"])'''
    java_process.stdin.write(action + "\n")
    java_process.stdin.flush()
    

class MyMultiAgentEnv(MultiAgentEnv):

    def __init__(self, config=None):
        super().__init__()
        ...

    def reset(self, *, seed=None, options=None):
        ...
        # return observation dict and infos dict.
        return {"agent_1": [obs of agent_1], "agent_2": [obs of agent_2]}, {}

    def step(self, action_dict):
        # return observation dict, rewards dict, termination/truncation dicts, and infos dict
        return {"agent_1": [obs of agent_1]}, {...}, ...