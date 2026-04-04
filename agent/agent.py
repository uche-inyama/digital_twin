import json
import os
import io
import sys
import anthropic



sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

# ── File paths ────────────────────────────────────────────────
tier_arg = sys.argv[1] if len(sys.argv) > 1 else "Unknown"
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
STATE_FILE = os.path.join(BASE_DIR, f"state_{tier_arg}.json")
RESPONSE_FILE = os.path.join(BASE_DIR, f"response_{tier_arg}.json")

# ── Read state from AnyLogic ──────────────────────────────────
with open(STATE_FILE, "r") as f:
    state = json.load(f)


if state["backlog"] > 0:
    state["inventory"] = max(0, state["inventory"] - state["backlog"])
    print(f"[FIX] Adjusted inventory to {state['inventory']} (cleared backlog)")


tier        = state["tier"]
inventory   = state["inventory"]
backlog     = state["backlog"]
order_up_to = state["orderUpTo"]
sim_time    = state["simTime"]
last_demand = state["lastDemand"]
lead_time   = state["leadTime"]
information_sharing  = state["informationSharing"]
downstream_inventory = state["downstreamInventory"]
downstream_backlog   = state["downstreamBacklog"]

# ── Build prompt ──────────────────────────────────────────────
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
        "2. Order formula: max(0, (order_up_to - inventory) - max(0, downstream_inventory - order_up_to))\n"
        "3. Smooth orders — avoid overordering\n"
        "4. Minimise total chain cost\n"
        "Respond ONLY with this JSON:\n"
        '{"order_quantity": <0-30>, "confidence": <0.0-1.0>, "reasoning": "<20 words max>"}'
    )
else:
    prompt = f"""Inventory manager for {tier} tier.
Week: {sim_time:.0f} | Inventory: {inventory:.0f} | Backlog: {backlog:.0f}
Last demand: {last_demand:.0f} | Lead time: {lead_time} weeks | Order-up-to: {order_up_to}
Holding cost: 0.5/unit/week | Backlog cost: 1.0/unit/week
Respond ONLY: {{"order_quantity": <0-30>, "confidence": <0.0-1.0>, "reasoning": "<10 words max>"}}"""
# ── Call Anthropic API ────────────────────────────────────────
# client = anthropic.Anthropic()  # reads ANTHROPIC_API_KEY from environment

message = client.messages.create(
    model="claude-haiku-4-5-20251001", 
    max_tokens=150, 
    messages=[
        {"role": "user", "content": prompt}
    ]
)

# ── Parse response ────────────────────────────────────────────
# response_text = message.content[0].text.strip()

response_text = message.content[0].text.strip()
response_text = response_text.replace("```json", "").replace("```", "").strip()
print(f"Raw response: {response_text}")  # temporary debug line

# Extract JSON from response even if Claude adds extra text
import re
json_match = re.search(r'\{.*\}', response_text, re.DOTALL)
if json_match:
    result = json.loads(json_match.group())
else:
    raise ValueError(f"No JSON found in response: {response_text}")

try:
    result = json.loads(response_text)
    order_qty   = max(0, float(result["order_quantity"]))
    confidence  = max(0.0, min(1.0, float(result["confidence"])))
    reasoning   = result.get("reasoning", "")
except Exception as e:
    print(f"ERROR calling Claude API: {e}")
    inv_position = inventory - backlog
    order_qty    = max(0, order_up_to - inv_position)
    confidence   = 0.0
    reasoning    = f"Fallback: {str(e)}"

    # write fallback response
    output = {
        "order_quantity": order_qty,
        "confidence":     confidence,
        "reasoning":      reasoning,
        "tier":           tier,
        "sim_time":       sim_time
    }
    with open(RESPONSE_FILE, "w") as f:
        json.dump(output, f)

    print(f"[{tier}] t={sim_time:.1f} -> order={order_qty:.1f} "
          f"confidence={confidence:.2f} | {reasoning}")


# ── Write response for AnyLogic ───────────────────────────────
output = {
    "order_quantity": order_qty,
    "confidence":     confidence,
    "reasoning":      reasoning,
    "tier":           tier,
    "sim_time":       sim_time
}

# Add this debug block
print(f"ABOUT TO WRITE: {RESPONSE_FILE}")
print(f"CONTENT: {json.dumps(output, indent=2)}")

with open(RESPONSE_FILE, "w") as f:
    json.dump(output, f)
    

with open(RESPONSE_FILE, "w") as f:
    json.dump(output, f)

print(f"[{tier}] t={sim_time:.1f} -> order={order_qty:.1f} "
      f"confidence={confidence:.2f} | {reasoning}")