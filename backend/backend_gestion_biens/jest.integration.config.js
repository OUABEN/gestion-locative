/** @type {import('jest').Config} */
const config = {
  preset: "ts-jest",
  testEnvironment: "node",
  roots: ["<rootDir>/tests/integration"],
  testMatch: ["**/*.integration.test.ts"],
  collectCoverageFrom: [
    "src/**/*.ts",
    "!src/server.ts",
    "!src/**/*.d.ts",
  ],
  coverageDirectory: "<rootDir>/../../docs/rapports/biens/coverage-integration",
  reporters: [
    "default",
    [
      "jest-junit",
      {
        outputDirectory: "../../docs/rapports/biens",
        outputName: "TEST-BiensIntegration.xml",
      },
    ],
    [
      "jest-html-reporters",
      {
        publicPath: "../../docs/rapports/biens",
        filename: "integration-report.html",
        pageTitle: "Rapport Tests d'Intégration - backend_gestion_biens",
        expand: true,
      },
    ],
  ],
};

module.exports = config;
