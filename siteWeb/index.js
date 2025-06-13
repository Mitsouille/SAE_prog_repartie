import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
});

const map = L.map('map').setView([48.683331, 6.2], 13);

L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
  attribution: '© OpenStreetMap contributors'
}).addTo(map);

const incidentIcon = L.icon({
  iconUrl: 'https://cdn-icons-png.flaticon.com/512/595/595067.png',
  iconSize: [32, 32],
  iconAnchor: [16, 32],
  popupAnchor: [0, -32]
});

const formatDate = (iso) => {
  const date = new Date(iso);
  return date.toLocaleString('fr-FR', {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
};

function loadStations() {
  fetch('https://api.cyclocity.fr/contracts/nancy/gbfs/v2/station_information.json')
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

function loadIncidents() {
  fetch('https://100.64.80.245:8443/incidents')
    .then(r => r.json())
    .then(d => d.incidents.forEach(i => {
      const [lat, lon] = i.location.polyline.trim().split(/\s+/).map(parseFloat);
      if (isNaN(lat) || isNaN(lon)) return;
      const m = L.marker([lat, lon], { icon: incidentIcon }).addTo(map);
      m.bindPopup(`<div class="popup-content"><div class="popup-title">${i.short_description}</div><div class="popup-info">${i.description}</div><div class="popup-info">${i.location.location_description}</div><div class="popup-info">Du ${formatDate(i.starttime)} au ${formatDate(i.endtime)}</div></div>`);
    }))
    .catch(e => console.error(e));
}

function loadWeather() {
  fetch("https://wttr.in/Nancy?format=%C+%t&lang=fr")
    .then(r => r.text())
    .then(data => document.getElementById("weather").textContent = data)
    .catch(() => document.getElementById("weather").textContent = "Indisponible");
}

let selectedRestaurantId = null;ù

function loadRestaurants() {
  fetch("https://100.64.80.245:8443/data/restaurants")
    .then(r => r.json())
    .then(data => {
      data.restaurants.forEach(r => {
        const marker = L.marker([r.latitude, r.longitude]).addTo(map);
        marker.on('click', () => {
          selectedRestaurantId = r.id;
          marker.bindPopup(`<div class="popup-content"><div class="popup-title">${r.nom}</div><div class="popup-info">${r.adresse}</div><div class="popup-info">Note : ${r.note.toFixed(1)}</div><button onclick="openModal()">Réserver</button></div>`).openPopup();
        });
      });
    })
    .catch(e => console.error(e));
}

window.openModal = () => {
  document.getElementById("reservationModal").style.display = "flex";
};

window.closeModal = () => {
  document.getElementById("reservationModal").style.display = "none";
};

function envoyerReservation(body) {
  return fetch("https://100.64.80.245:8443/data/reserver", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body)
  }).then(r => r.json());
}

function annulerReservation(body) {
  return fetch("https://100.64.80.245:8443/data/annuler", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body)
  }).then(r => r.json());
}

document.addEventListener("DOMContentLoaded", () => {
  const form = document.getElementById("reservationForm");

  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    const body = {
      idTable: 1,
      prenom: form.prenom.value,
      nom: form.nom.value,
      tel: form.tel.value,
      nbConvives: parseInt(form.nbConvives.value),
      debut: form.debut.value,
      fin: form.fin.value
    };

    const res = await fetch(`https://100.64.80.245:8443/data/placesDisponibles?idRestaurant=${selectedRestaurantId}&debut=${body.debut}&fin=${body.fin}`);
    const dispo = await res.text();
    if (parseInt(dispo) < body.nbConvives) {
      alert("Pas assez de places disponibles.");
      return;
    }

    envoyerReservation(body)
      .then(d => {
        const msg = typeof d.message === "string" ? d.message :
                    d.success ? "Réservation réussie." : "Réservation échouée.";
        alert(msg);
        closeModal();
      })
      .catch(() => alert("Erreur lors de la réservation"));
  });

  document.getElementById("cancelBtn").addEventListener("click", () => {
    const tel = form.tel.value;
    const debut = form.debut.value;
    if (!tel || !debut) {
      alert("Remplis téléphone et date pour annuler");
      return;
    }

    annulerReservation({ telephone: tel, debut })
      .then(d => {
        alert(d.message || "Annulation traitée");
        closeModal();
      })
      .catch(() => alert("Erreur d'annulation"));
  });

  loadRestaurants();
  loadWeather();
  loadStations();
  loadIncidents();
});
