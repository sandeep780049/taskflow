import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// In dev the dashboard runs on :5173 and proxies the API to the backend on :8080.
// In production the build is emitted into the Spring Boot static resources so a
// single service serves API + dashboard.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
      '/actuator': 'http://localhost:8080',
    },
  },
  build: {
    outDir: '../src/main/resources/static',
    emptyOutDir: true,
  },
});
