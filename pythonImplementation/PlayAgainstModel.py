import numpy as np
from stable_baselines3 import PPO
from stable_baselines3.common.policies import MultiInputActorCriticPolicy
from dominoes import DominoesEnv  # Import your custom environment

# Load the trained model
try:
    model_agent1 = PPO.load("./Dominoes/dominoes_model_player_1.zip")
    model_agent2 = PPO.load("./Dominoes/dominoes_model_player_2.zip")
    model_agent3 = PPO.load("./Dominoes/dominoes_model_player_3.zip")
except FileNotFoundError:
    print("Model file not found. Please ensure the model is saved correctly.")
    exit()

# Create the environment with 4 players
env = DominoesEnv(num_players=4)

# Reset the environment
obs, _ = env.reset()

# Game loop
while True:
    # Display the current game state
    print("\nCurrent Board:", env.game.board)
    print("Your Hand (Player 0):", env.game.players[0].hand)
    print("Valid Tiles:", env.game.players[0].get_playable_tiles(env.game.left_end, env.game.right_end))

    # Human player's turn (Player 0)
    if len(env.game.players[0].get_playable_tiles(env.game.left_end, env.game.right_end)) > 0:
        print("\nYour Turn (Player 0)!")
        print("Choose a tile to play (enter the index):")
        for i, tile in enumerate(env.game.players[0].get_playable_tiles(env.game.left_end, env.game.right_end)):
            print(f"{i}: {tile}")
        action = int(input("Enter the index of the tile: "))
    else:
        print("\nNo valid moves. You must pass.")
        action = None

    # Human player's action
    if action is not None:
        env.game.play_turn(0, action)  # Pass the action as an integer and side as 0

    # Check if the game is over
    if env.game.game_over:
        break

    # Model Agent 1's turn (Player 1)
    print("\nModel Agent 1's Turn (Player 1)!")
    obs_player1 = env.game.get_state(1)
    print("Observation for Player 1:", obs_player1)
    model_action1, _ = model_agent1.predict(obs_player1)

    # Filter the action to ensure it is valid
    valid_tiles_player1 = env.game.players[1].get_playable_tiles(env.game.left_end, env.game.right_end)
    if model_action1 >= len(valid_tiles_player1):
        model_action1 = len(valid_tiles_player1) - 1  # Choose the last valid tile

    print("Model Action for Player 1 (filtered):", model_action1)
    env.game.play_turn(1, model_action1)  # Pass the action as an integer and side as 0

    # Check if the game is over
    if env.game.game_over:
        break

    # Model Agent 2's turn (Player 2)
    print("\nModel Agent 2's Turn (Player 2)!")
    obs_player2 = env.game.get_state(2)
    print("Observation for Player 2:", obs_player2)
    model_action2, _ = model_agent2.predict(obs_player2)

    # Filter the action to ensure it is valid
    valid_tiles_player2 = env.game.players[2].get_playable_tiles(env.game.left_end, env.game.right_end)
    if model_action2 >= len(valid_tiles_player2):
        model_action2 = len(valid_tiles_player2) - 1  # Choose the last valid tile

    print("Model Action for Player 2 (filtered):", model_action2)
    env.game.play_turn(2, model_action2)  # Pass the action as an integer and side as 0

    # Check if the game is over
    if env.game.game_over:
        break

    # Model Agent 3's turn (Player 3)
    print("\nModel Agent 3's Turn (Player 3)!")
    obs_player3 = env.game.get_state(3)
    print("Observation for Player 3:", obs_player3)
    model_action3, _ = model_agent3.predict(obs_player3)

    # Filter the action to ensure it is valid
    valid_tiles_player3 = env.game.players[3].get_playable_tiles(env.game.left_end, env.game.right_end)
    if model_action3 >= len(valid_tiles_player3):
        model_action3 = len(valid_tiles_player3) - 1  # Choose the last valid tile

    print("Model Action for Player 3 (filtered):", model_action3)
    env.game.play_turn(3, model_action3)  # Pass the action as an integer and side as 0

    # Check if the game is over
    if env.game.game_over:
        break

    # Update the observation for the human player
    obs = env.game.get_state(0)