import React from 'react';
import './Dialog.css';

const Dialog = ({ message, mlResult, isOpen, onClose, onConfirm, isConfirm }) => {
  if (!isOpen) return null;

  const renderDefaultMessage = () => {
    if (!message) return null;
    if (typeof message === 'string' && message.startsWith('{')) {
      return <pre className="dialog-pre">{message}</pre>;
    }
    return <p>{message}</p>;
  };

  const renderRecommendations = (recommendations = []) => {
    if (!recommendations.length) return null;
    return (
      <div className="ml-recommendations">
        <h4>Recommendations</h4>
        <ul>
          {recommendations.map((rec, idx) => (
            <li key={`${rec.category}-${idx}`}>
              <div className="rec-meta">
                <span className={`rec-priority priority-${(rec.priority || 'medium').toLowerCase()}`}>
                  {rec.priority || 'MEDIUM'}
                </span>
                <span className="rec-category">{rec.category}</span>
              </div>
              <p>{rec.message}</p>
            </li>
          ))}
        </ul>
      </div>
    );
  };

  const renderMlResult = () => {
    if (!mlResult) return null;
    const {
      success,
      probability,
      label,
      confidence,
      expectedAttendance,
      expectedRevenue,
      recommendations,
      modelName,
      modelVersion,
      predictedAt,
    } = mlResult;
    const confidenceClass = (confidence || 'medium').toLowerCase();
    const formatPercent = (value) =>
      typeof value === 'number' ? `${(value * 100).toFixed(1)}%` : '—';
    const formatNumber = (value) =>
      typeof value === 'number' ? value.toLocaleString() : '—';
    const formatCurrency = (value) =>
      typeof value === 'number'
        ? new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value)
        : '—';

    return (
      <div className="ml-dialog">
        <div className="ml-header">
          <div>
            <p className="ml-model">
              {modelName || 'Model'} · v{modelVersion || '—'}
            </p>
            <h3>{success ? 'Forecast ready' : 'Heuristic forecast'}</h3>
            <p className="ml-meta">
              {label === 1 ? 'Likely to succeed' : 'At risk'} · {predictedAt ? new Date(predictedAt).toLocaleString() : '—'}
            </p>
          </div>
          <span className={`ml-pill confidence-${confidenceClass}`}>{confidence || 'MEDIUM'}</span>
        </div>

        <div className="ml-stats">
          <div>
            <p className="label">Probability</p>
            <p className="value">{formatPercent(probability)}</p>
          </div>
          <div>
            <p className="label">Expected attendance</p>
            <p className="value">{formatNumber(expectedAttendance)}</p>
          </div>
          <div>
            <p className="label">Expected revenue</p>
            <p className="value">{formatCurrency(expectedRevenue)}</p>
          </div>
        </div>

        {renderRecommendations(recommendations)}
      </div>
    );
  };

  return (
    <div className="dialog-overlay" onClick={onClose}>
      <div className="dialog-content" onClick={(e) => e.stopPropagation()}>
        {renderMlResult() || renderDefaultMessage()}

        {isConfirm ? (
          <div className="dialog-buttons">
            <button onClick={onConfirm} className="confirm">
              Yes
            </button>
            <button onClick={onClose} className="cancel">
              No
            </button>
          </div>
        ) : (
          <button onClick={onClose}>Close</button>
        )}
      </div>
    </div>
  );
};

export default Dialog;