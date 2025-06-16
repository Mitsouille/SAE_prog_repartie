import { map } from './modules/map.js';
import { loadStations } from './modules/stations.js';
import { loadIncidents } from './modules/incidents.js';
import { loadRestaurants } from './modules/restaurants.js';
import { loadWeather } from './modules/weather.js';
import { setApiBase } from './modules/config.js';
import {
  openModal,
  closeModal,
  openCancelModal,
  closeCancelModal,
  setupReservationForm,
  setupCancelForm
} from './modules/reservation.js';

window.openModal = openModal;
window.closeModal = closeModal;
window.openCancelModal = openCancelModal;
window.closeCancelModal = closeCancelModal;

document.addEventListener("DOMContentLoaded", () => {
  const input = document.getElementById("serverUrl");
  const button = document.getElementById("applyServerBtn");

  if (input && button) {
    button.addEventListener("click", () => {
      setApiBase(input.value);
      loadRestaurants();
      loadWeather();
      loadStations();
      loadIncidents();
    });
  }

  loadRestaurants();
  loadWeather();
  loadStations();
  loadIncidents();
  setupReservationForm();
  setupCancelForm();

  setTimeout(() => {
    if (map && typeof map.invalidateSize === 'function') {
      map.invalidateSize();
    }
  }, 100);
});
