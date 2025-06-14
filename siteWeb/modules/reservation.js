import { selectedRestaurantId } from './restaurants.js';
import { getApiBase } from './config.js';

export function setupReservationForm() {
  const form = document.getElementById("reservationForm");

  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    const body = {
      idRestaurant: selectedRestaurantId,
      nbConvives: parseInt(form.nbConvives.value),
      prenom: form.prenom.value,
      nom: form.nom.value,
      tel: form.tel.value,
      debut: form.debut.value,
      fin: form.fin.value
    };

    const res = await fetch(`${getApiBase()}/data/placesDisponibles?idRestaurant=${selectedRestaurantId}&debut=${body.debut}&fin=${body.fin}`);
    const dispo = await res.text();
    if (parseInt(dispo) < body.nbConvives) {
      alert("Pas assez de places disponibles.");
      return;
    }

    fetch(`${getApiBase()}/data/reserver`, {
      method: "POST", headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body)
    })
    .then(r => r.json())
    .then(d => {
      alert(typeof d.message === "string" ? d.message : d.success ? "Réservation réussie." : "Réservation échouée.");
      closeModal();
    })
    .catch(() => alert("Erreur lors de la réservation"));
  });
}

export function setupCancelForm() {
  document.getElementById("cancelReservationForm").addEventListener("submit", (e) => {
    e.preventDefault();
    const form = e.target;

    let debut = form.debut.value;
    if (!debut.match(/\d{2}:\d{2}:\d{2}$/)) debut += ":00";

    const body = {
      prenom: form.prenom.value,
      nom: form.nom.value,
      telephone: form.tel.value,
      debut
    };

    fetch(`${getApiBase()}/data/annuler`, {
      method: "POST", headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body)
    })
    .then(r => r.json())
    .then(d => {
      alert(d.message || "Annulation traitée");
      closeCancelModal();
    })
    .catch(() => alert("Erreur d'annulation"));
  });
}
