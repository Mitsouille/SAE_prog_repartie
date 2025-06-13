const url = "http://localhost:8080/data/reserver";
const reservation = {
    idTable: 1,
    prenom: "Jean",
    nom: "Dupont",
    nbConvives: 4,
    tel: "0601020304",
    debut: "2025-06-13T22:00:00",
    fin: "2025-06-13T23:00:00"
};

fetch(url, {
    method: "POST",
    headers: {
        "Content-Type": "application/json"
    },
    body: JSON.stringify(reservation)
})
.then(response => {
    if (!response.ok) {
        throw new Error("Erreur HTTP : " + response.status);
    }
    return response.json();
})
.then(data => {
    console.log("Réservation réussie :", data);
})
.catch(error => {
    console.error("Erreur lors de la requête :", error);
});
