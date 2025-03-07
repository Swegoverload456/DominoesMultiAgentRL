import random
from typing import List, Dict
from Player import Player
from Tile import Tile

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

        if not playable_tiles:
            self.consecutive_passes += 1
            if self.consecutive_passes == self.num_players:
                self.game_over = True
            return

        tile_to_play = playable_tiles[action["tile_index"]]
        side = action.get("side", 0)
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
        # Example: Flatten the board and player hand into a numpy array
        board_tiles = [tile.getA() for tile in self.board] + [tile.getB() for tile in self.board]
        player_hand = [tile.getA() for tile in self.players[player_index].hand] + [tile.getB() for tile in self.players[player_index].hand]
        valid_tiles = [tile.getA() for tile in self.players[player_index].get_playable_tiles(self.left_end, self.right_end)] + [tile.getB() for tile in self.players[player_index].get_playable_tiles(self.left_end, self.right_end)]
        state = board_tiles + player_hand + valid_tiles + [self.left_end, self.right_end, player_index]
        
        # Ensure the state has a consistent shape
        if len(state) < 31:
            # Pad with zeros if necessary
            state += [-1] * (31 - len(state))
        elif len(state) > 31:
            # Truncate if necessary
            state = state[:31]
        
        return np.array(state, dtype=np.float32)


# Reinforcement Learning Environment
class DominoesEnv(gym.Env):
    def __init__(self):
        super(DominoesEnv, self).__init__()
        self.game = DominoesGame()
        self.action_space = spaces.Discrete(28)  # Example: 28 possible actions
        self.observation_space = spaces.Box(low=-1, high=1, shape=(31,), dtype=np.float32)  # Updated to match observation shape

    def reset(self, seed=None, options=None):
        # Reset the game
        self.game.reset()
        # Get the initial state
        state = self.game.get_state(0)
        info = {}  # Additional info (optional)
        return state, info

    def step(self, action):
        # Play the action
        self.game.play_turn(0, action)
        # Get the next state
        next_state = self.game.get_state(0)
        # Define reward and termination condition
        reward = 1 if self.game.game_over else 0
        terminated = self.game.game_over
        truncated = False  # Truncation is not used in this example
        info = {}  # Additional info (optional)
        return next_state, reward, terminated, truncated, info


# Train the model
env = make_vec_env(lambda: DominoesEnv(), n_envs=1)
model = PPO("MlpPolicy", env, verbose=1)
model.learn(total_timesteps=10000)