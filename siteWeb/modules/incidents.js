import { map } from './map.js';
import { getApiBase } from './config.js';

const icon = L.icon({
  iconUrl: 'https://cdn-icons-png.flaticon.com/512/595/595067.png',
  iconSize: [32, 32],
  iconAnchor: [16, 32],
  popupAnchor: [0, -32]
});

const formatDate = (iso) => new Date(iso).toLocaleString('fr-FR', {
  day: 'numeric', month: 'long', year: 'numeric',
  hour: '2-digit', minute: '2-digit'
});

export function loadIncidents() {
  return fetch(`${getApiBase()}/incidents`)
    .then(r => r.json())
    .then(data => data.incidents.forEach(i => {
      const [lat, lon] = i.location.polyline.trim().split(/\s+/).map(parseFloat);
      if (!isNaN(lat) && !isNaN(lon)) {
        const m = L.marker([lat, lon], { icon }).addTo(map);
        m.bindPopup(`<div class="popup-content"><div class="popup-title">${i.short_description}</div><div class="popup-info">${i.description}</div><div class="popup-info">${i.location.location_description}</div><div class="popup-info">Du ${formatDate(i.starttime)} au ${formatDate(i.endtime)}</div></div>`);
      }
    }))
    .catch(console.error);
}
