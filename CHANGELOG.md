# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [7.2.0] - 2026-08-19

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 39.3.0 → 39.5.0 (minor)
- **ch.admin.bit.jeap:jeap-oauth-mock-server**: 9.0.0 → 9.1.0 (minor)
- **ch.admin.bit.jeap.jme:jme-spring-boot-integration-test**: 5.13.0 → 5.14.0 (minor)
- **typescript-eslint**: 8.66.0 → 8.67.0 (minor)
- **eslint**: 10.8.0 → 10.8.1 (patch)
- **@oblique/toolchain**: 15.4.3 → 15.4.4 (patch)
- **@oblique/oblique**: 15.4.3 → 15.4.4 (patch)
- **@jeap/jeap-jwe-client**: 1.2.0 → 1.3.4 (minor)
- **@jeap/jeap-frontend-license-checker**: 1.0.1 → 1.0.2 (patch)

## [7.1.1] - 2026-08-19

### Changed
- Replaced the frontend license checker with `@jeap/jeap-frontend-license-checker`. The generated third-party notices now carry the full license texts of the redistributed dependencies, and the license steps run as part of the npm frontend build.
## [7.1.0] - 2026-08-18

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 39.0.1 → 39.3.0 (minor)
- **ch.admin.bit.jeap.jme:jme-spring-boot-integration-test**: 5.12.0 → 5.13.0 (minor)
- **typescript-eslint**: 8.66.0 → 8.67.0 (minor)
- **eslint**: 10.8.0 → 10.8.1 (patch)
- **@oblique/toolchain**: 15.4.3 → 15.4.4 (patch)
- **@oblique/oblique**: 15.4.3 → 15.4.4 (patch)
- **@jeap/jeap-jwe-client**: 1.2.0 → 1.3.0 (minor)

## [7.0.0] - 2026-08-17

### Changed
- UI: migrated `eslint.config.mjs` from the `FlatCompat` eslintrc shim to native
  ESLint flat config using `typescript-eslint` and the `angular-eslint` premade
  configs. This drops the undeclared `@eslint/js` / `@eslint/eslintrc` imports
  that ESLint 10 no longer provides transitively.
- UI: removed the redundant `@angular-eslint/eslint-plugin`,
  `@angular-eslint/eslint-plugin-template`, `@angular-eslint/template-parser`,
  `@angular-eslint/utils`, `@typescript-eslint/eslint-plugin` and
  `@typescript-eslint/parser` dev dependencies — they are provided by the
  `angular-eslint` and `typescript-eslint` umbrella packages.
- UI: kept `angular-eslint` on 21.x; the 22.x line targets Angular 22 and does
  not match the Angular 21 version used here.

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 39.0.0 → 39.0.1 (patch)
- **ch.admin.bit.jeap:jeap-oauth-mock-server**: 8.6.0 → 9.0.0 (major)
- **eslint**: 10.8.0 → 10.8.1 (patch)
- **@typescript-eslint/parser**: 8.66.0 → 8.67.0 (minor)
- **@typescript-eslint/eslint-plugin**: 8.66.0 → 8.67.0 (minor)

## [6.0.0] - 2026-08-13

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 38.1.0 → 39.0.0 (major)
- **ch.admin.bit.jeap:jeap-oauth-mock-server**: 8.2.0 → 8.6.0 (minor)
- **ch.admin.bit.jeap.jme:jme-spring-boot-integration-test**: 5.8.1 → 5.12.0 (minor)
- **eslint**: 10.8.0 → 10.8.1 (patch)
- **@typescript-eslint/parser**: 8.66.0 → 8.67.0 (minor)
- **@typescript-eslint/eslint-plugin**: 8.66.0 → 8.67.0 (minor)
- **@babel/core**: 7.29.7 → 8.0.1 (major)

## [5.0.0] - 2026-08-06

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 38.0.1 → 38.1.0 (minor)
- **ch.admin.bit.jeap:jeap-oauth-mock-server**: 8.0.1 → 8.2.0 (minor)
- **ch.admin.bit.jeap.jme:jme-spring-boot-integration-test**: 5.8.0 → 5.8.1 (patch)
- **com.microsoft.playwright:playwright**: 1.61.0 → 1.62.0 (minor)
- **prettier**: 3.9.4 → 3.9.6 (patch)
- **npm**: 11.7.0 → 12.0.2 (major)
- **fast-uri**: 3.1.5 → 4.1.2 (major)
- **eslint-config-prettier**: 9.1.2 → 10.1.8 (major)
- **eslint**: 9.39.4 → 10.8.0 (major)
- **angular-oauth2-oidc**: 20.0.3 → 22.0.2 (major)
- **angular-eslint**: 21.4.0 → 22.1.0 (major)
- **ajv**: 8.18.0 → 8.20.0 (minor)
- **@typescript-eslint/parser**: 8.62.1 → 8.66.0 (minor)
- **@typescript-eslint/eslint-plugin**: 8.62.1 → 8.66.0 (minor)
- **@oblique/toolchain**: 15.4.0 → 15.4.3 (patch)
- **@oblique/oblique**: 15.4.0 → 15.4.3 (patch)
- **@babel/core**: 7.29.7 → 8.0.1 (major)

## [4.1.1] - 2026-08-04

### Security
- Update Angular to 21.2.19 and fast-uri to 3.1.5.

## [4.1.0] - 2026-08-02

### Dependencies
- **ch.admin.bit.jeap:jeap-oauth-mock-server**: 8.0.0 → 8.0.1 (patch)
- **ch.admin.bit.jeap.jme:jme-spring-boot-integration-test**: 5.7.5 → 5.8.0 (minor)

## [4.0.1] - 2026-07-30

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 38.0.0 → 38.0.1 (patch)

## [4.0.0] - 2026-07-29

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 37.7.0 → 38.0.0 (major)
- **ch.admin.bit.jeap:jeap-oauth-mock-server**: 7.3.0 → 8.0.0 (major)
- **ch.admin.bit.jeap.jme:jme-spring-boot-integration-test**: 5.7.4 → 5.7.5 (patch)

## [3.5.0] - 2026-07-28

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 37.6.0 → 37.7.0 (minor)

## [3.4.0] - 2026-07-26

### Dependencies
- **ch.admin.bit.jeap:jeap-oauth-mock-server**: 7.2.0 → 7.3.0 (minor)
- **ch.admin.bit.jeap.jme:jme-spring-boot-integration-test**: 5.7.3 → 5.7.4 (patch)

## [3.3.0] - 2026-07-24

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 37.5.0 → 37.6.0 (minor)

## [3.2.1] - 2026-07-23

### Dependencies
- **ch.admin.bit.jeap.jme:jme-spring-boot-integration-test**: 5.7.2 → 5.7.3 (patch)

## [3.2.0] - 2026-07-23

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 37.2.0 → 37.5.0 (minor)
- **ch.admin.bit.jeap:jeap-oauth-mock-server**: 7.1.0 → 7.2.0 (minor)

## [3.1.1] - 2026-07-22

### Dependencies
- **ch.admin.bit.jeap.jme:jme-spring-boot-integration-test**: 5.7.1 → 5.7.2 (patch)

## [3.1.0] - 2026-07-22

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 37.0.1 → 37.2.0 (minor)
- **ch.admin.bit.jeap:jeap-oauth-mock-server**: 7.0.0 → 7.1.0 (minor)

## [3.0.1] - 2026-07-22

### Security
- **fast-uri** (transitive npm dependency): 3.1.3 → 3.1.4, fixes CVE-2026-16221

## [3.0.0] - 2026-07-21

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 37.0.0 → 37.0.1 (patch)
- **ch.admin.bit.jeap:jeap-oauth-mock-server**: 6.7.0 → 7.0.0 (major)

## [2.0.0] - 2026-07-21

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 36.10.0 → 37.0.0 (major)

## [1.11.1] - 2026-07-20

### Dependencies
- **ch.admin.bit.jeap.jme:jme-spring-boot-integration-test**: 5.7.0 → 5.7.1 (patch)

## [1.11.0] - 2026-07-19

### Dependencies
- **ch.admin.bit.jeap:jeap-oauth-mock-server**: 6.6.1 → 6.7.0 (minor)

## [1.10.0] - 2026-07-18

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 36.9.0 → 36.10.0 (minor)

## [1.9.0] - 2026-07-16

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 36.8.0 → 36.9.0 (minor)
- **ch.admin.bit.jeap:jeap-oauth-mock-server**: 6.5.0 → 6.6.1 (minor)
- **ch.admin.bit.jeap.jme:jme-spring-boot-integration-test**: 5.6.0 → 5.7.0 (minor)

## [1.8.0] - 2026-07-15

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 36.7.0 → 36.8.0 (minor)
- **ch.admin.bit.jeap:jeap-oauth-mock-server**: 6.4.0 → 6.5.0 (minor)

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
