from stable_baselines3 import PPO
from stable_baselines3.common.policies import MultiInputActorCriticPolicy
from dominoes import DominoesEnv

# Create the environment
env = DominoesEnv(num_players=4)

# Create models for each player
models = [PPO(MultiInputActorCriticPolicy, env, verbose=1) for _ in range(4)]

# Training loop
total_timesteps = 10
for timestep in range(total_timesteps):
    # Reset the environment
    obs, _ = env.reset()
    done = False

    while not done:
        # Get the current player
        current_player = env.current_player

        # Get the action from the current player's model
        action, _ = models[current_player].predict(obs)

        # Step the environment
        next_obs, reward, terminated, truncated, info = env.step(action)

        # Train the current player's model
        models[current_player].learn(total_timesteps=1, reset_num_timesteps=False)

        # Update the observation
        obs = next_obs

        # Check if the game is over
        done = terminated or truncated

# Save the models
for i, model in enumerate(models):
    model.save(f"./Dominoes/dominoes_model_player_{i}")