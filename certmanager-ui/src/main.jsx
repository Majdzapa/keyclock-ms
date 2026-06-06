import React from 'react'
import ReactDOM from 'react-dom/client'
import { AuthProvider } from 'oidc-react'
import { oidcConfig } from './auth/authConfig'
import App from './App'
import './index.css'

ReactDOM.createRoot(document.getElementById('root')).render(
  <AuthProvider {...oidcConfig}>
    <App />
  </AuthProvider>
)
