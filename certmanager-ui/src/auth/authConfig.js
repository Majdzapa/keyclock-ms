export const oidcConfig = {
  authority:    `${import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8080'}/realms/${import.meta.env.VITE_KEYCLOAK_REALM || 'certmanager-realm'}`,
  clientId:     import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'certmanager-frontend',
  redirectUri:  window.location.origin,
  scope:        'openid profile email',
  // Do NOT use autoSignIn — we handle the login button manually in App.jsx
  autoSignIn:   false,
  onSignIn:     () => window.history.replaceState({}, document.title, window.location.pathname),
}

// Parse roles and permissions from the access token (NOT profile — that's the ID token)
export function parseAccessToken(userData) {
  if (!userData?.access_token) return { roles: [], permissions: [] }
  try {
    const payload = JSON.parse(atob(userData.access_token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')))
    return {
      roles: payload.realm_access?.roles ?? [],
      permissions: payload.permissions ?? []
    }
  } catch { return { roles: [], permissions: [] } }
}

// Logout: clear local session then redirect to Keycloak logout endpoint
export function keycloakLogout() {
  // 1. Clear the oidc-client-ts session from sessionStorage
  Object.keys(sessionStorage)
    .filter(k => k.startsWith('oidc.user:'))
    .forEach(k => sessionStorage.removeItem(k))

  // 2. Redirect to Keycloak logout (clears Keycloak SSO session too)
  const base     = import.meta.env.VITE_KEYCLOAK_URL    || 'http://localhost:8080'
  const realm    = import.meta.env.VITE_KEYCLOAK_REALM  || 'certmanager-realm'
  const clientId = import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'certmanager-frontend'
  const redirectUri = encodeURIComponent(window.location.origin)
  window.location.href = `${base}/realms/${realm}/protocol/openid-connect/logout?client_id=${clientId}&post_logout_redirect_uri=${redirectUri}`
}
