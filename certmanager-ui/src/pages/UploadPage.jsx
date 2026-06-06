import React, { useState } from 'react'
import { certificateApi } from '../api/certificateApi'

export default function UploadPage() {
  const [file,  setFile]  = useState(null)
  const [msg,   setMsg]   = useState('')
  const [error, setError] = useState('')
  const [busy,  setBusy]  = useState(false)

  const handleSubmit = e => {
    e.preventDefault()
    setBusy(true); setMsg(''); setError('')
    certificateApi.upload(file)
      .then(() => { setMsg('Uploaded successfully.'); setFile(null) })
      .catch(() => setError('Upload failed.'))
      .finally(() => setBusy(false))
  }

  return (
    <div>
      <h2>Upload Certificate</h2>
      {error && <p className="error">{error}</p>}
      {msg   && <p className="success">{msg}</p>}
      <form onSubmit={handleSubmit}>
        <div className="field">
          <label>File (.pem .crt .cer .p12 .pfx)</label>
          <input type="file" accept=".pem,.crt,.cer,.p12,.pfx"
                 onChange={e => setFile(e.target.files[0])} />
        </div>
        <button type="submit" disabled={!file || busy}>{busy ? 'Uploading…' : 'Upload'}</button>
      </form>
    </div>
  )
}
