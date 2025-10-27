# event_success_ml_pipeline.py
"""
Production-ready ML pipeline for event success prediction
Integrated with Eventora Java entities
"""

import numpy as np
import pandas as pd
import joblib
import json
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Tuple, Optional
from dataclasses import dataclass, asdict

from sklearn.model_selection import train_test_split, cross_val_score, GridSearchCV
from sklearn.ensemble import RandomForestClassifier, GradientBoostingClassifier
from sklearn.metrics import (
    accuracy_score, precision_score, recall_score, f1_score,
    roc_auc_score, classification_report, confusion_matrix
)
from sklearn.preprocessing import StandardScaler
import warnings
warnings.filterwarnings('ignore')

# ============================================================================
# CONFIGURATION
# ============================================================================

@dataclass
class ModelConfig:
    """Configuration for model training and prediction"""
    model_name: str = "event_success_v2"
    model_version: str = "2.0.0"
    random_state: int = 42
    test_size: float = 0.2
    cv_folds: int = 5
    success_threshold: float = 0.5  # 50% attendance
    
    # Model hyperparameters (tuned)
    n_estimators: int = 300
    max_depth: int = 15
    min_samples_split: int = 10
    min_samples_leaf: int = 4
    
    # Feature engineering params
    enable_feature_engineering: bool = True
    scale_features: bool = False  # Tree-based models don't need scaling
    
    # Paths
    model_path: str = "models/event_success_model.pkl"
    scaler_path: str = "models/feature_scaler.pkl"
    config_path: str = "models/model_config.json"
    metrics_path: str = "models/model_metrics.json"

# ============================================================================
# FEATURE DEFINITIONS
# ============================================================================

# Base features (from Event entity)
BASE_FEATURES = [
    "tags_count",
    "posted_days_before_event",
    "promotion_spend",
    "max_participants",
    "ticket_price",
    "organizer_reputation",
    "avg_past_attendance_rate",
    "ctr",
    "social_mentions",
    "weekday"
]

# Categorical features
CATEGORICAL_FEATURES = ["category", "city"]

# One-hot encoded columns
CATEGORY_VALUES = ["TECH", "EDUCATION", "ART", "SPORTS", "HEALTH", "OTHER"]
CITY_VALUES = ["small", "medium", "large"]

# Complete feature list after encoding
ALL_FEATURES = BASE_FEATURES + [
    f"category_{cat}" for cat in CATEGORY_VALUES
] + [
    f"city_{city}" for city in CITY_VALUES
]

# ============================================================================
# DATA GENERATION (IMPROVED)
# ============================================================================

def generate_synthetic_data(n_samples: int = 5000, seed: int = 42) -> pd.DataFrame:
    """
    Generate synthetic event data with realistic patterns
    
    Args:
        n_samples: Number of events to generate
        seed: Random seed for reproducibility
    
    Returns:
        DataFrame with synthetic event data
    """
    np.random.seed(seed)
    
    # Weekday impact (0=Monday, 6=Sunday)
    weekday_boost = {0: 0.0, 1: 0.0, 2: 0.05, 3: 0.05, 4: 0.1, 5: 0.2, 6: 0.15}
    
    # Category impact
    category_boost = {
        'TECH': 0.15, 'EDUCATION': 0.1, 'ART': 0.05,
        'SPORTS': 0.12, 'HEALTH': 0.08, 'OTHER': 0.0
    }
    
    rows = []
    for i in range(n_samples):
        # Basic attributes
        event_id = i + 1
        category = np.random.choice(CATEGORY_VALUES)
        city = np.random.choice(CITY_VALUES, p=[0.4, 0.35, 0.25])
        
        # Numeric features with realistic distributions
        tags_count = np.random.poisson(lam=3)
        posted_days_before = max(1, int(np.random.exponential(scale=15)))
        promotion_spend = max(0, int(np.random.normal(loc=250, scale=350)))
        max_participants = int(np.random.choice(
            [50, 100, 200, 500, 1000],
            p=[0.25, 0.35, 0.20, 0.15, 0.05]
        ))
        
        # Ticket price (correlated with category)
        base_price = np.random.choice([0, 50, 100, 200, 500])
        price_multiplier = 1.2 if category == 'TECH' else 1.0
        ticket_price = round(base_price * price_multiplier * (1 + np.random.rand() * 0.3), 2)
        
        # Organizer metrics
        organizer_reputation = round(np.clip(np.random.beta(2, 2), 0.0, 1.0), 3)
        avg_past_attendance = round(np.clip(np.random.beta(2.5, 2), 0.0, 1.0), 3)
        
        # Marketing metrics
        ctr = round(np.clip(np.random.beta(1.8, 5), 0.0, 1.0), 4)
        social_mentions = int(np.random.poisson(lam=3 + organizer_reputation * 8))
        
        weekday = np.random.randint(0, 7)
        
        # Calculate success probability (realistic model)
        city_mult = {'large': 0.15, 'medium': 0.08, 'small': 0.0}[city]
        category_mult = category_boost[category]
        
        # Promotion effectiveness (diminishing returns)
        promo_effect = 0.0015 * np.log1p(promotion_spend)
        
        # Price impact (negative correlation)
        price_penalty = -0.0003 * ticket_price if ticket_price > 0 else 0.05
        
        # Time to event impact
        time_boost = 0.1 if 7 <= posted_days_before <= 30 else 0.0
        
        base_prob = (
            0.15 +  # baseline
            0.45 * organizer_reputation +
            0.30 * avg_past_attendance +
            0.55 * ctr +
            promo_effect +
            price_penalty +
            weekday_boost[weekday] +
            city_mult +
            category_mult +
            time_boost +
            0.02 * min(tags_count, 5)  # cap tag benefit
        )
        
        # Add realistic noise
        noise = np.random.normal(0, 0.08)
        attendance_rate = np.clip(base_prob + noise, 0.05, 0.95)
        
        # Actual attendance
        checked_in_count = int(np.round(attendance_rate * max_participants))
        revenue = round(checked_in_count * ticket_price, 2)
        
        # Success label (50% threshold)
        success = int(checked_in_count >= 0.5 * max_participants)
        
        rows.append({
            "event_id": event_id,
            "category": category,
            "tags_count": tags_count,
            "posted_days_before_event": posted_days_before,
            "promotion_spend": promotion_spend,
            "max_participants": max_participants,
            "ticket_price": ticket_price,
            "organizer_reputation": organizer_reputation,
            "avg_past_attendance_rate": avg_past_attendance,
            "ctr": ctr,
            "social_mentions": social_mentions,
            "weekday": weekday,
            "city": city,
            "checked_in_count": checked_in_count,
            "revenue": revenue,
            "attendance_rate": attendance_rate,
            "success": success
        })
    
    df = pd.DataFrame(rows)
    return df

# ============================================================================
# FEATURE ENGINEERING
# ============================================================================

def engineer_features(df: pd.DataFrame) -> pd.DataFrame:
    """
    Create advanced features for better predictions
    
    Args:
        df: Input dataframe
    
    Returns:
        DataFrame with engineered features
    """
    df = df.copy()
    
    # Interaction features
    df['promo_per_participant'] = df['promotion_spend'] / (df['max_participants'] + 1)
    df['price_to_promo_ratio'] = df['ticket_price'] / (df['promotion_spend'] + 1)
    df['reputation_ctr_interaction'] = df['organizer_reputation'] * df['ctr']
    
    # Temporal features
    df['is_posted_optimal'] = ((df['posted_days_before_event'] >= 7) & 
                                (df['posted_days_before_event'] <= 30)).astype(int)
    df['is_weekend'] = df['weekday'].isin([5, 6]).astype(int)
    
    # Capacity features
    df['is_large_event'] = (df['max_participants'] >= 200).astype(int)
    df['is_free_event'] = (df['ticket_price'] == 0).astype(int)
    
    # Marketing effectiveness
    df['marketing_score'] = (
        0.4 * df['ctr'] +
        0.3 * (df['social_mentions'] / (df['social_mentions'].max() + 1)) +
        0.3 * (df['promotion_spend'] / (df['promotion_spend'].max() + 1))
    )
    
    # Organizer credibility
    df['organizer_score'] = (
        0.6 * df['organizer_reputation'] +
        0.4 * df['avg_past_attendance_rate']
    )
    
    return df

# ============================================================================
# MODEL TRAINING
# ============================================================================

class EventSuccessPredictor:
    """Production-ready event success prediction model"""
    
    def __init__(self, config: ModelConfig = None):
        self.config = config or ModelConfig()
        self.model = None
        self.scaler = None
        self.feature_columns = None
        self.metrics = {}
        
    def prepare_features(self, df: pd.DataFrame, is_training: bool = True) -> pd.DataFrame:
        """Prepare features for training or prediction"""
        df = df.copy()
        
        # Remove target leakage (don't use checked_in_count or revenue as features)
        if is_training and 'checked_in_count' in df.columns:
            df = df.drop(['checked_in_count', 'revenue', 'attendance_rate'], axis=1)
        
        # Feature engineering
        if self.config.enable_feature_engineering:
            df = engineer_features(df)
        
        # One-hot encode categorical features
        df = pd.get_dummies(df, columns=CATEGORICAL_FEATURES, drop_first=False)
        
        # Ensure all expected columns exist
        expected_cols = ALL_FEATURES.copy()
        if self.config.enable_feature_engineering:
            expected_cols += [
                'promo_per_participant', 'price_to_promo_ratio',
                'reputation_ctr_interaction', 'is_posted_optimal',
                'is_weekend', 'is_large_event', 'is_free_event',
                'marketing_score', 'organizer_score'
            ]
        
        for col in expected_cols:
            if col not in df.columns:
                df[col] = 0
        
        # Store feature columns order
        if is_training:
            self.feature_columns = expected_cols
        
        return df[self.feature_columns]
    
    def train(self, df: pd.DataFrame, tune_hyperparameters: bool = False):
        """
        Train the model
        
        Args:
            df: Training dataframe
            tune_hyperparameters: Whether to perform grid search
        """
        print("=" * 60)
        print(f"Training {self.config.model_name} v{self.config.model_version}")
        print("=" * 60)
        
        # Prepare features
        X = self.prepare_features(df, is_training=True)
        y = df['success']
        
        print(f"\nDataset shape: {X.shape}")
        print(f"Success rate: {y.mean():.2%}")
        print(f"Features: {len(self.feature_columns)}")
        
        # Split data
        X_train, X_test, y_train, y_test = train_test_split(
            X, y, test_size=self.config.test_size,
            random_state=self.config.random_state,
            stratify=y
        )
        
        # Optionally tune hyperparameters
        if tune_hyperparameters:
            print("\nTuning hyperparameters...")
            self.model = self._tune_hyperparameters(X_train, y_train)
        else:
            # Use configured parameters
            self.model = RandomForestClassifier(
                n_estimators=self.config.n_estimators,
                max_depth=self.config.max_depth,
                min_samples_split=self.config.min_samples_split,
                min_samples_leaf=self.config.min_samples_leaf,
                random_state=self.config.random_state,
                n_jobs=-1
            )
        
        # Train model
        print("\nTraining model...")
        self.model.fit(X_train, y_train)
        
        # Evaluate
        self._evaluate(X_train, X_test, y_train, y_test)
        
        # Feature importance
        self._analyze_feature_importance()
        
        print("\n✅ Training complete!")
    
    def _tune_hyperparameters(self, X_train, y_train):
        """Perform grid search for hyperparameter tuning"""
        param_grid = {
            'n_estimators': [200, 300, 400],
            'max_depth': [10, 15, 20],
            'min_samples_split': [5, 10, 15],
            'min_samples_leaf': [2, 4, 6]
        }
        
        grid_search = GridSearchCV(
            RandomForestClassifier(random_state=self.config.random_state, n_jobs=-1),
            param_grid,
            cv=self.config.cv_folds,
            scoring='roc_auc',
            n_jobs=-1,
            verbose=1
        )
        
        grid_search.fit(X_train, y_train)
        print(f"Best parameters: {grid_search.best_params_}")
        print(f"Best CV score: {grid_search.best_score_:.4f}")
        
        return grid_search.best_estimator_
    
    def _evaluate(self, X_train, X_test, y_train, y_test):
        """Comprehensive model evaluation"""
        print("\n" + "=" * 60)
        print("MODEL EVALUATION")
        print("=" * 60)
        
        # Predictions
        y_train_pred = self.model.predict(X_train)
        y_test_pred = self.model.predict(X_test)
        y_test_proba = self.model.predict_proba(X_test)[:, 1]
        
        # Metrics
        train_metrics = {
            'accuracy': accuracy_score(y_train, y_train_pred),
            'precision': precision_score(y_train, y_train_pred),
            'recall': recall_score(y_train, y_train_pred),
            'f1': f1_score(y_train, y_train_pred)
        }
        
        test_metrics = {
            'accuracy': accuracy_score(y_test, y_test_pred),
            'precision': precision_score(y_test, y_test_pred),
            'recall': recall_score(y_test, y_test_pred),
            'f1': f1_score(y_test, y_test_pred),
            'roc_auc': roc_auc_score(y_test, y_test_proba)
        }
        
        # Cross-validation
        cv_scores = cross_val_score(
            self.model, X_train, y_train,
            cv=self.config.cv_folds,
            scoring='roc_auc',
            n_jobs=-1
        )
        
        print("\nTrain Metrics:")
        for metric, value in train_metrics.items():
            print(f"  {metric.capitalize()}: {value:.4f}")
        
        print("\nTest Metrics:")
        for metric, value in test_metrics.items():
            print(f"  {metric.capitalize()}: {value:.4f}")
        
        print(f"\nCross-Validation ROC-AUC: {cv_scores.mean():.4f} (+/- {cv_scores.std():.4f})")
        
        print("\nConfusion Matrix:")
        print(confusion_matrix(y_test, y_test_pred))
        
        print("\nClassification Report:")
        print(classification_report(y_test, y_test_pred, target_names=['Failure', 'Success']))
        
        # Store metrics
        self.metrics = {
            'train': train_metrics,
            'test': test_metrics,
            'cv_roc_auc_mean': float(cv_scores.mean()),
            'cv_roc_auc_std': float(cv_scores.std()),
            'trained_at': datetime.now().isoformat()
        }
    
    def _analyze_feature_importance(self):
        """Analyze and display feature importance"""
        importances = self.model.feature_importances_
        indices = np.argsort(importances)[::-1]
        
        print("\n" + "=" * 60)
        print("TOP 15 FEATURE IMPORTANCES")
        print("=" * 60)
        
        for i in range(min(15, len(self.feature_columns))):
            idx = indices[i]
            print(f"{i+1:2d}. {self.feature_columns[idx]:30s} : {importances[idx]:.4f}")
    
    def predict(self, event_data: Dict) -> Tuple[float, int, List[str]]:
        """
        Predict event success and generate recommendations
        
        Args:
            event_data: Dictionary with event features
        
        Returns:
            Tuple of (probability, label, recommendations)
        """
        if self.model is None:
            raise ValueError("Model not trained. Call train() first or load a saved model.")
        
        # Convert to DataFrame
        df = pd.DataFrame([event_data])
        
        # Prepare features
        X = self.prepare_features(df, is_training=False)
        
        # Predict
        proba = self.model.predict_proba(X)[0][1]
        label = int(proba >= 0.5)
        
        # Generate recommendations
        recommendations = self._generate_recommendations(event_data, proba)
        
        return proba, label, recommendations
    
    def _generate_recommendations(self, event_data: Dict, prob: float) -> List[str]:
        """Generate actionable recommendations based on prediction"""
        recs = []
        
        # Critical factors (if probability < 0.6)
        if prob < 0.6:
            if event_data.get("organizer_reputation", 0) < 0.5:
                recs.append(
                    "🎯 CRITICAL: Improve organizer credibility (current: "
                    f"{event_data.get('organizer_reputation', 0):.2f}). "
                    "Showcase past events, testimonials, and achievements."
                )
            
            if event_data.get("ctr", 0) < 0.25:
                recs.append(
                    f"🎯 CRITICAL: Improve CTR (current: {event_data.get('ctr', 0):.2%}). "
                    "Use eye-catching visuals, compelling title, and clear value proposition."
                )
        
        # Marketing recommendations
        promo = event_data.get("promotion_spend", 0)
        if promo < 200:
            expected_boost = min(0.15, (400 - promo) * 0.0003)
            recs.append(
                f"💰 Increase promotion budget to ₹400+ (current: ₹{promo}). "
                f"Expected success boost: +{expected_boost:.1%}"
            )
        
        if event_data.get("social_mentions", 0) < 5:
            recs.append(
                "📱 Boost social media presence. Target: 10+ mentions. "
                "Use hashtags, influencer partnerships, and engaging content."
            )
        
        # Timing recommendations
        days_before = event_data.get("posted_days_before_event", 0)
        if days_before < 7:
            recs.append(
                f"⏰ Post event earlier (current: {days_before} days). "
                "Sweet spot: 7-30 days before event for maximum reach."
            )
        elif days_before > 45:
            recs.append(
                "⏰ Event posted too early. Consider refresh campaign closer to event date."
            )
        
        # Pricing recommendations
        price = event_data.get("ticket_price", 0)
        max_p = event_data.get("max_participants", 100)
        
        if price > 300 and prob < 0.5:
            revenue_at_lower = 0.7 * max_p * (price * 0.75)
            revenue_current = prob * max_p * price
            if revenue_at_lower > revenue_current:
                recs.append(
                    f"💵 Consider reducing price from ₹{price:.0f} to ₹{price*0.75:.0f}. "
                    f"Expected revenue increase: ₹{revenue_at_lower - revenue_current:.0f}"
                )
        
        # Capacity optimization
        if prob > 0.8 and price > 0:
            recs.append(
                f"✨ Strong performance predicted! Consider increasing capacity "
                f"from {max_p} to {int(max_p * 1.2)} to maximize revenue."
            )
        
        # Tags and discoverability
        if event_data.get("tags_count", 0) < 3:
            recs.append(
                "🏷️ Add more relevant tags (target: 4-6) to improve discoverability."
            )
        
        # Weekend vs weekday
        if event_data.get("weekday", 0) < 5 and prob < 0.6:
            recs.append(
                "📅 Consider moving to weekend (Friday/Saturday) for better attendance."
            )
        
        # If no recommendations
        if not recs and prob >= 0.7:
            recs.append("✅ Event setup looks great! No major changes recommended.")
        elif not recs:
            recs.append("📊 Event metrics are moderate. Focus on overall marketing strategy.")
        
        return recs
    
    def save(self):
        """Save model, config, and metrics"""
        # Create models directory
        Path(self.config.model_path).parent.mkdir(parents=True, exist_ok=True)
        
        # Save model
        joblib.dump(self.model, self.config.model_path)
        joblib.dump(self.feature_columns, self.config.model_path.replace('.pkl', '_features.pkl'))
        
        # Save config
        with open(self.config.config_path, 'w') as f:
            json.dump(asdict(self.config), f, indent=2)
        
        # Save metrics
        with open(self.config.metrics_path, 'w') as f:
            json.dump(self.metrics, f, indent=2)
        
        print(f"\n✅ Model saved to {self.config.model_path}")
    
    @classmethod
    def load(cls, model_path: str = None):
        """Load a trained model"""
        config = ModelConfig()
        if model_path:
            config.model_path = model_path
        
        predictor = cls(config)
        predictor.model = joblib.load(config.model_path)
        predictor.feature_columns = joblib.load(
            config.model_path.replace('.pkl', '_features.pkl')
        )
        
        # Load config if exists
        if Path(config.config_path).exists():
            with open(config.config_path, 'r') as f:
                loaded_config = json.load(f)
                predictor.config = ModelConfig(**loaded_config)
        
        print(f"✅ Model loaded from {config.model_path}")
        return predictor

# ============================================================================
# MAIN EXECUTION
# ============================================================================

def main():
    """Main training pipeline"""
    
    # Generate synthetic data
    print("Generating synthetic event data...")
    df = generate_synthetic_data(n_samples=6000)
    df.to_csv("synthetic_event_success.csv", index=False)
    print(f"✅ Saved synthetic_event_success.csv, shape: {df.shape}")
    
    # Initialize and train predictor
    config = ModelConfig(
        enable_feature_engineering=True,
        n_estimators=300,
        max_depth=15
    )
    
    predictor = EventSuccessPredictor(config)
    predictor.train(df, tune_hyperparameters=False)  # Set True for grid search
    
    # Save model
    predictor.save()
    
    # Test prediction
    print("\n" + "=" * 60)
    print("SAMPLE PREDICTION")
    print("=" * 60)
    
    sample_event = {
        "tags_count": 5,
        "posted_days_before_event": 20,
        "promotion_spend": 800,
        "max_participants": 300,
        "ticket_price": 150,
        "organizer_reputation": 0.85,
        "avg_past_attendance_rate": 0.78,
        "ctr": 0.45,
        "social_mentions": 12,
        "weekday": 5,  # Saturday
        "category": "TECH",
        "city": "large"
    }
    
    prob, label, recs = predictor.predict(sample_event)
    
    print(f"\nEvent: {sample_event['category']} event in {sample_event['city']} city")
    print(f"Predicted Success Probability: {prob:.1%}")
    print(f"Predicted Label: {'✅ SUCCESS' if label else '❌ FAILURE'}")
    print(f"\n📋 Recommendations ({len(recs)}):")
    for i, rec in enumerate(recs, 1):
        print(f"{i}. {rec}")

if __name__ == "__main__":
    main()