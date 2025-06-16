import { map } from './map.js';
import { getApiBase } from './config.js';

export let selectedRestaurantId = null;

export function loadRestaurants() {
  return fetch(`${getApiBase()}/data/restaurants`)
    .then(r => r.json())
    .then(data => {
      data.restaurants.forEach(r => {
        const marker = L.marker([r.latitude, r.longitude]).addTo(map);
        marker.on('click', () => {
          selectedRestaurantId = r.id;
          marker.bindPopup(`
            <div class="popup-content">
              <div class="popup-title">${r.nom}</div>
              <div class="popup-info">${r.adresse}</div>
              <div class="popup-info">Note : ${r.note.toFixed(1)}</div>
              <button onclick="openModal()">Réserver</button>
              <button onclick="openCancelModal()">Annuler une réservation</button>
            </div>
          `).openPopup();
        });
      });
    })
    .catch(console.error);
}
