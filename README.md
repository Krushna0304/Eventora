# Eventora ML Service

Lightweight machine-learning service for predicting event success. This repository contains training and service scripts, a small synthetic dataset, and model metadata.

Files of interest
- `event_success_ml_pipeline.py` — training/pipeline script used to prepare data and train models.
- `ml_service.py` — small inference service wrapper (HTTP or CLI) to load the trained model and serve predictions.
- `requirements.txt` — Python dependencies for development and running the service.
- `synthetic_event_success.csv` — sample dataset used for experimentation.
- `models/model_config.json` — model configuration and hyperparameters (kept in repo).
- `models/model_metrics.json` — evaluation metrics for the stored model (kept in repo).

Quick start (Windows PowerShell)

1. Create and activate a virtual environment (recommended):

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
```

2. Install dependencies:

```powershell
pip install -r requirements.txt
```

3. Run the training/pipeline (example):

```powershell
python event_success_ml_pipeline.py
```

4. Run the inference/service (example):

```powershell
python ml_service.py
```

Notes
- The repository purposely keeps `models/model_config.json` and `models/model_metrics.json` tracked to document model choices and performance.
- Binary model artifacts (for example, `*.pkl`, `*.joblib`, `*.h5`, `*.pt`) are ignored by `.gitignore`. If you want to store models in the repo, remove or change `.gitignore` accordingly.
- The supplied `synthetic_event_success.csv` is a small demo dataset. Replace it with your real data for production runs.

Development
- Run tests (if any) with your preferred runner. There are no automated tests included by default — consider adding unit tests for the pipeline and the service.

Suggested next steps
- Add a small example request/response to `ml_service.py` or a `tests/` folder demonstrating typical inputs.
- Add CI to run linting and tests on push.

License
MIT License — see `LICENSE` (not included). Add a license file if required.

Maintainer
If you need help, open an issue or contact the repository owner.
