from Tile import Tile
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
