## Installation
### 1. Clone the repository
- git clone https://github.com/uche-inyama/digital_twin.git
- cd into digital_twin

### 2. Install Python dependencies
- cd into agent & run pip install anthropic

### 3. Set your Anthropic API Key
#### Windows:
- set ANTHROPIC_API_KEY="your-api-key-here"

### 4. Running the simulation
1. DigitalTwin folder -> open DigitalTwin.alpx in AnyLogic;
2. Set experiment parameters in main.

| Parameter             | Description        | Values                                                                                |
|:----------------------|:-------------------|:--------------------------------------------------------------------------------------|
| `demandScenario`      | Demand pattern     | 0 = Stable, 1 = Shock, 2 = Disruption                                                 |
| `autonomyCondition`   | Autonomy level     | A0 = Baseline, A2 = Full Auto, A3 = Info_sharing, A4 = LLM + RL, A5 = LLM + RL + HITL |
| `replicationNumber`   | Replication number | 1 to 5                                                                                |

### 5. Enabling Agents & HITL checkbox in main

| Condition        | Agent Enabled | Info Sharing | LLM | RL | HITL |
|------------------|--------------|--------------|-----|----|------|
| A0 Baseline      | ❌ | ❌ | ❌ | ❌ | ❌ |
| A2 Full Auto     | ✅ | ❌ | ❌ | ❌ | ❌ |
| A3 Info Sharing  | ✅ | ✅ | ❌ | ❌ | ❌ |
| A4 LLM+RL        | ✅ | ✅ | ✅ | ✅ | ❌ |
| A5 LLM+RL+HITL   | ✅ | ✅ | ✅ | ✅ | ✅ |

### 6. Run the simulation
- click ▶️ on AnyLogic application to run simulation

### 7. My results
