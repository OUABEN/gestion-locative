const { defineConfig } = require('cypress');

module.exports = defineConfig({
  e2e: {
    baseUrl: process.env.CYPRESS_BASE_URL || 'http://localhost:3000',
    viewportWidth: 1280,
    viewportHeight: 800,
    video: true,
    videosFolder: '../../docs/qualite/locataires/cypress/videos',
    screenshotsFolder: '../../docs/qualite/locataires/cypress/screenshots',
    reporter: 'mochawesome',
    reporterOptions: {
      reportDir: '../../docs/qualite/locataires/cypress',
      overwrite: false,
      html: true,
      json: true,
    },
    setupNodeEvents(on, config) {
      // Configuration des plugins Cypress
    },
  },
});
