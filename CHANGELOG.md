# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.0] - 2026-07-06

### Dependencies
- **org.codehaus.mojo:exec-maven-plugin**: 3.6.2 → 3.6.3 (patch)
- **ch.admin.bit.jeap:jeap-oauth-mock-server**: 6.1.0 → 6.2.0 (minor)
- **com.nimbusds:nimbus-jose-jwt**: 10.9 → 10.9.1 (patch)

## [1.0.0] - 2026-07-03

### Added

- Initial version of the jEAP JWE example
- jme-jwe-scs: Self-contained system with a Spring Boot backend using the jEAP JWE starter
  and an Angular/Oblique frontend using the jEAP JWE client
- jme-jwe-auth-scs: OAuth mock server instance used as authorization server
- jme-jwe-test: Automated integration tests covering the local Docker Compose setup with Vault
