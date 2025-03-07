import random
from typing import List, Dict
from Player import Player
from Tile import Tile

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
        return {
            "board": self.board,
            "player_hand": self.players[player_index].hand,
            "valid_tiles": self.players[player_index].get_playable_tiles(self.left_end, self.right_end),
            "current_player": player_index,
        }