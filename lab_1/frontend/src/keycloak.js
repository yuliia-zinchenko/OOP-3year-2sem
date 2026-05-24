import Keycloak from "keycloak-js";

export const keycloak = new Keycloak({
  url: "http://localhost:8081",
  realm: "library",
  clientId: "library-frontend",
});

export async function initAuth() {
  const ok = await keycloak.init({
    onLoad: "login-required",
    checkLoginIframe: false,
  });
  return ok;
}
