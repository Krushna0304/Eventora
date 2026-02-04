# ml_service.py
"""
FastAPI Microservice for Event Success Prediction
Complete end-to-end ML service with training, prediction, and model management
"""

from fastapi import FastAPI, HTTPException, BackgroundTasks, Depends
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field, validator
from typing import List, Optional, Dict, Any
from datetime import datetime
from enum import Enum
import uvicorn
import joblib
import pandas as pd
import numpy as np
from pathlib import Path
import logging
import json

# Import the ML pipeline (from previous artifact)
# Assuming event_success_ml_pipeline.py is in the same directory
from event_success_ml_pipeline import (
    EventSuccessPredictor, ModelConfig, generate_synthetic_data
)

# ============================================================================
# LOGGING CONFIGURATION
# ============================================================================

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# ============================================================================
# PYDANTIC MODELS (Request/Response Schemas)
# ============================================================================

class EventCategory(str, Enum):
    TECH = "TECH"
    EDUCATION = "EDUCATION"
    ART = "ART"
    SPORTS = "SPORTS"
    HEALTH = "HEALTH"
    OTHER = "OTHER"

class CityCategory(str, Enum):
    SMALL = "small"
    MEDIUM = "medium"
    LARGE = "large"

class EventPredictionRequest(BaseModel):
    """Request model for event prediction"""
    event_id: Optional[int] = None
    tags_count: int = Field(..., ge=0, le=20, description="Number of tags")
    posted_days_before_event: int = Field(..., ge=1, le=365, description="Days before event")
    promotion_spend: int = Field(..., ge=0, description="Promotion budget")
    max_participants: int = Field(..., ge=1, description="Maximum participants")
    ticket_price: float = Field(..., ge=0, description="Ticket price")
    organizer_reputation: float = Field(..., ge=0.0, le=1.0, description="Organizer reputation (0-1)")
    avg_past_attendance_rate: float = Field(..., ge=0.0, le=1.0, description="Past attendance rate (0-1)")
    ctr: float = Field(..., ge=0.0, le=1.0, description="Click-through rate (0-1)")
    social_mentions: int = Field(..., ge=0, description="Social media mentions")
    weekday: int = Field(..., ge=0, le=6, description="Day of week (0=Mon, 6=Sun)")
    category: EventCategory
    city: CityCategory
    
    class Config:
        schema_extra = {
            "example": {
                "tags_count": 5,
                "posted_days_before_event": 20,
                "promotion_spend": 800,
                "max_participants": 300,
                "ticket_price": 150.0,
                "organizer_reputation": 0.85,
                "avg_past_attendance_rate": 0.78,
                "ctr": 0.45,
                "social_mentions": 12,
                "weekday": 5,
                "category": "TECH",
                "city": "large"
            }
        }

class Recommendation(BaseModel):
    """Individual recommendation"""
    priority: str = Field(..., description="HIGH, MEDIUM, LOW")
    category: str = Field(..., description="MARKETING, PRICING, TIMING, etc.")
    message: str
    impact: Optional[str] = None

class PredictionResponse(BaseModel):
    """Response model for prediction"""
    success: bool
    event_id: Optional[int]
    probability: float = Field(..., description="Success probability (0-1)")
    label: int = Field(..., description="Predicted label (0=Failure, 1=Success)")
    confidence: str = Field(..., description="HIGH, MEDIUM, LOW")
    expected_attendance: int
    expected_revenue: float
    recommendations: List[Recommendation]
    model_name: str
    model_version: str
    predicted_at: str

class BatchPredictionRequest(BaseModel):
    """Batch prediction request"""
    events: List[EventPredictionRequest]

class BatchPredictionResponse(BaseModel):
    """Batch prediction response"""
    predictions: List[PredictionResponse]
    total_events: int
    processed_at: str

class TrainingRequest(BaseModel):
    """Training request model"""
    n_samples: int = Field(default=5000, ge=100, le=50000)
    tune_hyperparameters: bool = Field(default=False)
    enable_feature_engineering: bool = Field(default=True)

class TrainingResponse(BaseModel):
    """Training response model"""
    success: bool
    message: str
    metrics: Optional[Dict[str, Any]]
    model_path: str
    trained_at: str

class ModelInfo(BaseModel):
    """Model information"""
    model_name: str
    model_version: str
    trained_at: Optional[str]
    features_count: int
    metrics: Optional[Dict[str, Any]]
    config: Dict[str, Any]

class HealthResponse(BaseModel):
    """Health check response"""
    status: str
    model_loaded: bool
    model_name: Optional[str]
    model_version: Optional[str]
    uptime_seconds: float

# ============================================================================
# FASTAPI APPLICATION
# ============================================================================

app = FastAPI(
    title="Event Success Prediction Service",
    description="ML Microservice for predicting event success and providing recommendations",
    version="2.0.0",
    docs_url="/docs",
    redoc_url="/redoc"
)

# CORS configuration
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Configure appropriately for production
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ============================================================================
# GLOBAL STATE
# ============================================================================

class AppState:
    """Application state management"""
    def __init__(self):
        self.predictor: Optional[EventSuccessPredictor] = None
        self.model_loaded: bool = False
        self.start_time: datetime = datetime.now()
        self.prediction_count: int = 0
        self.training_in_progress: bool = False
    
    def load_model(self):
        """Load the trained model"""
        try:
            model_path = "models/event_success_model.pkl"
            if Path(model_path).exists():
                self.predictor = EventSuccessPredictor.load(model_path)
                self.model_loaded = True
                logger.info(f"Model loaded successfully from {model_path}")
            else:
                logger.warning(f"Model not found at {model_path}. Please train a model first.")
                self.model_loaded = False
        except Exception as e:
            logger.error(f"Error loading model: {str(e)}")
            self.model_loaded = False
            raise

state = AppState()

# ============================================================================
# STARTUP/SHUTDOWN EVENTS
# ============================================================================

@app.on_event("startup")
async def startup_event():
    """Initialize application on startup"""
    logger.info("Starting Event Success Prediction Service...")
    try:
        state.load_model()
    except Exception as e:
        logger.warning(f"Could not load model on startup: {str(e)}")
    logger.info("Service started successfully")

@app.on_event("shutdown")
async def shutdown_event():
    """Cleanup on shutdown"""
    logger.info(f"Shutting down... Total predictions served: {state.prediction_count}")

# ============================================================================
# HELPER FUNCTIONS
# ============================================================================

def categorize_recommendations(recs: List[str]) -> List[Recommendation]:
    """Convert string recommendations to structured format"""
    structured_recs = []
    
    for rec in recs:
        priority = "HIGH" if "CRITICAL" in rec else ("MEDIUM" if any(x in rec for x in ["Consider", "Boost"]) else "LOW")
        
        # Determine category
        if any(x in rec for x in ["promotion", "budget", "social", "marketing", "CTR"]):
            category = "MARKETING"
        elif any(x in rec for x in ["price", "revenue", "₹"]):
            category = "PRICING"
        elif any(x in rec for x in ["days", "posted", "weekend", "timing"]):
            category = "TIMING"
        elif any(x in rec for x in ["credibility", "reputation", "organizer"]):
            category = "CREDIBILITY"
        elif any(x in rec for x in ["capacity", "participants"]):
            category = "CAPACITY"
        elif any(x in rec for x in ["tags", "discoverability"]):
            category = "DISCOVERY"
        else:
            category = "GENERAL"
        
        # Extract impact if present
        impact = None
        if "boost:" in rec.lower():
            impact = rec.split("boost:")[-1].split()[0].strip()
        elif "increase:" in rec.lower():
            impact = rec.split("increase:")[-1].split()[0].strip()
        
        structured_recs.append(Recommendation(
            priority=priority,
            category=category,
            message=rec.replace("🎯 CRITICAL: ", "").replace("💰 ", "").replace("📱 ", "")
                       .replace("⏰ ", "").replace("💵 ", "").replace("✨ ", "")
                       .replace("🏷️ ", "").replace("📅 ", "").replace("✅ ", "")
                       .replace("📊 ", ""),
            impact=impact
        ))
    
    return structured_recs

def calculate_confidence(probability: float) -> str:
    """Calculate confidence level"""
    if probability >= 0.75 or probability <= 0.25:
        return "HIGH"
    elif probability >= 0.6 or probability <= 0.4:
        return "MEDIUM"
    else:
        return "LOW"

def calculate_expected_metrics(event_data: Dict, probability: float) -> tuple:
    """Calculate expected attendance and revenue"""
    max_participants = event_data.get("max_participants", 0)
    ticket_price = event_data.get("ticket_price", 0)
    
    expected_attendance = int(probability * max_participants)
    expected_revenue = round(expected_attendance * ticket_price, 2)
    
    return expected_attendance, expected_revenue

# ============================================================================
# API ENDPOINTS
# ============================================================================

@app.get("/", response_model=Dict[str, str])
async def root():
    """Root endpoint"""
    return {
        "service": "Event Success Prediction API",
        "version": "2.0.0",
        "status": "operational",
        "docs": "/docs"
    }

@app.get("/health", response_model=HealthResponse)
async def health_check():
    """Health check endpoint"""
    uptime = (datetime.now() - state.start_time).total_seconds()
    
    return HealthResponse(
        status="healthy" if state.model_loaded else "degraded",
        model_loaded=state.model_loaded,
        model_name=state.predictor.config.model_name if state.model_loaded else None,
        model_version=state.predictor.config.model_version if state.model_loaded else None,
        uptime_seconds=uptime
    )

@app.post("/predict", response_model=PredictionResponse)
async def predict_event_success(request: EventPredictionRequest):
    """
    Predict event success probability and provide recommendations
    
    This is the main endpoint called by Spring Boot backend
    """
    if not state.model_loaded:
        raise HTTPException(
            status_code=503,
            detail="Model not loaded. Please train or load a model first."
        )
    
    try:
        # Convert request to dict
        event_data = request.dict()
        event_data['category'] = request.category.value
        event_data['city'] = request.city.value
        
        # Get prediction
        probability, label, recommendations = state.predictor.predict(event_data)
        
        # Calculate expected metrics
        expected_attendance, expected_revenue = calculate_expected_metrics(
            event_data, probability
        )
        
        # Structure recommendations
        structured_recs = categorize_recommendations(recommendations)
        
        # Increment counter
        state.prediction_count += 1
        
        # Build response
        response = PredictionResponse(
            success=True,
            event_id=request.event_id,
            probability=round(probability, 4),
            label=label,
            confidence=calculate_confidence(probability),
            expected_attendance=expected_attendance,
            expected_revenue=expected_revenue,
            recommendations=structured_recs,
            model_name=state.predictor.config.model_name,
            model_version=state.predictor.config.model_version,
            predicted_at=datetime.now().isoformat()
        )
        
        logger.info(f"Prediction completed for event_id={request.event_id}, probability={probability:.2%}")
        
        return response
        
    except Exception as e:
        logger.error(f"Prediction error: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Prediction failed: {str(e)}")

@app.post("/predict/batch", response_model=BatchPredictionResponse)
async def predict_batch(request: BatchPredictionRequest):
    """
    Batch prediction for multiple events
    """
    if not state.model_loaded:
        raise HTTPException(
            status_code=503,
            detail="Model not loaded. Please train or load a model first."
        )
    
    try:
        predictions = []
        
        for event_req in request.events:
            # Reuse single prediction endpoint logic
            event_data = event_req.dict()
            event_data['category'] = event_req.category.value
            event_data['city'] = event_req.city.value
            
            probability, label, recommendations = state.predictor.predict(event_data)
            expected_attendance, expected_revenue = calculate_expected_metrics(
                event_data, probability
            )
            structured_recs = categorize_recommendations(recommendations)
            
            predictions.append(PredictionResponse(
                success=True,
                event_id=event_req.event_id,
                probability=round(probability, 4),
                label=label,
                confidence=calculate_confidence(probability),
                expected_attendance=expected_attendance,
                expected_revenue=expected_revenue,
                recommendations=structured_recs,
                model_name=state.predictor.config.model_name,
                model_version=state.predictor.config.model_version,
                predicted_at=datetime.now().isoformat()
            ))
        
        state.prediction_count += len(predictions)
        
        return BatchPredictionResponse(
            predictions=predictions,
            total_events=len(predictions),
            processed_at=datetime.now().isoformat()
        )
        
    except Exception as e:
        logger.error(f"Batch prediction error: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Batch prediction failed: {str(e)}")

@app.post("/train", response_model=TrainingResponse)
async def train_model(
    request: TrainingRequest,
    background_tasks: BackgroundTasks
):
    """
    Train a new model (async operation)
    """
    if state.training_in_progress:
        raise HTTPException(
            status_code=409,
            detail="Training already in progress"
        )
    
    def train_task():
        try:
            state.training_in_progress = True
            logger.info(f"Starting model training with {request.n_samples} samples...")
            
            # Generate data
            df = generate_synthetic_data(n_samples=request.n_samples)
            
            # Configure and train
            config = ModelConfig(
                enable_feature_engineering=request.enable_feature_engineering
            )
            predictor = EventSuccessPredictor(config)
            predictor.train(df, tune_hyperparameters=request.tune_hyperparameters)
            
            # Save model
            predictor.save()
            
            # Update state
            state.predictor = predictor
            state.model_loaded = True
            
            logger.info("Model training completed successfully")
            
        except Exception as e:
            logger.error(f"Training error: {str(e)}")
        finally:
            state.training_in_progress = False
    
    # Start training in background
    background_tasks.add_task(train_task)
    
    return TrainingResponse(
        success=True,
        message="Training started in background",
        metrics=None,
        model_path="models/event_success_model.pkl",
        trained_at=datetime.now().isoformat()
    )

@app.get("/model/info", response_model=ModelInfo)
async def get_model_info():
    """Get current model information"""
    if not state.model_loaded:
        raise HTTPException(
            status_code=404,
            detail="No model loaded"
        )
    
    try:
        # Load metrics if available
        metrics_path = Path(state.predictor.config.metrics_path)
        metrics = None
        if metrics_path.exists():
            with open(metrics_path, 'r') as f:
                metrics = json.load(f)
        
        return ModelInfo(
            model_name=state.predictor.config.model_name,
            model_version=state.predictor.config.model_version,
            trained_at=metrics.get('trained_at') if metrics else None,
            features_count=len(state.predictor.feature_columns),
            metrics=metrics,
            config={
                "n_estimators": state.predictor.config.n_estimators,
                "max_depth": state.predictor.config.max_depth,
                "feature_engineering": state.predictor.config.enable_feature_engineering
            }
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/model/reload")
async def reload_model():
    """Reload model from disk"""
    try:
        state.load_model()
        return {
            "success": True,
            "message": "Model reloaded successfully",
            "model_loaded": state.model_loaded
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to reload model: {str(e)}")

@app.get("/stats")
async def get_stats():
    """Get service statistics"""
    uptime = (datetime.now() - state.start_time).total_seconds()
    
    return {
        "total_predictions": state.prediction_count,
        "uptime_seconds": uptime,
        "uptime_hours": round(uptime / 3600, 2),
        "model_loaded": state.model_loaded,
        "training_in_progress": state.training_in_progress,
        "predictions_per_hour": round(state.prediction_count / (uptime / 3600), 2) if uptime > 0 else 0
    }

# ============================================================================
# RUN APPLICATION
# ============================================================================

if __name__ == "__main__":
    uvicorn.run(
        "ml_service:app",
        host="0.0.0.0",
        port=5000,
        reload=True,  # Disable in production
        log_level="info"
    )