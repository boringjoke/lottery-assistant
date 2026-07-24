FROM node:24-alpine AS build

WORKDIR /workspace

COPY lottery-frontend/package.json lottery-frontend/package-lock.json ./

RUN npm ci

COPY lottery-frontend/ ./

RUN npm run build

FROM nginx:stable-alpine

COPY lottery-frontend/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/dist /usr/share/nginx/html

EXPOSE 80
