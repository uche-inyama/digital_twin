from stable_baselines3 import PPO
from env.supply_chain_env import AnyLogicEnv
from utils.model_versioning import save_version

def train(total_timesteps=200_000, scenario_value=0, scenario_name="Stable"):

    env = AnyLogicEnv()
    
    env.model.setDemandScenario(scenario_value)
    
    model = PPO(
        "MlpPolicy",
        env,
        verbose=1,
        learning_rate=3e-4,
        n_steps=2048,
        batch_size=64,
        gamma=0.99,
        gae_lambda=0.95,
        clip_range=0.2
    )

    model.learn(total_timesteps=total_timesteps)

    print(f"📊 Set demand scenario to: {scenario_value}")
    
    # Create model with name
    model_name = f"policy_{scenario_name}"

    save_version(model, model_name)

    model.save(f"models/{model_name}_final")

    return model