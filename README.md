## Installation
#### 1. Clone the repository
- git clone https://github.com/uche-inyama/digital_twin.git
- cd into digital_twin

#### 2. Install Python dependency
- cd into agent & run pip install anthropic

#### 3. Set your Anthropic API Key
##### Windows:
- create a .env file at the root of the project folder
- copy and paste into .env file: ```set ANTHROPIC_API_KEY="your-api-key-here" ```

#### 4. Running the simulation
1. DigitalTwin folder -> open DigitalTwin.alpx in AnyLogic;
2. Set experiment parameters in main.

| Parameter             | Description        | Values                                                                                |
|:----------------------|:-------------------|:--------------------------------------------------------------------------------------|
| `demandScenario`      | Demand Scenario     | 0 = Stable, 1 = Shock, 2 = Disruption                                                 |
| `autonomyCondition`   | Autonomy level     | 0 = Baseline, 2 = Full Auto, 3 = Info_sharing, 4 = LLM + RL, 5 = LLM + RL + HITL |
| `replicationNumber`   | Replication number | 1 to 5                                                                                |

FYI: The values of ``` Demand Scenario ``` are crucial, because a function in AnyLogic - DigitalTwin environment needs them.

#### 5. Enabling checkboxes in the main

Inside the DigitalTwin project on AnyLogic, on the left side of the window, click on main, then choose Manufacturer, Distributor, and Retailer one after the other - instances of the supplyNode.
When you select one of the supplyNode instances, a properties panel will open on the right side of the window, and you will see the parameters defined as column headings in the table below. Check the boxes accordingly using the table below, depending on the ***condition*** you want to simulate.

| Condition        | Agent Enabled | Info Sharing | LLM | RL | HITL |
|------------------|--------------|--------------|-----|----|------|
| A0 Baseline      | ❌ | ❌ | ❌ | ❌ | ❌ |
| A2 Full Auto     | ✅ | ❌ | ❌ | ❌ | ❌ |
| A3 Info_sharing  | ✅ | ✅ | ❌ | ❌ | ❌ |
| A4 LLM+RL        | ✅ | ✅ | ✅ | ✅ | ❌ |
| A5 LLM+RL+HITL   | ✅ | ✅ | ✅ | ✅ | ✅ |

#### 6. Run the simulation
- click ▶️ on AnyLogic to run the simulation

#### 7. My results
- It is at the root of the project folder: ```Sprint3_Results0.xlsx ```, focus on the first sheet - the raw data sheet.
