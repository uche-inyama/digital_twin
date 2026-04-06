import os
import shutil
from datetime import datetime


MODEL_DIR = "models"


def save_version(model, name="policy"):
    os.makedirs(MODEL_DIR, exist_ok=True)

    version = datetime.now().strftime("%Y%m%d_%H%M%S")
    path = f"{MODEL_DIR}/{name}_{version}"

    model.save(path)
    print(f"Saved model → {path}")


def save_best(model, score, best_score):
    if score > best_score:
        path = f"{MODEL_DIR}/policy_best"
        model.save(path)
        print("🔥 New BEST model saved!")
        return score
    return best_score


def load_model(model_class, env, path):
    return model_class.load(path, env=env)