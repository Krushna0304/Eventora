import React from 'react';
import MLPredictionResult from './MLPredictionResult';
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

  return (
    <div className="dialog-overlay" onClick={onClose}>
      <div className="dialog-content" onClick={(e) => e.stopPropagation()}>
        {mlResult ? (
          <MLPredictionResult result={mlResult} />
        ) : (
          renderDefaultMessage()
        )}

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