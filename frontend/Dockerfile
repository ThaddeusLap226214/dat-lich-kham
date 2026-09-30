# syntax=docker/dockerfile:1
# Giai đoạn 1: build Angular ra file tĩnh. Giai đoạn 2: nginx phục vụ file tĩnh (vài chục MB RAM).
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci --no-audit --no-fund
COPY . .
# Giới hạn RAM của trình build để không chiếm hết máy ảo Docker
ENV NODE_OPTIONS=--max-old-space-size=1536
RUN npx ng build --configuration production

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/fe/browser /usr/share/nginx/html
