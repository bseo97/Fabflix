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

            // Add to Cart button event
            const addToCartBtn = document.getElementById('addToCartBtn');
            addToCartBtn.addEventListener('click', function() {
                addToCart(movieId, addToCartBtn);
            });
        })
        .catch(error => {
            console.error("Error fetching movie details:", error);
            document.body.innerHTML = "<h1>Error loading movie details</h1>";
        });

    // Add to Cart logic (same as main.html)
    function getCart() {
        return JSON.parse(localStorage.getItem('cart') || '{}');
    }
    function setCart(cart) {
        localStorage.setItem('cart', JSON.stringify(cart));
    }
    function addToCart(movieId, button) {
        const cart = getCart();
        cart[movieId] = (cart[movieId] || 0) + 1;
        setCart(cart);

        fetch('/api/cart', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ cart })
        })
        .then(response => {
            if (!response.ok) throw new Error('Failed to add to cart');
            return response.json();
        })
        .then(data => {
            button.textContent = '✓ Added to cart!';
            setTimeout(() => {
                button.textContent = 'Add to Cart';
            }, 2000);
        })
        .catch(error => {
            console.error('Error:', error);
            button.textContent = 'Add to Cart';
        });
    }
});
