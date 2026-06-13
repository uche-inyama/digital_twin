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
- Anthropic model: Claude Haiku 4.5
- FYI: A new api-key can be generated at [Anthropic platform](https://platform.claude.com/)

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

#### 6. Run the Simulation

> **Note:** Create a `results` folder in the root directory of the project before running the simulation.
> Then, in main, find the endOfRun function, and change the **String folder = "<your folder path>/results/";** to match your folder path.
> Also, for each supplyNode instance - retailer, manufacturer, and distributor, change the agentScriptPath to match your file path.

- Click ▶️ in AnyLogic to start the simulation.
- After each completed run:
  - `summary_results.csv` will be updated in the `results` folder.
  - A new `timeseries__.csv` file will be generated in the `results` folder.

#### 7. Reinforcement Learning

To train the reinforcement learning model used in the RL implementation:

- In AnyLogic, go to main; configure each instance of supplyNode parameters  based on the expected simulation outcome, then start and pause the simulation.
- Navigate to the `rl_anyLogic` directory and run:

```bash
python main.py 0
```

- This command will restart the simulation in AnyLogic.

> **Note:** The value after `main.py` represents the demand scenario:
>
> - `0` = Demand Scenario 0
> - `1` = Demand Scenario 1
> - `2` = Demand Scenario 2

##### Example

Suppose you want to train the RL model for **Demand Scenario 0** under the **Baseline Autonomy** configuration:

1. Configure AnyLogic with:
   - **Autonomy Level:** Baseline
   - **Demand Scenario:** `0`
2. Start the simulation and pause it.
3. Run:

```bash
python main.py 0
```

4. The script will reconnect to AnyLogic and restart the simulation for training.

#### 8. Testing the RL Policy

To evaluate a trained policy:

- Configure the desired simulation parameters in AnyLogic.
- Start the simulation and pause it.
- Navigate to `rl_anyLogic/training` and run:

```bash
python trained.py 0
```

- As with training, replace `0` with `1` or `2` to test the corresponding demand scenario.
- The script will restart the simulation in AnyLogic and execute the trained policy.

#### 9. Collecting Results

After all simulation runs have been completed:

- Move `import_results.py` into the `results` folder.
- Navigate to the `results` directory and run:

```bash
python import_results.py
```

This script aggregates the simulation outputs into a consolidated results file.

#### 10. Additional Resources – Experiment Results

The results from my experiment runs are available in the project root directory:

```text
Sprint3_Results0.xlsx
```

- Focus on the **first worksheet ("Raw Data")**, which contains the complete set of collected simulation results.
