/* movielist.js */
// File: src/main/webapp/js/movielist.js

window.addEventListener("DOMContentLoaded", () => {
    fetch("/fabflix/api/movielist")
        .then(response => response.json())
        .then(data => populateTable(data))
        .catch(error => console.error("Error fetching movie data:", error));
});

function populateTable(movies) {
    const tbody = document.querySelector("#movie_table tbody");
    tbody.innerHTML = "";

    movies.forEach(movie => {
        const row = document.createElement("tr");
        
        // Create star links
        const starLinks = movie.stars?.map(star => 
            `<a href="single-star.html?name=${encodeURIComponent(star)}">${star}</a>`
        ).join(", ") ?? "N/A";

        row.innerHTML = `
            <td><a href="single-movie.html?id=${encodeURIComponent(movie.id)}">${movie.title}</a></td>
            <td>${movie.year}</td>
            <td>${movie.director}</td>
            <td>${movie.genres?.join(", ") ?? "N/A"}</td>
            <td>${starLinks}</td>
            <td>${movie.rating}</td>
        `;

        tbody.appendChild(row);
    });
}
