
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
    .then(response => response.json())
    .then(infoData => {
      return fetch('https://api.cyclocity.fr/contracts/nancy/gbfs/v2/station_status.json')
        .then(response => response.json())
        .then(statusData => {
          const stations = infoData.data.stations;
          const statuses = statusData.data.stations;

          let totalBikes = 0;
          let totalDocks = 0;

          stations.forEach(station => {
            const status = statuses.find(s => s.station_id === station.station_id);
            if (!status) return;

            const color = status.num_bikes_available > 5 ? 'green' :
                          status.num_bikes_available > 0 ? 'orange' : 'red';

            const marker = L.circleMarker([station.lat, station.lon], {
              color: color,
              fillColor: color,
              fillOpacity: 0.7,
              radius: 8
            }).addTo(map);

            marker.bindPopup(`
              <div class="popup-content">
                <div class="popup-title">${station.name}</div>
                <div class="popup-info">Vélos disponibles : ${status.num_bikes_available}</div>
                <div class="popup-info">Places libres : ${status.num_docks_available}</div>
              </div>
            `);

            totalBikes += status.num_bikes_available;
            totalDocks += status.num_docks_available;
          });

          document.getElementById('station-count').textContent = stations.length;
          document.getElementById('bike-count').textContent = totalBikes;
          document.getElementById('dock-count').textContent = totalDocks;
        });
    })
    .catch(error => {
      console.error('Erreur stations:', error);
      alert('Impossible de charger les données vélo');
    });
}

function loadIncidents() {
  fetch('http://localhost:8080/incidents')
    .then(response => response.json())
    .then(data => {
      const incidents = data.incidents;
      incidents.forEach(incident => {
        const coords = incident.location.polyline.trim().split(/\s+/);
        const lat = parseFloat(coords[0]);
        const lon = parseFloat(coords[1]);
        if (isNaN(lat) || isNaN(lon)) return;

        const marker = L.marker([lat, lon], { icon: incidentIcon }).addTo(map);

        marker.bindPopup(`
          <div class="popup-content">
            <div class="popup-title">${incident.short_description}</div>
            <div class="popup-info">${incident.description}</div>
            <div class="popup-info">${incident.location.location_description}</div>
            <div class="popup-info">Du ${formatDate(incident.starttime)} au ${formatDate(incident.endtime)}</div>
          </div>
        `);
      });
    })
    .catch(error => {
      console.error('Erreur incidents:', error);
      alert('Impossible de charger les incidents');
    });
}

function loadWeather() {
  fetch("https://wttr.in/Nancy?format=%C+%t&lang=fr")
    .then(response => response.text())
    .then(data => {
      const weatherElement = document.getElementById('weather');
      if (weatherElement) {
        weatherElement.textContent = data;
      }
    })
    .catch(error => {
      console.error("Erreur météo :", error);
      const weatherElement = document.getElementById('weather');
      if (weatherElement) {
        weatherElement.textContent = "Indisponible";
      }
    });
}

let selectedRestaurantId = null;

function loadRestaurants() {
  fetch("http://localhost:8080/data/restaurants")
    .then(response => response.json())
    .then(data => {
      data.restaurants.forEach(r => {
        const marker = L.marker([r.latitude, r.longitude]).addTo(map);

        marker.on('click', () => {
          marker.bindPopup(`
            <div class="popup-content">
              <div class="popup-title">${r.nom}</div>
              <div class="popup-info">${r.adresse}</div>
              <div class="popup-info">Note : ${r.note.toFixed(1)}</div>
              <button onclick="openModal(${r.id})">Réserver</button>
            </div>
          `).openPopup();
        });
      });
    })
    .catch(error => {
      console.error("Erreur restaurants :", error);
      alert("Impossible de charger les restaurants");
    });
}

window.openModal = (restaurantId) => {
  selectedRestaurantId = restaurantId;
  document.getElementById("reservationModal").style.display = "flex";
};

window.closeModal = () => {
  document.getElementById("reservationModal").style.display = "none";
};

document.getElementById("reservationForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  const form = e.target;

  const body = {
    idTable: 1,
    prenom: form.prenom.value,
    nom: form.nom.value,
    tel: form.tel.value,
    nbConvives: parseInt(form.nbConvives.value),
    debut: form.debut.value,       
    fin: form.fin.value
  };

  const dispoRes = await fetch(`http://localhost:8080/data/placesDisponibles?idRestaurant=${selectedRestaurantId}&debut=${body.debut}&fin=${body.fin}`);
  const dispo = await dispoRes.text();
  if (parseInt(dispo) < body.nbConvives) {
    alert("❌ Pas assez de places disponibles.");
    return;
  }

  fetch("http://localhost:8080/data/reservations", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body)
  })
    .then(res => res.json())
    .then(data => {
      alert(data.message);
      closeModal();
    })
    .catch(err => {
      console.error("Erreur réservation:", err);
      alert("Erreur lors de la réservation");
    });
});

loadRestaurants();
loadWeather();
loadStations();
loadIncidents();
