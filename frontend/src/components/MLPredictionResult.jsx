import React from 'react';
import './MLPredictionResult.css';

/**
 * Component to display ML prediction results in a visually appealing format
 */
const MLPredictionResult = ({ result }) => {
  if (!result) return null;

  const {
    success,
    probability,
    label,
    confidence,
    expectedAttendance,
    expectedRevenue,
    recommendations = [],
    modelName,
    modelVersion,
    predictedAt,
    eventId,
  } = result;

  const confidenceClass = (confidence || 'medium').toLowerCase();
  const formatPercent = (value) =>
    typeof value === 'number' ? `${(value * 100).toFixed(1)}%` : '—';
  const formatNumber = (value) =>
    typeof value === 'number' ? value.toLocaleString() : '—';
  const formatCurrency = (value) =>
    typeof value === 'number' && value > 0
      ? new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value)
      : 'Free';

  const getStatusLabel = () => {
    if (label === 1) return 'Likely to succeed';
    if (label === 0) return 'At risk';
    return 'Unknown';
  };

  const getStatusColor = () => {
    if (label === 1) return 'success';
    if (label === 0) return 'warning';
    return 'neutral';
  };

  return (
    <div className="ml-prediction-result">
      {/* Header Section */}
      <div className="ml-header">
        <div className="ml-header-content">
          <div className="ml-model-info">
            <span className="ml-model-name">{modelName || 'Unknown Model'}</span>
            {modelVersion && <span className="ml-model-version">v{modelVersion}</span>}
          </div>
          <h3 className={`ml-title ${success ? 'success' : 'fallback'}`}>
            {success ? '✨ ML Prediction Ready' : '⚠️ Heuristic Forecast'}
          </h3>
          <div className="ml-meta-info">
            <span className={`ml-status status-${getStatusColor()}`}>{getStatusLabel()}</span>
            {predictedAt && (
              <span className="ml-timestamp">
                {new Date(predictedAt).toLocaleString()}
              </span>
            )}
          </div>
        </div>
        <div className={`ml-confidence-badge confidence-${confidenceClass}`}>
          {confidence || 'MEDIUM'}
        </div>
      </div>

      {/* Key Metrics */}
      <div className="ml-metrics">
        <div className="ml-metric-card">
          <div className="ml-metric-label">Success Probability</div>
          <div className="ml-metric-value probability">
            {formatPercent(probability)}
          </div>
          <div className="ml-metric-bar">
            <div
              className="ml-metric-fill"
              style={{ width: `${(probability || 0) * 100}%` }}
            />
          </div>
        </div>

        <div className="ml-metric-card">
          <div className="ml-metric-label">Expected Attendance</div>
          <div className="ml-metric-value attendance">
            {formatNumber(expectedAttendance)}
          </div>
          <div className="ml-metric-subtext">people</div>
        </div>

        <div className="ml-metric-card">
          <div className="ml-metric-label">Expected Revenue</div>
          <div className="ml-metric-value revenue">
            {formatCurrency(expectedRevenue)}
          </div>
          {expectedRevenue > 0 && <div className="ml-metric-subtext">estimated</div>}
        </div>
      </div>

      {/* Recommendations */}
      {recommendations && recommendations.length > 0 && (
        <div className="ml-recommendations-section">
          <h4 className="ml-recommendations-title">
            <span className="ml-recommendations-icon">💡</span>
            Recommendations
          </h4>
          <div className="ml-recommendations-list">
            {recommendations.map((rec, idx) => (
              <div
                key={`${rec.category}-${idx}`}
                className={`ml-recommendation-item priority-${(rec.priority || 'medium').toLowerCase()}`}
              >
                <div className="ml-rec-header">
                  <span className={`ml-rec-priority priority-${(rec.priority || 'medium').toLowerCase()}`}>
                    {rec.priority || 'MEDIUM'}
                  </span>
                  <span className="ml-rec-category">{rec.category}</span>
                </div>
                <p className="ml-rec-message">{rec.message}</p>
                {rec.impact && (
                  <div className="ml-rec-impact">
                    <strong>Impact:</strong> {rec.impact}
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Footer Info */}
      {eventId && (
        <div className="ml-footer">
          <span className="ml-footer-text">Event ID: {eventId}</span>
        </div>
      )}
    </div>
  );
};

export default MLPredictionResult;

