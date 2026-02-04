# Eventora Frontend

A modern, professional event management platform built with React and Vite. Eventora enables users to discover, create, and manage events seamlessly with features including event registration, organizer dashboards, and machine learning-powered event predictions.

## Features

- **Event Discovery**: Browse and search events with advanced filtering options
- **User Authentication**: Secure login/registration with OAuth support (Google, GitHub, LinkedIn)
- **Event Management**: Create, edit, and manage events as an organizer
- **Event Registration**: Register for events and manage your event registrations
- **Organizer Dashboard**: Comprehensive dashboard for event organizers with ML predictions
- **Responsive Design**: Modern UI built with Material-UI components

## Technology Stack

- **React 19** - Modern React with latest features
- **Vite** - Fast build tool and development server
- **React Router** - Client-side routing
- **Material-UI (MUI)** - UI component library
- **Axios** - HTTP client for API requests
- **Emotion** - CSS-in-JS styling solution

## Prerequisites

- Node.js (v18 or higher recommended)
- npm or yarn package manager
- Backend API server running (see backend configuration)

## Installation

1. Clone the repository:
```bash
git clone <repository-url>
cd my-react-app
```

2. Install dependencies:
```bash
npm install
```

3. Configure environment variables:
   - Copy `.env.example` to `.env` (if not already present)
   - Update the environment variables in `.env`:
     ```env
     VITE_API_BASE_URL=http://localhost:8080
     VITE_GOOGLE_CLIENT_ID=your_google_client_id
     VITE_GITHUB_CLIENT_ID=your_github_client_id
     VITE_LINKEDIN_CLIENT_ID=your_linkedin_client_id
     VITE_FRONTEND_URL=http://localhost:5173
     ```

## Development

Start the development server:
```bash
npm run dev
```

The application will be available at `http://localhost:5173` (or the port specified in your configuration).

## Building for Production

Build the application for production:
```bash
npm run build
```

The production build will be created in the `dist` directory.

Preview the production build:
```bash
npm run preview
```

## Project Structure

```
src/
├── components/          # React components
│   ├── Home.jsx        # Main event listing page
│   ├── Login.jsx       # Authentication page
│   ├── Register.jsx    # User registration page
│   └── ...
├── services/           # API service layer
│   └── api.js         # Centralized API client
├── config/            # Configuration files
│   └── constants.js   # Application constants
├── utils/             # Utility functions
│   └── oauth.js       # OAuth helper functions
├── App.jsx            # Main application component
└── main.jsx           # Application entry point
```

## API Integration

The application uses a centralized API service layer (`src/services/api.js`) that handles all backend communication. The API client includes:

- Automatic authentication token injection
- Request/response interceptors
- Centralized error handling
- Type-safe API methods

### API Endpoints

- **Authentication**: Login, registration, OAuth callbacks
- **Events**: CRUD operations, filtering, search
- **Registrations**: Event registration and cancellation
- **ML Services**: Event predictions and analytics

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `VITE_API_BASE_URL` | Backend API base URL | `http://localhost:8080` |
| `VITE_GOOGLE_CLIENT_ID` | Google OAuth client ID | - |
| `VITE_GITHUB_CLIENT_ID` | GitHub OAuth client ID | - |
| `VITE_LINKEDIN_CLIENT_ID` | LinkedIn OAuth client ID | - |
| `VITE_FRONTEND_URL` | Frontend application URL | `http://localhost:5173` |

## Code Quality

Run ESLint to check code quality:
```bash
npm run lint
```

## Browser Support

- Chrome (latest)
- Firefox (latest)
- Safari (latest)
- Edge (latest)

## Contributing

1. Create a feature branch
2. Make your changes
3. Ensure code passes linting
4. Submit a pull request

## License

This project is proprietary and confidential.

## Support

For issues or questions, please contact the development team.
