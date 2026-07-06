# jme-jwe-scs-ui

Angular frontend of the jEAP JWE example, built with [Oblique](https://oblique.bit.admin.ch/) and
[@jeap/jeap-jwe-client](https://www.npmjs.com/package/@jeap/jeap-jwe-client).

The relevant integration points:

* [`app-module.ts`](src/app/app-module.ts): registers `provideJeapJweClient` (pointing at the
  backend's `/.well-known/jwe-configuration`), the `jeapJweInterceptor`, and
  `OAuthModule.forRoot` for the Bearer token.
* [`auth-initializer.ts`](src/app/auth/auth-initializer.ts): loads the OIDC configuration from the
  backend and performs the authorization code flow with PKCE against the OAuth mock server.
* [`person.service.ts`](src/app/person/person.service.ts): plain `HttpClient` calls — the JWE
  interceptor transparently encrypts/decrypts everything matching the backend's included paths.

No local `include`/`exclude` patterns are needed: the backend-published, context-path-prefixed
included and excluded paths are the source of truth for the protect/skip decision, and requests
to the backend origin wait for that configuration instead of being decided locally. Only
`jweConfigPath` is set explicitly — the library derives its default from the base href, which is
the context path (`/jme-jwe-scs/`) in the production build but `/` under `ng serve`, where the
discovery endpoint is reached through the dev proxy instead.

The Maven build (see [`pom.xml`](pom.xml)) runs the frontend tests and the production build via
npm and writes the output to `target/classes/static`, so the frontend is packaged into a jar the
backend serves as static resources.

During development, run `npm start` to serve the app on http://localhost:4200 with a proxy to the
locally running backend (see [`proxy.conf.js`](proxy.conf.js)).
