window.addEventListener("DOMContentLoaded", () => {
    const urlParams = new URLSearchParams(window.location.search);
    const movieId = urlParams.get('id');
    
    if (!movieId) {
        document.body.innerHTML = "<h1>Error: Movie ID not provided</h1>";
        return;
    }

    fetch(`/api/single-movie?id=${encodeURIComponent(movieId)}`)
        .then(response => response.json())
        .then(movie => {
            document.getElementById("movie-title").textContent = movie.title;
            document.getElementById("movie-year").textContent = movie.year;
            document.getElementById("movie-director").textContent = movie.director;

            // Genres (already an array)
            document.getElementById("movie-genres").textContent = Array.isArray(movie.genres)
                ? movie.genres.join(", ")
                : "N/A";

            // Stars (already an array)
            const stars = Array.isArray(movie.stars) ? movie.stars : [];
            const starLinks = stars.length
                ? stars.map(star => `<a href="single-star.html?name=${encodeURIComponent(star.trim())}">${star.trim()}</a>`).join(", ")
                : "N/A";
            document.getElementById("movie-stars").innerHTML = starLinks;

            document.getElementById("movie-rating").textContent = (movie.rating !== undefined)
                ? Number(movie.rating).toFixed(1)
                : "N/A";
        })
        .catch(error => {
            console.error("Error fetching movie details:", error);
            document.body.innerHTML = "<h1>Error loading movie details</h1>";
        });
});
