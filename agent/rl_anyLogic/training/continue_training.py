from stable_baselines3 import PPO
from env.supply_chain_env import AnyLogicEnv
from utils.model_versioning import save_version


def continue_training(path="models/policy_v1_final", timesteps=100_000):

    env = AnyLogicEnv()

    model = PPO.load(path, env=env)

    model.learn(total_timesteps=timesteps)

    save_version(model, "policy_v2")

    model.save("models/policy_v2_final")

    return model