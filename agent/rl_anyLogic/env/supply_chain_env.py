import gymnasium as gym
from gymnasium import spaces
import numpy as np
from py4j.java_gateway import JavaGateway


class AnyLogicEnv(gym.Env):

    def __init__(self, max_time=52):
        super().__init__()

        self.gateway = JavaGateway()
        self.model = self.gateway.entry_point

        self.max_time = max_time

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
        self.model.resetModel()

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

        reward = float(self.model.getReward())
        reward = np.tanh(reward / 100.0)

        terminated = self.model.getTime() >= self.max_time
        truncated = False

        return obs, reward, terminated, truncated, {}