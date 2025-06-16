import { map } from './map.js';

export function loadStations() {
  return fetch('https://api.cyclocity.fr/contracts/nancy/gbfs/v2/station_information.json')
    .then(r => r.json())
    .then(info => fetch('https://api.cyclocity.fr/contracts/nancy/gbfs/v2/station_status.json')
      .then(r => r.json())
      .then(status => {
        info.data.stations.forEach(station => {
          const st = status.data.stations.find(s => s.station_id === station.station_id);
          if (!st) return;
          const color = st.num_bikes_available > 5 ? 'green' : st.num_bikes_available > 0 ? 'orange' : 'red';
          const marker = L.circleMarker([station.lat, station.lon], {
            color, fillColor: color, fillOpacity: 0.7, radius: 8
          }).addTo(map);
          marker.bindPopup(`<div class="popup-content"><div class="popup-title">${station.name}</div><div class="popup-info">Vélos : ${st.num_bikes_available}</div><div class="popup-info">Places : ${st.num_docks_available}</div></div>`);
        });

        document.getElementById('station-count').textContent = info.data.stations.length;
        document.getElementById('bike-count').textContent = status.data.stations.reduce((a, s) => a + s.num_bikes_available, 0);
        document.getElementById('dock-count').textContent = status.data.stations.reduce((a, s) => a + s.num_docks_available, 0);
      }))
    .catch(e => console.error(e));
}
