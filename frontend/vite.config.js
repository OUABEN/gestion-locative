
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      '/api/locataires': {
        target: 'http://127.0.0.1:8082',
        changeOrigin: true,
      },
      '/api/baux': {
        target: 'http://127.0.0.1:8082',
        changeOrigin: true,
      },
      '/api/quittances': {
        target: 'http://127.0.0.1:8084',
        changeOrigin: true,
        rewrite: (path) =>
          path.replace(/^\/api\/quittances(?=\/|$)/, ''),
      },
      '/api': {
        target: 'http://127.0.0.1:8081',
        changeOrigin: true,
      },
    },
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/tests/setup.js',
    include: ['src/tests/**/*.test.{js,jsx}'],
    reporters: ['default', 'junit'],
    outputFile: {
      junit: '../docs/rapports/biens/TEST-FrontendBiens.xml',
    },
    coverage: {
      provider: 'istanbul',
      reporter: ['text', 'html', 'lcov', 'json-summary'],
      reportsDirectory: '../docs/qualite/biens/istanbul-frontend',
      include: ['src/**/*.{js,jsx}'],
      exclude: ['src/main.jsx', 'src/tests/**'],
    },
  },
});
