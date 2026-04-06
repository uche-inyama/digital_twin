from py4j.java_gateway import JavaGateway
from stable_baselines3 import PPO
import time
import os

# Connect to AnyLogic
gateway = JavaGateway()
main = gateway.entry_point

# Load trained model
scenario = main.getDemandScenario()
scenario_names = {0: "Stable", 1: "Shock", 2: "Disruption"}
scenario_name = scenario_names.get(scenario, "Stable")

model = PPO.load(f"../models/policy_{scenario_name}_final")

# model_path = os.path.abspath(f"models/policy_{scenario_name}_final")
# print(f"Loading from: {model_path}")

# model = PPO.load(model_path)
# # model = PPO.load(f"models/policy_{scenario_name}_final")

print(f"🤖 Testing {scenario_name} model in AnyLogic")
print("Watch the simulation...")
print("-" * 50)

# Reset simulation
main.resetModel()

# Run for 100 weeks
step = 0
while main.getTime() < 102:  # Changed from 52 to 100
  # Get current state
  state = main.getState()
  
  # Get action from model
  action, _ = model.predict(state, deterministic=True)
  
  # Apply action
  # main.setRLAction(action.tolist())
  # Use the 3-parameter version - NO type conversion issues
  main.setRLAction(float(action[0]), float(action[1]), float(action[2]))
  
  # Run one step (one week)
  main.runOneStep()
  
  step += 1
  print(f"Week {step}: Time={main.getTime():.1f}, Action={[round(a,1) for a in action]}")

print("-" * 50)
print(f"✅ Test complete! Total weeks: {step}")
print(f"💰 Final Reward: {main.getReward():.2f}")