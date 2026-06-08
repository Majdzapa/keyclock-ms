import React from 'react'
import { keycloakLogout } from '../auth/authConfig'

export default function NavBar({ page, setPage, username, canWrite, canRead }) {
  return (
    <nav>
      <span className="brand">CertManager</span>
      <div className="nav-links">
        {canRead && <button onClick={() => setPage('list')}   className={page === 'list'   ? 'active' : ''}>Certificates</button>}
        {canRead && <button onClick={() => setPage('search')} className={page === 'search' ? 'active' : ''}>Search</button>}
        {canWrite   && <button onClick={() => setPage('upload')} className={page === 'upload' ? 'active' : ''}>Upload</button>}
        {canWrite   && <button onClick={() => setPage('fetch')}  className={page === 'fetch'  ? 'active' : ''}>Fetch URL</button>}
      </div>
      <div className="nav-right">
        <span>{username}</span>
        <button onClick={keycloakLogout}>Logout</button>
      </div>
    </nav>
  )
}
