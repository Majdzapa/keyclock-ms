import React, { useState, useEffect } from 'react'
import { certificateApi } from '../api/certificateApi'

export default function CertList({ admin, onView }) {
  const [certs,   setCerts]   = useState([])
  const [loading, setLoading] = useState(true)
  const [error,   setError]   = useState('')

  const load = () => {
    setLoading(true); setError('')
    certificateApi.list()
      .then(setCerts)
      .catch(() => setError('Failed to load certificates.'))
      .finally(() => setLoading(false))
  }

  useEffect(load, [])

  const handleDelete = (id, name) => {
    if (!window.confirm(`Delete "${name}"?`)) return
    certificateApi.delete(id).then(load).catch(() => setError('Delete failed.'))
  }

  if (loading) return <p>Loading…</p>

  return (
    <div>
      <h2>Certificates</h2>
      {error && <p className="error">{error}</p>}
      {certs.length === 0 ? <p>No certificates found.</p> : (
        <table>
          <thead>
            <tr>
              <th>Filename</th><th>Owner</th><th>Expires</th><th>Status</th><th>Group</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {certs.map(c => (
              <tr key={c.id}>
                <td>{c.filename}</td>
                <td>{c.owner ?? '—'}</td>
                <td>{c.expirationDate?.slice(0, 10) ?? '—'}</td>
                <td>
                  {c.isExpired      ? <span className="badge red">Expired</span>
                   : c.isExpiringSoon ? <span className="badge yellow">Expiring Soon</span>
                   :                   <span className="badge green">Valid</span>}
                </td>
                <td>{c.uploadedByGroup ?? '—'}</td>
                <td>
                  <button onClick={() => onView(c.id)}>View</button>
                  {admin && (
                    <>
                      {' '}
                      <a href={certificateApi.downloadUrl(c.id)} download>Download</a>
                      {' '}
                      <button onClick={() => handleDelete(c.id, c.filename)}>Delete</button>
                    </>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}
