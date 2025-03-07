import stable_baselines3
from stable_baselines3 import PPO
from stable_baselines3.common.envs import MultiAgentEnv
import subprocess

# Start the Java program
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
    

class DominoesEnv(MultiAgentEnv):
    def __init__(self, java_process):
        self.java_process = java_process
        self.agents = ["player_0", "player_1", "player_2", "player_3"]  # Define your agents

    def reset(self):
        # Send a reset command to the Java program (if needed)
        send_action("reset")
        return self._get_state()

    def step(self, actions):
        # Send actions to the Java program
        for agent, action in actions.items():
            send_action(f"{action}")

        # Read the next state and reward
        next_state = self._get_state()
        reward = self._get_reward()
        done = self._is_done()
        return next_state, reward, done, {}

    def _get_state(self):
        state = read_game_state()
        return json.loads(state)

    def _get_reward(self):
        # Parse the reward from the Java program's output
        reward_output = java_process.stdout.readline().strip()
        if(reward_output.__contains__("won!!!!")):
            return 100
        
        else:
            return 0
        
        #return json.loads(reward_output)

    def _is_done(self):
        # Parse the done signal from the Java program's output
        done_output = java_process.stdout.readline().strip()
        return done_output == "DONE"

# Create the environment
env = DominoesEnv(java_process)

# Train the agents
model = PPO("MlpPolicy", env, verbose=1)
model.learn(total_timesteps=10000)