import React, { useState, useEffect } from 'react'
import { certificateApi } from '../api/certificateApi'

export default function CertDetails({ id, onBack }) {
  const [cert, setCert] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!id) return;
    setLoading(true)
    certificateApi.getById(id)
      .then(setCert)
      .catch(err => setError(err.response?.data?.message || 'Failed to load details.'))
      .finally(() => setLoading(false))
  }, [id])

  if (loading) return <p>Loading details...</p>
  if (error) return <div><p className="error">{error}</p><button onClick={onBack}>← Back to List</button></div>
  if (!cert) return null

  return (
    <div className="cert-details">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
        <h2>Certificate Details</h2>
        <button onClick={onBack}>← Back to List</button>
      </div>
      
      <div className="details-card" style={{ padding: '20px', border: '1px solid #ccc', borderRadius: '8px', display: 'flex', flexDirection: 'column', gap: '15px' }}>
        <div>
          <strong>Filename:</strong> <br/>
          <span>{cert.filename}</span>
        </div>
        
        {cert.owner && (
          <div>
            <strong>Owner:</strong> <br/>
            <span>{cert.owner}</span>
          </div>
        )}

        <div>
          <strong>Subject:</strong> <br/>
          <span>{cert.subject}</span>
        </div>
        
        <div>
          <strong>Issuer:</strong> <br/>
          <span>{cert.issuer}</span>
        </div>
        
        <div>
          <strong>Serial Number:</strong> <br/>
          <span>{cert.serialNumber}</span>
        </div>
        
        <div style={{ display: 'flex', gap: '40px' }}>
          <div>
            <strong>Valid From:</strong> <br/>
            <span>{cert.validFrom ? new Date(cert.validFrom).toLocaleString() : '—'}</span>
          </div>
          <div>
            <strong>Valid To:</strong> <br/>
            <span>{cert.expirationDate ? new Date(cert.expirationDate).toLocaleString() : '—'}</span>
          </div>
        </div>
        
        <div>
          <strong>Algorithm:</strong> <br/>
          <span>{cert.algorithm}</span>
        </div>
        
        <div>
          <strong>Fingerprint:</strong> <br/>
          <span style={{ fontFamily: 'monospace', background: '#f5f5f5', padding: '4px', borderRadius: '4px', display: 'inline-block', marginTop: '4px' }}>
            {cert.fingerprint}
          </span>
        </div>
        
        {cert.sans && (
          <div>
            <strong>Subject Alternative Names (SANs):</strong> <br/>
            <span style={{ wordBreak: 'break-word' }}>{cert.sans}</span>
          </div>
        )}
      </div>
    </div>
  )
}
