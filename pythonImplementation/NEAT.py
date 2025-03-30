import random
import neat
import os
from multiprocessing import Pool

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

def eval_genome(genome, config):
    net = neat.nn.FeedForwardNetwork.create(genome, config)
    fitnesses = [simulate_game([net] * 4) for _ in range(5)]  # Run 5 games
    return sum(fitnesses) / len(fitnesses)  # Return average fitness

def simulate_game(networks):
    game = Game()
    consecutive_passes = 0
    turn = random.randint(0, 3)
    fitness = 0
    
    while consecutive_passes < 4:
        player = game.players[turn]
        net = networks[turn]
        
        # Prepare inputs
        inputs = []
        # Hand (28 inputs)
        hand_map = [0] * 28
        tile_idx = 0
        for i in range(7):
            for j in range(i + 1):
                if Tile(i, j) in player.hand:
                    hand_map[tile_idx] = 1
                tile_idx += 1
        inputs.extend(hand_map)
        # Left/Right ends (2 inputs)
        inputs.append(game.left_end / 6 if game.left_end != -1 else -1)
        inputs.append(game.right_end / 6 if game.right_end != -1 else -1)
        # Board summary (7 inputs)
        board_counts = [0] * 7
        for tile in game.board:
            board_counts[tile.a] += 1
            board_counts[tile.b] += 1
        inputs.extend([c / 10 for c in board_counts])  # Normalize
        
        # Get output
        output = net.activate(inputs)
        tile_idx = output[:-1].index(max(output[:-1]))
        side = 1 if output[-1] >= 0.5 else 0
        
        # Map tile_idx back to tile
        tile_map = [Tile(i, j) for i in range(7) for j in range(i + 1)]
        chosen_tile = tile_map[tile_idx]
        
        playable = player.get_playable_tiles(game.left_end, game.right_end)
        if chosen_tile in playable:
            game.add_to_board(chosen_tile, side)
            player.remove(chosen_tile)
            fitness += 10  # Reward for playing a tile
            consecutive_passes = 0
            if player.size() == 0:
                return 1000 - sum(p.sum() for p in game.players)  # Reward winning
        else:
            consecutive_passes += 1
            fitness += 500 - player.sum()
        
        turn = (turn + 1) % 4
    
    # Stalemate: reward based on tile sum
    return fitness

from multiprocessing import Pool
import neat
import pickle
import logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

def run(config_file):
    # Load the NEAT configuration
    config = neat.Config(neat.DefaultGenome, neat.DefaultReproduction,
                         neat.DefaultSpeciesSet, neat.DefaultStagnation,
                         config_file)
    
    # Create the population
    p = neat.Population(config)
    p.add_reporter(neat.StdOutReporter(True))
    stats = neat.StatisticsReporter()
    p.add_reporter(stats)
    
    # Define the fitness function for p.run
    def fitness_function(genomes, config):
        # Extract just the genome objects from the (genome_id, genome) pairs
        genome_list = [genome for genome_id, genome in genomes]
        
        # Use multiprocessing Pool to evaluate genomes in parallel
        with Pool() as pool:
            fitnesses = pool.starmap(eval_genome, [(genome, config) for genome in genome_list])
        
        # Assign the computed fitness values back to the genomes
        for (genome_id, genome), fitness in zip(genomes, fitnesses):
            genome.fitness = fitness

        for genome_id, genome in genomes:
            genome.fitness = eval_genome(genome, config)
            logger.info(f"Genome {genome_id}: Fitness = {genome.fitness}")
    
    # Run the evolution for 50 generations
    winner = p.run(fitness_function, 50)
    
    # Save the best network
    with open("best_network.pkl", "wb") as f:
        pickle.dump(winner, f)
    
    return winner

# Assuming this is how you call the function
if __name__ == "__main__":
    config_path = os.path.join(os.path.dirname(__file__), "config.txt")
    best_genome = run(config_path)