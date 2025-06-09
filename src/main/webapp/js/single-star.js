window.addEventListener("DOMContentLoaded", () => {
    const urlParams = new URLSearchParams(window.location.search);
    const starName = urlParams.get('name');
    
    if (!starName) {
        document.body.innerHTML = "<h1>Error: Star name not provided</h1>";
        return;
    }

    fetch(`/api/single-star?name=${encodeURIComponent(starName)}`)
        .then(response => response.json())
        .then(star => {
            document.getElementById("star-name").textContent = star.name;
            document.getElementById("star-birth-year").textContent = star.birthYear || "N/A";
            
            const moviesList = document.getElementById("star-movies");
            moviesList.innerHTML = "";
            
            if (star.movies && star.movies.length > 0) {
                star.movies.forEach(movie => {
                    const li = document.createElement("li");
                    li.innerHTML = `<a href="single-movie.html?id=${encodeURIComponent(movie.id)}">${movie.title}</a> (${movie.year})`;
                    moviesList.appendChild(li);
                });
            } else {
                moviesList.innerHTML = "<li>No movies found</li>";
            }
        })
        .catch(error => {
            console.error("Error fetching star details:", error);
            document.body.innerHTML = "<h1>Error loading star details</h1>";
        });
}); 