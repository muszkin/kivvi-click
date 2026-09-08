# Journey: login

The store owner can sign in with an e-mail address and see their identity in the panel shell

Source tests: tests/e2e/specs/public.spec.ts (login), tests/Controller/SecurityControllerTest.php

Run id placeholder: `__RUN__` (per-run value normalized to `<RUN-ID>`).

## Steps

1. `{"goto":"/pl/login"}`
2. `{"fill":"#f-_username","value":"anna@aureashop.pl"}`
3. `{"fill":"#f-_password","value":"haslo-testowe"}`
4. `{"click":"button[type=\"submit\"]","waitUrl":"/pl/dashboard"}`
5. `{"post":"/pl/login","form":{"_username":"","_password":""},"note":"empty e-mail rejected server-side"}`
6. `{"post":"/pl/login","form":{"_username":"not-an-email"},"note":"malformed e-mail rejected server-side"}`
7. `{"post":"/pl/logout","form":{},"note":"sign-out restores default identity"}`
8. `{"goto":"/pl/dashboard"}`
