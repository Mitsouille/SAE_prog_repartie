export function loadWeather() {
  return fetch("https://wttr.in/Nancy?format=%C+%t&lang=fr")
    .then(r => r.text())
    .then(data => document.getElementById("weather").textContent = data)
    .catch(() => document.getElementById("weather").textContent = "Indisponible");
}
