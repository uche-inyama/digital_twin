import gymnasium as gym
from gymnasium import spaces
import numpy as np
from py4j.java_gateway import JavaGateway


class AnyLogicEnv(gym.Env):

    def __init__(self, max_steps=100):
        super().__init__()

        self.gateway = JavaGateway()
        self.model = self.gateway.entry_point

        self.max_steps = max_steps

        # Action: normalized RL output
        self.action_space = spaces.Box(
            low=-1,
            high=1,
            shape=(3,),
            dtype=np.float32
        )

        # Observation space
        self.observation_space = spaces.Box(
            low=-1e6,
            high=1e6,
            shape=(6,),
            dtype=np.float32
        )

    def reset(self, seed=None, options=None):
        super().reset(seed=seed)
        # self.model.resetModel()
        self.current_step = 0
        # print(f"RESET called - current_step = {self.current_step}")  # DEBUG

        obs = np.array(self.model.getState(), dtype=np.float32)
        return obs, {}

    def _scale_action(self, action):
        r = (action[0] + 1) / 2 * 24
        d = (action[1] + 1) / 2 * 40
        m = (action[2] + 1) / 2 * 50

        return np.clip(r, 0, 24), np.clip(d, 0, 40), np.clip(m, 0, 50)

    def step(self, action):

        r, d, m = self._scale_action(action)

        self.model.setRLAction(float(r), float(d), float(m))
        self.model.runOneStep()

        obs = np.array(self.model.getState(), dtype=np.float32)

        # Get raw cost from AnyLogic (holding + backlog)
        raw_cost = float(self.model.getReward())

        reward = raw_cost

        reward = reward / 100.0

        self.current_step += 1
        terminated = self.current_step >= self.max_steps
        truncated = False

        return obs, reward, terminated, truncated, {}