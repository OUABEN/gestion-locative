/** @type {import('jest').Config} */
const config = {
  preset: "ts-jest",
  testEnvironment: "node",
  roots: ["<rootDir>/tests/unit"],
  testMatch: ["**/*.test.ts"],
  collectCoverageFrom: [
    "src/services/**/*.ts",
    "src/middlewares/validation.middleware.ts",
    "!src/**/*.d.ts",
  ],
  coverageDirectory: "<rootDir>/coverage",
  coverageThreshold: {
    global: {
      branches: 85,
      functions: 90,
      lines: 90,
      statements: 90,
    },
  },
  reporters: [
    "default",
    [
      "jest-junit",
      {
        outputDirectory: "target/surefire-reports",
        outputName: "TEST-BienService.xml",
      },
    ],
    [
      "jest-html-reporters",
      {
        publicPath: "target/surefire-reports",
        filename: "report.html",
        pageTitle: "Rapport Tests Unitaires - BienService",
        expand: true,
      },
    ],
  ],
};

module.exports = config;
