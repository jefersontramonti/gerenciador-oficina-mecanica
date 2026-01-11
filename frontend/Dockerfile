# Multi-stage build para frontend
FROM node:20-alpine AS build

WORKDIR /app

# Build arguments para Vite (valores default para desenvolvimento local)
ARG VITE_API_URL=http://localhost:8080/api
ARG VITE_WS_URL=ws://localhost:8080/ws
ARG VITE_API_BASE_URL=http://localhost:8080

# Tornar disponíveis para o build do Vite
ENV VITE_API_URL=$VITE_API_URL
ENV VITE_WS_URL=$VITE_WS_URL
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

# Copiar package files para cache de dependências
COPY package.json package-lock.json ./
RUN npm ci

# Copiar código e buildar
COPY . .
RUN npm run build

# Stage final - Nginx para servir arquivos estáticos
FROM nginx:1.25-alpine

# Remover configuração padrão
RUN rm /etc/nginx/conf.d/default.conf

# Copiar build do React
COPY --from=build /app/dist /usr/share/nginx/html

# Copiar configuração customizada do nginx
COPY nginx.conf /etc/nginx/nginx.conf

# Criar usuário não-root
RUN chown -R nginx:nginx /usr/share/nginx/html && \
    chown -R nginx:nginx /var/cache/nginx && \
    chown -R nginx:nginx /var/log/nginx && \
    chown -R nginx:nginx /etc/nginx/conf.d

RUN touch /var/run/nginx.pid && \
    chown -R nginx:nginx /var/run/nginx.pid

USER nginx

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost/health || exit 1

CMD ["nginx", "-g", "daemon off;"]
