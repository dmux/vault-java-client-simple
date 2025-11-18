package com.github.dmux.vault.integration;

/**
 * Marker interface for integration tests.
 * Used with JUnit @Category annotation to separate integration tests from unit tests.
 * 
 * Integration tests can be run separately using Maven profiles:
 * - Unit tests only: mvn test
 * - Integration tests: mvn test -Pintegration
 */
public interface IntegrationTest {
    // Marker interface - no methods needed
}
