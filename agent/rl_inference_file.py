# # rl_inference_file.py
# import sys
# import json
# import numpy as np
# import os
# from stable_baselines3 import PPO

# # ── BASE DIRECTORIES ───────────────────────────────────────────
# # State files are in: C:\...\DigitalTwin\agent\
# STATE_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "agent")

# # Models are in: C:\...\DigitalTwin\agent\rl_anyLogic\models\
# MODELS_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "agent", "rl_anyLogic", "models")

# print(f"STATE_DIR: {STATE_DIR}")
# print(f"MODELS_DIR: {MODELS_DIR}")

# # Map scenario numbers to names
# scenario_names = {
#     0: "Stable",
#     1: "Shock", 
#     2: "Disruption"
# }

# tier = sys.argv[1] if len(sys.argv) > 1 else "retailer"

# # ── Read state from file ──────────────────────────────────────
# state_file = os.path.join(STATE_DIR, f"state_rl_{tier}.json")
# print(f"Reading state from: {state_file}")

# with open(state_file, "r") as f:
#     state = json.load(f)

# # ── Get scenario from state ────────────────────────────────────
# scenario_num = state.get('demandScenario', 1)
# scenario_name = scenario_names.get(scenario_num, "Stable")

# # ── Load the CORRECT model for this scenario ───────────────────
# model_path = os.path.join(MODELS_DIR, f"policy_{scenario_name}_final")
# print(f"Loading model: {model_path}")
# model = PPO.load(model_path)

# # ── Convert to observation ─────────────────────────────────────
# obs = np.array([
#     state['inventory'],
#     state['backlog'],
#     state['downstreamInventory'],
#     state['downstreamBacklog'],
#     state['systemTotalInventory'],
#     state['systemTotalBacklog']
# ], dtype=np.float32)

# # ── Predict ────────────────────────────────────────────────────
# action, _ = model.predict(obs, deterministic=True)

# # Get order for this tier
# tier_index = {"retailer": 0, "distributor": 1, "manufacturer": 2}
# raw_action = float(action[tier_index.get(tier, 0)])

# order_qty = (raw_action + 1) / 2 * 24
# order_qty = max(0, min(24, order_qty))

# # ── Write response ─────────────────────────────────────────────
# response_file = os.path.join(STATE_DIR, f"response_rl_{tier}.json")
# response = {
#     "order_quantity": order_qty,
#     "confidence": 0.9,
#     "reasoning": f"RL policy from {scenario_name} model"
# }

# with open(response_file, "w") as f:
#     json.dump(response, f)

# print(f"[{tier}] Scenario: {scenario_name}, RL order: {order_qty}")
# print(f"Response written to: {response_file}")


import sys
import json
import numpy as np
import os
from stable_baselines3 import PPO

# STATE_DIR  = os.path.join(os.path.dirname(
#     os.path.dirname(os.path.abspath(__file__))), "agent")
# MODELS_DIR = os.path.join(os.path.dirname(
#     os.path.dirname(os.path.abspath(__file__))), "agent", "rl_anyLogic", "models")

STATE_DIR  = os.path.dirname(os.path.abspath(__file__))
MODELS_DIR = os.path.join(STATE_DIR, "rl_anyLogic", "models")

print(f"STATE_DIR: {STATE_DIR}")
print(f"MODELS_DIR: {MODELS_DIR}")

# ── Fix scenario mapping to match AnyLogic ────────────────────
# AnyLogic: 0=Stable, 1=Shock, 2=Disruption
# Training used:  1=Stable, 2=Shock, 3=Disruption (off by one)
# So load model trained on scenario+1 to match
scenario_names = {
    0: "Stable",
    1: "Shock",
    2: "Disruption"
}

# ── Correct action scaling per tier ───────────────────────────
tier_scales = {
    "Retailer":     24,
    "Distributor":  40,
    "Manufacturer": 50
}
tier_indices = {
    "Retailer":     0,
    "Distributor":  1,
    "Manufacturer": 2
}

tier = sys.argv[1] if len(sys.argv) > 1 else "Retailer"

# ── Read state ────────────────────────────────────────────────
state_file = os.path.join(STATE_DIR, f"state_rl_{tier}.json")
print(f"Reading state from: {state_file}")

with open(state_file, "r") as f:
    state = json.load(f)

scenario_num  = state.get("demandScenario", 0)
scenario_name = scenario_names.get(scenario_num, "Stable")

# ── Load model ────────────────────────────────────────────────
model_path = os.path.join(MODELS_DIR, f"policy_{scenario_name}_final")
print(f"Loading model: {model_path}")
model = PPO.load(model_path)

# ── Build observation — RAW values, no normalisation ──────────
# Must match what AnyLogicEnv.getState() returns during training
obs = np.array([
    state["retailerInventory"],
    state["retailerBacklog"],
    state["distributorInventory"],
    state["distributorBacklog"],
    state["manufacturerInventory"],
    state["manufacturerBacklog"]
], dtype=np.float32)

# ── Predict ───────────────────────────────────────────────────
action, _ = model.predict(obs, deterministic=True)

# ── Scale correctly per tier ──────────────────────────────────
idx       = tier_indices.get(tier, 0)
scale     = tier_scales.get(tier, 24)
raw       = float(action[idx])
order_qty = (raw + 1) / 2 * scale
order_qty = max(0, min(scale, order_qty))

# ── Write response ────────────────────────────────────────────
response_file = os.path.join(STATE_DIR, f"response_rl_{tier}.json")
response = {
    "order_quantity": order_qty,
    "confidence":     0.9,
    "reasoning":      f"RL policy: {scenario_name} scenario"
}

with open(response_file, "w") as f:
    json.dump(response, f)

print(f"[{tier}] Scenario: {scenario_name}, RL order: {order_qty}")
print(f"Response written to: {response_file}")




print(f"This script location: {os.path.abspath(__file__)}")
print(f"STATE_DIR resolves to: {STATE_DIR}")
print(f"MODELS_DIR resolves to: {MODELS_DIR}")
print(f"State file exists: {os.path.exists(state_file)}")
print(f"Model file exists: {os.path.exists(model_path + '.zip')}")