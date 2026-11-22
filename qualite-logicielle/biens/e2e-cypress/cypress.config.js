const { defineConfig } = require('cypress');

module.exports = defineConfig({
  e2e: {
    baseUrl: process.env.CYPRESS_BASE_URL || 'http://localhost:3005',
    viewportWidth: 1280,
    viewportHeight: 800,
    video: true,
    videosFolder: '../../docs/qualite/biens/cypress/videos',
    screenshotsFolder: '../../docs/qualite/biens/cypress/screenshots',
    reporter: 'mochawesome',
    reporterOptions: {
      reportDir: '../../docs/qualite/biens/cypress',
      overwrite: false,
      html: true,
      json: true,
    },
    setupNodeEvents(on, config) {
      // Configuration des plugins Cypress
    },
  },
});
