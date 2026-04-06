from training.train import train
from training.continue_training import continue_training
from training.trained import run_trained_model
from env.supply_chain_env import AnyLogicEnv
import sys

if __name__ == "__main__":

    # Get scenario as integer from command line
    scenario = int(sys.argv[1]) if len(sys.argv) > 1 else 0
    
    # Map integer to name for file saving
    scenario_names = {0: "Stable", 1: "Shock", 2: "Disruption"}
    scenario_name = scenario_names.get(scenario, "Stable")
    
    print(f"🎯 Training for demand scenario: {scenario} ({scenario_name})")
    
    # Train (pass integer to set in AnyLogic)
    model = train(200_000, scenario_value=scenario, scenario_name=scenario_name)
    model = continue_training(f"models/policy_{scenario_name}_final", 100_000)
    