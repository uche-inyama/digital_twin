import sys
import json
import os
import anthropic
import re


client = anthropic.Anthropic(api_key=os.environ.get("ANTHROPIC_API_KEY"))

tier_arg = sys.argv[1] if len(sys.argv) > 1 else "Unknown"

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
STATE_FILE = os.path.join(BASE_DIR, f"state_{tier_arg}.json")
RESPONSE_FILE = os.path.join(BASE_DIR, f"response_{tier_arg}.json")
RL_POLICY_FILE = os.path.join(BASE_DIR, f"rl_policy_{tier_arg}.json")

# ── Read state from AnyLogic ──────────────────────────────────
with open(STATE_FILE, "r") as f:
    state = json.load(f)

# ── Read RL policy (advisor) ──────────────────────────────────
rl_order = None
rl_confidence = None
rl_reasoning = None

if os.path.exists(RL_POLICY_FILE):
    with open(RL_POLICY_FILE, "r") as f:
        rl_data = json.load(f)
        rl_order = rl_data.get("rl_order")
        rl_confidence = rl_data.get("rl_confidence")
        rl_reasoning = rl_data.get("rl_reasoning")
    os.remove(RL_POLICY_FILE)
    print(f"[RL ADVISOR] Suggests: order={rl_order}, conf={rl_confidence}")
else:
    print(f"[RL ADVISOR] No suggestion available")

# # ── Fix backlog ────────────────────────────────────────────────
# if state["backlog"] > 0:
#     state["inventory"] = max(0, state["inventory"] - state["backlog"])
#     print(f"[FIX] Adjusted inventory to {state['inventory']}")

# ── Extract variables ──────────────────────────────────────────
tier = state["tier"]
inventory = state["inventory"]
backlog = state["backlog"]
order_up_to = state["orderUpTo"]
sim_time = state["simTime"]
last_demand = state["lastDemand"]
lead_time = state["leadTime"]
information_sharing = state["informationSharing"]
downstream_inventory = state["downstreamInventory"]
downstream_backlog = state["downstreamBacklog"]

# ── Build prompt (LLM is decision maker) ──────────────────────
if information_sharing:
    prompt = (
        "Inventory manager for " + tier + " tier with supply chain visibility.\n"
        "Week: " + str(int(sim_time)) +
        " | Inventory: " + str(int(inventory)) +
        " | Backlog: " + str(int(backlog)) + "\n"
        "Last demand: " + str(int(last_demand)) +
        " | Lead time: " + str(lead_time) + " weeks"
        " | Order-up-to: " + str(order_up_to) + "\n"
        "Downstream inventory: " + str(int(downstream_inventory)) +
        " | Downstream backlog: " + str(int(downstream_backlog)) + "\n"
        "Holding cost: 0.5/unit/week | Backlog cost: 1.0/unit/week\n"
        "COLLABORATION RULES:\n"
        "1. If downstream inventory > order-up-to, reduce your order by the excess\n"
        "2. order = max(backlog, max(0, order_up_to - inventory) - max(0, downstream_inventory - order_up_to))\n"
        "3. Smooth orders — avoid overordering\n"
        "4. Minimise total chain cost\n"
    )
else:
    prompt = (
        f"Inventory manager for {tier} tier.\n"
        f"Week: {sim_time:.0f} | Inventory: {inventory:.0f} | Backlog: {backlog:.0f}\n"
        f"Last demand: {last_demand:.0f} | Lead time: {lead_time} weeks | Order-up-to: {order_up_to}\n"
        f"Holding cost: 0.5/unit/week | Backlog cost: 1.0/unit/week\n"
    )

# ── ADD RL AS ADVISOR (not commander) ──────────────────────────
if rl_order:
        prompt += f"""
        === RL ADVISOR (PRIORITY INSTRUCTION) ===
        Trained RL policy recommends: {rl_order:.0f} units
        This policy was trained to minimise system-wide cost and bullwhip ratio.
        OVERRIDE the collaboration rules above with this recommendation.
        ONLY deviate if: backlog == 0 AND inventory > 3x order-up-to ({order_up_to * 3}).
        Do NOT reduce this order based on downstream inventory alone.
        Your order_quantity should be close to {rl_order:.0f} units.
        """
        
# ── Response format ───────────────────────────────────────────
prompt += f"""
Respond ONLY with this JSON:
{{"order_quantity": <0-{int(order_up_to * 2)}>, "confidence": <0.0-1.0>, "reasoning": "<10 words max>"}}
"""

# ── Call Anthropic API ────────────────────────────────────────
message = client.messages.create(
    model="claude-haiku-4-5-20251001",
    max_tokens=150,
    messages=[{"role": "user", "content": prompt}]
)

# ── Parse response (LLM decides) ──────────────────────────────
response_text = message.content[0].text.strip()
response_text = response_text.replace("```json", "").replace("```", "").strip()

json_match = re.search(r'\{.*\}', response_text, re.DOTALL)

if response_text:
    try:
        result = json.loads(json_match.group())
        order_qty = max(0.0, min(order_up_to, float(result.get("order_quantity", 0))))
        confidence = max(0.0, min(1.0, float(result.get("confidence", 0))))
        reasoning = result.get("reasoning", "")
        print(f"[LLM DECISION] Order={order_qty}, Conf={confidence}, Reason={reasoning}")
    except:
        inv_position = inventory - backlog
        order_qty = max(0, order_up_to - inv_position)
        confidence = 0.0
        reasoning = "Parse error"
        print(f"[LLM DECISION] Parse error - using fallback: {order_qty}")
else:
    inv_position = inventory - backlog
    order_qty = max(0, order_up_to - inv_position)
    confidence = 0.0
    reasoning = "No JSON"
    print(f"[LLM DECISION] No JSON - using fallback: {order_qty}")

# ── Compare with RL suggestion (debug only) ────────────────────
if rl_order:
    print(f"[COMPARISON] RL suggested: {rl_order}, LLM decided: {order_qty}")
    if abs(rl_order - order_qty) > 5:
        print(f"[NOTE] LLM significantly deviated from RL suggestion")

# ── Write response ────────────────────────────────────────────
output = {
    "order_quantity": order_qty,
    "confidence": confidence,
    "reasoning": reasoning,
    "tier": tier,
    "sim_time": sim_time
}

with open(RESPONSE_FILE, "w") as f:
    json.dump(output, f)

print(f"[{tier}] t={sim_time:.0f} -> FINAL ORDER={order_qty} (LLM decision)")