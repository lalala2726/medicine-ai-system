import type { Config } from 'jest';

const config: Config = {
  testEnvironment: 'jsdom',
  testEnvironmentOptions: {
    url: 'http://localhost:8000',
  },
  setupFiles: ['./tests/setupTests.jsx'],
  transform: {
    '^.+\\.(ts|tsx)$': 'ts-jest',
    '^.+\\.(js|jsx)$': 'ts-jest',
  },
  moduleNameMapper: {
    '^@/(.*)$': '<rootDir>/src/$1',
    '\\.(css|less)$': '<rootDir>/tests/__mocks__/styleMock.js',
  },
  globals: {
    localStorage: null,
  },
};

export default config;
