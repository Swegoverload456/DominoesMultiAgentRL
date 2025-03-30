import random
from copy import deepcopy

class Tile:
    def __init__(self, a, b):
        self.a = a
        self.b = b
    
    def __str__(self):
        return f"{self.a}:{self.b}"

class Player:
    def __init__(self):
        self.hand = []
    
    def add(self, tile):
        self.hand.append(tile)
    
    def remove(self, tile):
        self.hand.remove(tile)
    
    def get_playable_tiles(self, left_end, right_end):
        if left_end == -1 and right_end == -1:
            return self.hand[:]
        playable = []
        for tile in self.hand:
            if tile.a in (left_end, right_end) or tile.b in (left_end, right_end):
                playable.append(tile)
        return playable
    
    def size(self):
        return len(self.hand)
    
    def sum(self):
        return sum(t.a + t.b for t in self.hand)

class Game:
    def __init__(self):
        self.set = [Tile(i, j) for i in range(7) for j in range(i + 1)]
        random.shuffle(self.set)
        self.players = [Player() for _ in range(4)]
        self.board = []
        self.left_end = -1
        self.right_end = -1
        self.deal_tiles()
    
    def deal_tiles(self):
        for i, tile in enumerate(self.set[:28]):
            self.players[i % 4].add(tile)
    
    def add_to_board(self, tile, side):
        if not self.board:
            self.board.append(Tile(tile.a, tile.b))
            self.left_end = tile.a
            self.right_end = tile.b
            return
        if side == 1:  # Left
            if tile.a == self.left_end:
                self.board.insert(0, Tile(tile.b, tile.a))
                self.left_end = tile.b
            elif tile.b == self.left_end:
                self.board.insert(0, Tile(tile.a, tile.b))
                self.left_end = tile.a
        else:  # Right
            if tile.a == self.right_end:
                self.board.append(Tile(tile.a, tile.b))
                self.right_end = tile.b
            elif tile.b == self.right_end:
                self.board.append(Tile(tile.b, tile.a))
                self.right_end = tile.a