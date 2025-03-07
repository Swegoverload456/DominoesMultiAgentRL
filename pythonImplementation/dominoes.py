import random
import numpy as np
import gymnasium as gym
from gymnasium import spaces
from stable_baselines3 import PPO
from stable_baselines3.common.env_util import make_vec_env

# Tile class
class Tile:
    def __init__(self, a, b):
        self.side1 = a
        self.side2 = b

    def getA(self):
        return self.side1

    def getB(self):
        return self.side2

    def sum(self):
        return self.side1 + self.side2

    def __str__(self):
        return f"{self.side1}:{self.side2}"

    def __repr__(self):
        return self.__str__()

    def __eq__(self, other):
        return (self.side1 == other.side1 and self.side2 == other.side2) or (
            self.side1 == other.side2 and self.side2 == other.side1
        )

    def __lt__(self, other):
        if self.side2 == other.side2:
            return self.side1 < other.side1
        return self.side2 < other.side2


# Player class
class Player:
    def __init__(self):
        self.hand = []

    def add(self, tile):
        self.hand.append(tile)

    def remove(self, a, b):
        tile_to_remove = Tile(a, b)
        self.hand = [tile for tile in self.hand if tile != tile_to_remove]

    def get_playable_tiles(self, end_left, end_right):
        if end_left == -1 and end_right == -1:
            return self.hand

        playable_tiles = []
        for tile in self.hand:
            if (
                tile.getA() == end_left
                or tile.getA() == end_right
                or tile.getB() == end_left
                or tile.getB() == end_right
            ):
                playable_tiles.append(tile)
        return playable_tiles

    def sum_hand(self):
        return sum(tile.sum() for tile in self.hand)

    def sort_hand(self):
        self.hand.sort()

    def __str__(self):
        return str(self.hand)

    def __repr__(self):
        return self.__str__()


# DominoesGame class
class DominoesGame:
    def __init__(self, num_players=4):
        self.num_players = num_players
        self.set = self.create_tile_set()
        self.players = [Player() for _ in range(num_players)]
        self.board = []
        self.left_end = -1
        self.right_end = -1
        self.consecutive_passes = 0
        self.turn = random.randint(0, num_players - 1)
        self.game_over = False

    def create_tile_set(self):
        tiles = []
        for i in range(7):
            for j in range(i + 1):
                tiles.append(Tile(j, i))
        random.shuffle(tiles)
        return tiles

    def deal_tiles(self):
        for player in self.players:
            for _ in range(7):
                player.add(self.set.pop())

    def add_to_board(self, tile, side=0):
        if not self.board:
            self.board.append(tile)
            self.left_end = tile.getA()
            self.right_end = tile.getB()
            return

        if side == 1:  # Place on the left
            if tile.getA() == self.left_end:
                self.board.insert(0, Tile(tile.getB(), tile.getA()))
                self.left_end = tile.getB()
            elif tile.getB() == self.left_end:
                self.board.insert(0, Tile(tile.getA(), tile.getB()))
                self.left_end = tile.getA()
            else:
                self.board.insert(0, tile)
                self.left_end = tile.getA()
        elif side == 2:  # Place on the right
            if tile.getA() == self.right_end:
                self.board.append(Tile(tile.getA(), tile.getB()))
                self.right_end = tile.getB()
            elif tile.getB() == self.right_end:
                self.board.append(Tile(tile.getB(), tile.getA()))
                self.right_end = tile.getA()
            else:
                self.board.append(tile)
                self.right_end = tile.getA()
        else:  # Default placement
            if tile.getA() == self.left_end or tile.getB() == self.left_end:
                self.add_to_board(tile, 1)
            elif tile.getA() == self.right_end or tile.getB() == self.right_end:
                self.add_to_board(tile, 2)

    def play_turn(self, player_index, action):
        player = self.players[player_index]
        playable_tiles = player.get_playable_tiles(self.left_end, self.right_end)

        if not playable_tiles or action < 0 or action >= len(playable_tiles):
            # No playable tiles or invalid action: pass
            self.consecutive_passes += 1
            if self.consecutive_passes == self.num_players:
                self.game_over = True
            return

        # Use the integer action as the index of the tile to play
        tile_to_play = playable_tiles[action]
        side = 0  # Default side (you can modify this logic if needed)
        self.add_to_board(tile_to_play, side)
        player.remove(tile_to_play.getA(), tile_to_play.getB())
        self.consecutive_passes = 0

        if not player.hand:
            self.game_over = True
            print(f"Player {player_index} wins!")

    def reset(self):
        self.set = self.create_tile_set()
        self.players = [Player() for _ in range(self.num_players)]
        self.board = []
        self.left_end = -1
        self.right_end = -1
        self.consecutive_passes = 0
        self.turn = random.randint(0, self.num_players - 1)
        self.game_over = False
        self.deal_tiles()

    def get_state(self, player_index):
        # Board state
        board_tiles = [tile.getA() for tile in self.board] + [tile.getB() for tile in self.board]
        board_tiles += [-1] * (56 - len(board_tiles))  # Pad with -1

        # Player's hand
        player_hand = [tile.getA() for tile in self.players[player_index].hand] + [tile.getB() for tile in self.players[player_index].hand]
        player_hand += [-1] * (14 - len(player_hand))  # Pad with -1

        # Valid tiles
        valid_tiles = [tile.getA() for tile in self.players[player_index].get_playable_tiles(self.left_end, self.right_end)] + [tile.getB() for tile in self.players[player_index].get_playable_tiles(self.left_end, self.right_end)]
        valid_tiles += [-1] * (14 - len(valid_tiles))  # Pad with -1

        return {
            "board": np.array(board_tiles, dtype=np.float32),
            "hand": np.array(player_hand, dtype=np.float32),
            "valid_tiles": np.array(valid_tiles, dtype=np.float32),
        }


class DominoesEnv(gym.Env):
    def __init__(self, num_players=4):
        super(DominoesEnv, self).__init__()
        self.num_players = num_players
        self.game = DominoesGame(num_players=num_players)
        self.action_space = spaces.Discrete(7)  # Example: 7 possible actions
        self.observation_space = spaces.Dict({
            "board": spaces.Box(low=-1, high=6, shape=(56,), dtype=np.float32),
            "hand": spaces.Box(low=-1, high=6, shape=(14,), dtype=np.float32),
            "valid_tiles": spaces.Box(low=-1, high=6, shape=(14,), dtype=np.float32),
        })
        self.current_player = 0  # Track the current player

    def reset(self, seed=None, options=None):
        self.game.reset()
        self.current_player = 0
        state = self.game.get_state(self.current_player)
        info = {}
        return state, info

    def step(self, action):
        # Play the action for the current player
        self.game.play_turn(self.current_player, action)

        # Check if the game is over
        if self.game.game_over:
            rewards = self.calculate_rewards()
            reward = rewards[self.current_player]  # Get the reward for the current player
            terminated = True
        else:
            reward = 0
            terminated = False

        # Move to the next player
        self.current_player = (self.current_player + 1) % self.num_players

        # Get the next state for the new current player
        next_state = self.game.get_state(self.current_player)
        truncated = False
        info = {}

        return next_state, reward, terminated, truncated, info

    def calculate_rewards(self):
        rewards = {}
        winner = None
        for i, player in enumerate(self.game.players):
            if not player.hand:
                winner = i
                break

        for i in range(self.num_players):
            if i == winner:
                rewards[i] = 100  # Winner gets 100
            else:
                rewards[i] = 100 - self.game.players[i].sum_hand()  # Others get 100 - sum of their hand

        return rewards

'''from stable_baselines3 import PPO
from stable_baselines3.common.callbacks import CheckpointCallback
from stable_baselines3.common.env_util import make_vec_env
import os

# Create the environment
env = make_vec_env(lambda: DominoesEnv(), n_envs=1)

from stable_baselines3.common.policies import MultiInputActorCriticPolicy

# Create the model with MultiInputActorCriticPolicy
model = PPO(MultiInputActorCriticPolicy, env, verbose=1)

# Save a checkpoint every 10,000 timesteps
checkpoint_callback = CheckpointCallback(save_freq=10000, save_path="./checkpoints/", name_prefix="dominoes_model")

# Train the model with checkpointing
model.learn(total_timesteps=50000, callback=checkpoint_callback)

# Find the latest checkpoint
checkpoint_dir = "./checkpoints/"
checkpoints = [f for f in os.listdir(checkpoint_dir) if f.startswith("dominoes_model")]
latest_checkpoint = max(checkpoints, key=lambda x: int(x.split("_")[2]))

# Load the latest checkpoint
model = PPO.load(os.path.join(checkpoint_dir, latest_checkpoint))

# Set the environment for the loaded model
model.set_env(env)

# Continue training
model.learn(total_timesteps=50000, reset_num_timesteps=False)

# Save the final model
model.save("./Dominoes/dominoes_model_final")'''