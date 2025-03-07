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
         # Define the agents in the game.
        self.agents = self.possible_agents = ["player0", "player1", "player2", "player3"]

        # Each agent observes a 9D tensor, representing the 3x3 fields of the board.
        # A 0 means an empty field, a 1 represents a piece of player 1, a -1 a piece of
        # player 2.
        self.observation_spaces = {
            "player0": gym.spaces.Box()
            "player1": gym.spaces.Box(-1.0, 1.0, (9,), np.float32),
            "player2": gym.spaces.Box(-1.0, 1.0, (9,), np.float32),
        }
        # Each player has 9 actions, encoding the 9 fields each player can place a piece
        # on during their turn.
        self.action_spaces = {
            "player1": gym.spaces.Discrete(9),
            "player2": gym.spaces.Discrete(9),
        }

        self.board = None
        self.current_player = None


    def reset(self, *, seed=None, options=None):
        ...
        # return observation dict and infos dict.
        return {"agent_1": [obs of agent_1], "agent_2": [obs of agent_2]}, {}

    def step(self, action_dict):
        # return observation dict, rewards dict, termination/truncation dicts, and infos dict
        return {"agent_1": [obs of agent_1]}, {...}, ...