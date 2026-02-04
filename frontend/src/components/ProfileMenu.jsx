import React from 'react';
import './ProfileMenu.css';

const ProfileMenu = ({ isOpen, onClose, userInfo, onLogout, onSwitchToOrganizer, onMyEvents, switchLabel = 'Switch to Organizer' }) => {
  if (!isOpen) return null;

  return (
    <>
      <div className="profile-menu-overlay" onClick={onClose} />
      <div className="profile-menu">
        <div className="profile-header">
          <div className="profile-info">
            <h3>{userInfo.displayName}</h3>
            <p>{userInfo.email}</p>
          </div>
        </div>
        
        <div className="profile-menu-items">
          <button className="menu-item" onClick={onSwitchToOrganizer}>
            {switchLabel}
          </button>
          <button className="menu-item" onClick={onLogout}>
            Logout
          </button>
        </div>
      </div>
    </>
  );
};

export default ProfileMenu;