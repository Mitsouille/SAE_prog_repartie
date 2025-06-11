const url = "http://localhost:8080/incidents";

fetch(url)
    .then(response => {
        if (!response.ok) {
            throw new Error("Erreur HTTP : " + response.status);
        }
        return response.json();
    })
    .then(data => {
        console.log("Réponse du serveur :", data);
    })
    .catch(error => {
        console.error("Erreur lors de la requête :", error);
    });