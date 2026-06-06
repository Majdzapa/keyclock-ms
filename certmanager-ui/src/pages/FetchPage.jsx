import React, { useState } from 'react'
import { certificateApi } from '../api/certificateApi'

export default function FetchPage() {
  const [url,   setUrl]   = useState('')
  const [port,  setPort]  = useState('443')
  const [msg,   setMsg]   = useState('')
  const [error, setError] = useState('')
  const [busy,  setBusy]  = useState(false)

  const handleSubmit = e => {
    e.preventDefault()
    setBusy(true); setMsg(''); setError('')
    certificateApi.fetchFromUrl(url, port)
      .then(() => setMsg('Certificate fetched and saved.'))
      .catch(() => setError('Fetch failed. Check the host and port.'))
      .finally(() => setBusy(false))
  }

  return (
    <div>
      <h2>Fetch Certificate from URL</h2>
      {error && <p className="error">{error}</p>}
      {msg   && <p className="success">{msg}</p>}
      <form onSubmit={handleSubmit}>
        <div className="field">
          <label>Host</label>
          <input type="text" placeholder="example.com" value={url}
                 onChange={e => setUrl(e.target.value)} required />
        </div>
        <div className="field">
          <label>Port</label>
          <input type="number" value={port}
                 onChange={e => setPort(e.target.value)} required />
        </div>
        <button type="submit" disabled={!url || busy}>{busy ? 'Fetching…' : 'Fetch'}</button>
      </form>
    </div>
  )
}
