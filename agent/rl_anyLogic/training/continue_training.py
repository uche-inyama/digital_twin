from stable_baselines3 import PPO
from env.supply_chain_env import AnyLogicEnv
from utils.model_versioning import save_version


def continue_training(model_path, timesteps=100_000):

    env = AnyLogicEnv()

    model = PPO.load(model_path, env=env)

    model.learn(total_timesteps=timesteps)

    save_version(model, "policy_v2")

    model.save(f"{model_path}_continued")
    print(f"✅ Model saved to {model_path}_continued")

    return model