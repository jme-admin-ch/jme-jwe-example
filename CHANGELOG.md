# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.7.0] - 2026-07-14

### Dependencies
- **ch.admin.bit.jeap.jme:jme-spring-boot-integration-test**: 5.5.0 → 5.6.0 (minor)

## [1.6.0] - 2026-07-14

### Dependencies
- **ch.admin.bit.jeap:jeap-oauth-mock-server**: 6.3.0 → 6.4.0 (minor)

## [1.5.0] - 2026-07-11

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 36.6.0 → 36.7.0 (minor)

## [1.4.0] - 2026-07-10

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 36.3.1 → 36.6.0 (minor)
- **ch.admin.bit.jeap:jeap-oauth-mock-server**: 6.2.0 → 6.3.0 (minor)

## [1.3.1] - 2026-07-08

### Added

- Browser-based end-to-end test (`JweExampleBrowserIT`) in jme-jwe-test: drives the Angular UI in
  headless Chrome with Playwright, logs in through the OAuth mock server and verifies on the wire
  that the encrypted endpoints transport `application/jose` while the allowlisted endpoint stays
  plain JSON.
- "Decrypt JWE" demo view: a new Angular route where a compact JWE captured from the network tab
  can be pasted and decrypted by the backend (`POST /api/demo/decrypt`), displaying the colored
  JWE segments, the protected header and the recovered plaintext. Response JWEs (`alg: dir`) are
  decrypted by additionally supplying the `JWE-Response-Key` header of the same request, from
  which the backend recovers the response CEK. Strictly a demo feature — the endpoint is a
  deliberate decryption oracle. Covered by the module and browser integration tests.

## [1.3.0] - 2026-07-08

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 36.2.2 → 36.3.1 (minor)
- **ch.admin.bit.jeap.jme:jme-spring-boot-integration-test**: 5.4.0 → 5.5.0 (minor)

## [1.2.1] - 2026-07-07

### Fixed

- The frontend no longer hardcodes the /jme-jwe-scs context path: index.html is served with the
  `<base href>` rewritten to the servlet context path, and the frontend derives its API paths from
  the base href. This fixes the broken static resource loading (403) in downstream instances of this
  example that run under a different context path (e.g. jme-nivel-jwe-example, jme-rhos-jwe-example).

## [1.2.0] - 2026-07-06

### Changed

- jme-jwe-scs-web is now published as a plain library jar (Spring Boot repackaging skipped, like in
  jme-jwe-auth-scs and the process context example). This allows downstream instances of this example
  (e.g. jme-nivel-jwe-example) to depend on the module as a library — classes inside a repackaged fat
  jar cannot be loaded as a dependency.

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
