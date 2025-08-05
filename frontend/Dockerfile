# Production stage
FROM node:18-alpine

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install all dependencies (including dev dependencies for build)
RUN npm ci

# Copy source code
COPY . .

# ARG를 통해 빌드 시점에 환경변수 전달
ARG VITE_API_BASE_URL
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

# Build the app for production (이제 환경변수가 제대로 포함됨)
RUN npm run build

# Install serve to serve static files
RUN npm install -g serve

# Expose port
EXPOSE 5173

# Serve the built app
CMD ["serve", "-s", "dist", "-l", "5173"] 