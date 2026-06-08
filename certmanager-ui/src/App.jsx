import React, { useState, useEffect } from 'react'
import { useAuth } from 'oidc-react'
import { parseAccessToken } from './auth/authConfig'

// Components
import NavBar from './components/NavBar'
import CertList from './pages/CertList'
import UploadPage from './pages/UploadPage'
import FetchPage from './pages/FetchPage'

export default function App() {
  const auth = useAuth()
  const [page, setPage] = useState('list')

  // Auto-redirect to Keycloak when not authenticated (covers logout too)
  useEffect(() => {
    if (!auth.isLoading && !auth.userData) {
      auth.signIn()
    }
  }, [auth.isLoading, auth.userData])

  // Still initialising or about to redirect — show nothing meaningful
  if (auth.isLoading || !auth.userData) {
    return <p style={{ padding: 24 }}>Redirecting to login…</p>
  }

  const username = auth.userData.profile?.preferred_username ?? ''
  const { roles, permissions } = parseAccessToken(auth.userData)
  const admin    = roles.includes('ROLE_ADMIN')
  const canRead  = admin || permissions.includes('READ')
  const canWrite =  admin || permissions.includes('WRITE')

  return (
    <div className="app">
      <NavBar page={page} setPage={setPage} username={username} canWrite={canWrite} canRead={canRead} />

      <main>
        {page === 'list'   && (canRead ? <CertList canRead={canRead} /> : <p>You do not have permission to read certificates.</p>)}
        {page === 'upload' && (canWrite ? <UploadPage /> : <p>You do not have permission to upload.</p>)}
        {page === 'fetch'  && (canWrite ? <FetchPage /> : <p>You do not have permission to fetch.</p>)}
      </main>
    </div>
  )
}
