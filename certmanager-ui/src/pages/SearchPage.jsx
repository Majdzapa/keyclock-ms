import React, { useState } from 'react'

export default function SearchPage({ onView }) {
  const [searchId, setSearchId] = useState('')

  const handleSearch = (e) => {
    e.preventDefault()
    if (searchId) {
      onView(searchId)
    }
  }

  return (
    <div>
      <h2>Search Certificate by ID</h2>
      <form onSubmit={handleSearch} style={{ display: 'flex', gap: '10px', marginTop: '20px' }}>
        <input 
          type="number" 
          placeholder="Enter Certificate ID" 
          value={searchId} 
          onChange={e => setSearchId(e.target.value)} 
          required
          style={{ padding: '8px', width: '250px' }}
        />
        <button type="submit">Search</button>
      </form>
    </div>
  )
}
