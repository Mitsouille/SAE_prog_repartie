export let apiBase = "https://localhost:8443";

export function setApiBase(url) {
  apiBase = url;
}

export function getApiBase() {
  return apiBase;
}
