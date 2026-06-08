# Frontend Setup Guide

If you want to completely recreate the frontend project from scratch, here is the step-by-step guide to scaffolding a modern React application using Vite and setting up the OpenID Connect (Keycloak) authentication that you currently have.

### Step 1: Scaffold the Vite Project
First, open your terminal, navigate to your main `keyclock-ms` folder, and run the Vite creation script.

```bash
cd /Users/hedfimajd/Documents/pictet/keyclock-ms
npm create vite@latest new-certmanager-ui -- --template react
```
*(This creates a new folder called `new-certmanager-ui` with a blank React project).*

### Step 2: Install Dependencies
Navigate into your new folder and install the base React packages, plus the libraries you need for Keycloak, routing, and API calls.

```bash
cd new-certmanager-ui
npm install
npm install oidc-client-ts react-oidc-context axios react-router-dom
```

### Step 3: Setup Environment Variables
Create a file named `.env` in the root of `new-certmanager-ui` (next to `package.json`). Add the environment variables your app needs to talk to Keycloak and your backend:

```env
VITE_KEYCLOAK_URL=http://localhost:8080
VITE_KEYCLOAK_REALM=certmanager-realm
VITE_KEYCLOAK_CLIENT_ID=certmanager-frontend
VITE_API_URL=http://localhost:8081/api/v1
```

### Step 4: Recreate the Auth Configuration
Create your authentication configuration file. 
1. Inside `src/`, create a folder called `auth/`.
2. Inside `src/auth/`, create `authConfig.js` and paste your existing config:

```javascript
export const oidcConfig = {
  authority: `${import.meta.env.VITE_KEYCLOAK_URL}/realms/${import.meta.env.VITE_KEYCLOAK_REALM}`,
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID,
  redirectUri: window.location.origin,
  scope: 'openid profile email',
  autoSignIn: false,
  onSignIn: () => window.history.replaceState({}, document.title, window.location.pathname)
}

export function keycloakLogout() {
  Object.keys(sessionStorage).filter(k => k.startsWith('oidc.user:')).forEach(k => sessionStorage.removeItem(k));
  const base = import.meta.env.VITE_KEYCLOAK_URL;
  const realm = import.meta.env.VITE_KEYCLOAK_REALM;
  const clientId = import.meta.env.VITE_KEYCLOAK_CLIENT_ID;
  const redirectUri = encodeURIComponent(window.location.origin);
  window.location.href = `${base}/realms/${realm}/protocol/openid-connect/logout?client_id=${clientId}&post_logout_redirect_uri=${redirectUri}`;
}
```

### Step 5: Wrap the Application in the Auth Provider
Open `src/main.jsx` and wrap your React app with the `AuthProvider` from `react-oidc-context` so Keycloak state is available globally:

```javascript
import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.jsx'
import './index.css'
import { AuthProvider } from 'react-oidc-context'
import { oidcConfig } from './auth/authConfig'

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <AuthProvider {...oidcConfig}>
      <App />
    </AuthProvider>
  </React.StrictMode>,
)
```

### Step 6: Create the Login UI in App.jsx
Finally, update your `src/App.jsx` to show a login button if the user is not authenticated, or the main application if they are:

```javascript
import { useAuth } from 'react-oidc-context';
import { keycloakLogout } from './auth/authConfig';

function App() {
  const auth = useAuth();

  if (auth.isLoading) {
    return <div>Loading...</div>;
  }

  if (auth.error) {
    return <div>Oops... {auth.error.message}</div>;
  }

  if (auth.isAuthenticated) {
    return (
      <div>
        <h1>Welcome, {auth.user?.profile.preferred_username}</h1>
        <p>Your Token: {auth.user?.access_token.substring(0, 20)}...</p>
        <button onClick={() => keycloakLogout()}>Log out</button>
      </div>
    );
  }

  return (
    <div>
      <h1>CertManager Login</h1>
      <button onClick={() => void auth.signinRedirect()}>Log in</button>
    </div>
  );
}

export default App;
```

### Step 7: Start the Server
Run the new application to make sure it works!

```bash
npm run dev
```
