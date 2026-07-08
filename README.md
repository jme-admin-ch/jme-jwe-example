# jme-jwe-example

Example project demonstrating transparent JWE end-to-end encryption between an Angular frontend
and a Spring Boot backend using the
[jEAP JWE starter](https://github.com/jeap-admin-ch/jeap-spring-boot-jwe-starter) and its frontend
companion [@jeap/jeap-jwe-client](https://www.npmjs.com/package/@jeap/jeap-jwe-client).

The example is a self-contained system (SCS) and consists of the following modules:

* **jme-jwe-scs**: The self-contained system
    * **jme-jwe-scs-web**: Spring Boot backend using the jEAP JWE starter together with the
      jEAP security starter (OAuth2 resource server) and the jEAP web config starter
    * **jme-jwe-scs-ui**: Angular frontend using [Oblique](https://oblique.bit.admin.ch/) and the
      jEAP JWE client, packaged into the backend jar as static resources
* **jme-jwe-auth-scs**: An instance of the
  [jEAP OAuth mock server](https://github.com/jeap-admin-ch/jeap-oauth-mock-server) used as
  authorization server
* **jme-jwe-test**: Automated integration tests covering the local Docker Compose setup

## Showcased cases

| Case | Endpoint | Transport |
|------|----------|-----------|
| Encrypted POST | `POST /api/persons` | Request body encrypted as compact JWE (`application/jose`), response encrypted with the request-local response key |
| Encrypted GET | `GET /api/persons` | Response encrypted with the key from the `JWE-Response-Key` header |
| Unencrypted request with allowlist | `GET /api/public/info` | Plain JSON — `/api/public/**` is listed in `jeap.jwe.filter.excluded-paths`, but the endpoint still requires a valid Bearer token |

Encryption and decryption are transparent on both sides: the Spring controllers only see plain
JSON, and the Angular services use the ordinary `HttpClient`. The jEAP JWE servlet filter and the
`jeapJweInterceptor` handle the JWE protocol, driven by the configuration the backend publishes at
`/.well-known/jwe-configuration` (see `jeap.jwe.*` in
[`application.yml`](jme-jwe-scs/jme-jwe-scs-web/src/main/resources/application.yml)).

Key management is backed by the HashiCorp Vault transit secret engine: the Docker Compose setup
creates an exportable `rsa-4096` transit key and rotates it once (see
[`docker/docker-compose.yml`](docker/docker-compose.yml)), so two key versions exist. The backend
serves both public keys at `/.well-known/jwks.json` with the newest version first
(`jme-jwe-scs-key:2`), the frontend encrypts against the newest one, and payloads encrypted with
the previous version (`jme-jwe-scs-key:1`) are still decrypted — the rotation grace that keeps
clients with a cached JWKS working across a key rotation.

## Changes

This library adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).
Changes are documented in [CHANGELOG.md](CHANGELOG.md).

## Prerequisites

- JDK 25 or later
- Docker (for Vault)
- Node.js 22 / npm (only needed to run the frontend independently with `ng serve`)
- Google Chrome (used headless by the browser-based integration test in `jme-jwe-test`)
- Use the maven wrapper `./mvnw` to build the project

## Getting started

### Infrastructure

Start Vault (dev mode, with the transit engine and JWE key created automatically):

```shell
docker compose -f docker/docker-compose.yml up
```

Vault is published on `http://localhost:8201` (root token `root`) to avoid clashing with other
locally running Vault instances.

### Build

```shell
./mvnw install
```

This also runs the frontend build and tests via npm (an `npm ci` is triggered automatically when
`node_modules` is missing).

### Start

Start the OAuth mock server first, then the SCS:

```shell
./mvnw --projects jme-jwe-auth-scs spring-boot:run -Dspring-boot.run.profiles=local
./mvnw --projects jme-jwe-scs/jme-jwe-scs-web spring-boot:run -Dspring-boot.run.profiles=local
```

Then open http://localhost:8080/jme-jwe-scs/ and log in as the predefined user. Open the browser's
network tab to see the encrypted requests and responses (`application/jose`) as well as the plain
allowlisted request.

### Independent UI

To develop the frontend with a live dev server, start the backend as described above and run:

```shell
cd jme-jwe-scs/jme-jwe-scs-ui
npm start
```

The dev server on http://localhost:4200 proxies all backend calls to `http://localhost:8080`
(see [`proxy.conf.js`](jme-jwe-scs/jme-jwe-scs-ui/proxy.conf.js)).

## Integration tests

The `jme-jwe-test` module covers the Docker Compose setup with automated tests: it uses Spring
Boot's Docker Compose support to automatically start and stop the Vault infrastructure, then
starts the two Spring Boot services as Maven subprocesses via `mvnw spring-boot:run` and polls
their health endpoints. The tests then exercise the JWE protocol end-to-end with real
Vault-backed keys: encrypted POST, encrypted GET, the unencrypted allowlisted request, plaintext
rejection, the JWKS endpoint serving both Vault transit key versions (newest first), and the
rotation grace — an encrypted request using the previous key version is still accepted.

The module also contains a browser-based end-to-end test (`JweExampleBrowserIT`) that drives the
real Angular UI in headless Chrome with [Playwright](https://playwright.dev/java/): it logs in
through the OAuth mock server's login page (authorization code flow with PKCE, exactly as a user
would) and then asserts on the captured network traffic that `/api/persons` is transported as
`application/jose` (request body encrypted in the browser, response a compact JWE) while the
allowlisted `/api/public/info` stays plain `application/json`. Playwright uses the locally
installed Google Chrome (`chrome` channel), so no browser download is required.

```shell
# Build and install all local modules (required: the services are forked from the local build)
./mvnw install -pl '!:jme-jwe-test'
# Run all integration tests
./mvnw verify -pl jme-jwe-test
# Run only the browser-based test
./mvnw verify -pl jme-jwe-test -Dit.test=JweExampleBrowserIT
# Skip the integration tests in a full build
./mvnw install -Dskip.failsafe.tests=true
```

Ensure Docker is running and the ports 8080, 8081 and 8201 are available.

The `jme-jwe-scs-web` module additionally contains an integration test that runs without any
infrastructure by using static JWE test keys (`jeap.jwe.test.enabled=true` with
`jeap-spring-boot-jwe-test`).

### Running on CI

On CI, the `CI` environment variable activates the `ci` Spring profile, which layers
[`docker-compose-ci.yml`](docker/docker-compose-ci.yml) over the compose file and reaches the
containers through an isolated Docker network instead of published localhost ports.

## Note

This repository is part of the open source distribution of jEAP. See
[github.com/jeap-admin-ch/jeap](https://github.com/jeap-admin-ch/jeap#licenses-and-copyright) for
more information.

## License

This repository is Apache-2.0 licensed, see [LICENSE](LICENSE).
