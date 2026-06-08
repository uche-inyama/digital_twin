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

In the DigitalTwin project in AnyLogic, locate the project tree on the left side of the window and click Main. Then select Manufacturer, Distributor, and Retailer in turn (these are instances of the supplyNode agent).

When you select a supplyNode instance, its Properties panel will appear on the right side of the window. The parameters available for configuration are listed as the column headings in the table below.

To configure a simulation scenario, tick the appropriate checkboxes for each supplyNode instance according to the *condition* you want to simulate, using the table below as a guide

| Condition        | Agent Enabled | Info Sharing | LLM | RL | HITL |
|------------------|--------------|--------------|-----|----|------|
| A0 Baseline      | ❌ | ❌ | ❌ | ❌ | ❌ |
| A2 Full Auto     | ✅ | ❌ | ❌ | ❌ | ❌ |
| A3 Info_sharing  | ✅ | ✅ | ❌ | ❌ | ❌ |
| A4 LLM+RL        | ✅ | ✅ | ✅ | ✅ | ❌ |
| A5 LLM+RL+HITL   | ✅ | ✅ | ✅ | ✅ | ✅ |

#### 6. Run the simulation
Caveat: Create a results folder at the root of the project directory.
- click ▶️ on AnyLogic to run the simulation

#### 7. My results
- It is at the root of the project folder: ```Sprint3_Results0.xlsx ```, focus on the first sheet - the raw data sheet.

#### Reinforcement Learning.
To train the reinforcement learning algorithm used for the RL implementation:
- Start the anyLogic simulation with parameters configured to your expectations, then pause it.
- cd into rl_anyLogic folder and run ``` main.py 0 ```. This would restart the simulation in anyLogic.
FYI, the value 0 after main.py represents the demand scenario and should be changed to 1 and 2 accordingly.

For example, assuming training is being done for demand scenario 0 under baseline autonomy level. 
The configuration for a baseline autonomy would be set and the demand scenario also set to 0 on anyLogic. **Then anyLogic would be started and paused.**
We would proceed to run main.py ```**demand scenario**```, which would restart the simulation on anyLogic.

#### Testing the RL_Policy
To test the policy:
-  configure main with the parameters for the simulation expected. Start the simulation and pause it.
-  cd rl_anyLogic/training and run **trained.py 0**. Like before, change the value: 0 -> 1 -> 2. AnyLogic would restart the simulation.

Getting your results:
At the end of all the simulations
- Move import_results.py to the results folder.
- cd into results and run import_results.py. 
