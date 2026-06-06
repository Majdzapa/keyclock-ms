# ═══════════════════════════════════════════════════════════
# Frontend Dockerfile — Multi-stage build
# Node 20 (build) → Nginx Alpine (serve)
# ═══════════════════════════════════════════════════════════

# Stage 1: Build React app
FROM node:20-alpine AS builder

WORKDIR /app

# Copy package files first for dependency caching
COPY package.json package-lock.json* ./
RUN npm ci --silent

# Copy source and build
COPY . .

# Pass build-time Keycloak URL via build arg
ARG VITE_KEYCLOAK_URL=http://localhost:8080
ARG VITE_KEYCLOAK_REALM=certmanager-realm
ARG VITE_KEYCLOAK_CLIENT_ID=certmanager-frontend
ARG VITE_API_URL=http://localhost:8081/api/v1

ENV VITE_KEYCLOAK_URL=$VITE_KEYCLOAK_URL
ENV VITE_KEYCLOAK_REALM=$VITE_KEYCLOAK_REALM
ENV VITE_KEYCLOAK_CLIENT_ID=$VITE_KEYCLOAK_CLIENT_ID
ENV VITE_API_URL=$VITE_API_URL

RUN npm run build

# Stage 2: Serve with Nginx
FROM nginx:1.27-alpine AS runtime

# Copy built assets
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy custom Nginx config for SPA routing
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Non-root: nginx master runs as root by design, workers as nginx user
EXPOSE 80

HEALTHCHECK --interval=10s --timeout=3s --start-period=5s --retries=3 \
    CMD wget -q --spider http://localhost:80 || exit 1

CMD ["nginx", "-g", "daemon off;"]
