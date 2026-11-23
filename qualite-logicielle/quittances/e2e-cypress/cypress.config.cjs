const { defineConfig } = require('cypress');
const path = require('node:path');

const reports = path.resolve(__dirname, '../../../qualite-logicielle/quittances/rapports/cypress');

module.exports = defineConfig({
  video: false,
  screenshotOnRunFailure: true,
  screenshotsFolder: path.join(reports, 'captures'),
  reporter: 'junit',
  reporterOptions: {
    mochaFile: path.join(reports, 'TEST-cypress-[hash].xml'),
    toConsole: true,
  },
  e2e: {
    baseUrl: 'http://127.0.0.1:13003',
    specPattern: 'cypress/e2e/**/*.cy.js',
    supportFile: false,
    retries: 0,
    defaultCommandTimeout: 10000,
  },
});
