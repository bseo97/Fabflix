package org.example;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@WebServlet(name = "ChatbotServlet", urlPatterns = "/api/chatbot")
public class ChatbotServlet extends HttpServlet {
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL = "gpt-3.5-turbo";
    private static final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        String apiKey = System.getenv("OPENAI_API_KEY");
        System.out.println("DEBUG: OPENAI_API_KEY=" + apiKey); // Debug log
        if (apiKey == null || apiKey.isEmpty()) {
            response.setStatus(500);
            response.getWriter().write("{\"error\":\"OpenAI API key not set\"}");
            System.out.println("ERROR: OpenAI API key not set"); // Debug log
            return;
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        JsonObject reqJson = gson.fromJson(sb.toString(), JsonObject.class);
        String userMessage = reqJson.has("message") ? reqJson.get("message").getAsString().toLowerCase() : "";
        if (userMessage.isEmpty()) {
            response.setStatus(400);
            response.getWriter().write("{\"error\":\"No message provided\"}");
            System.out.println("ERROR: No message provided"); // Debug log
            return;
        }
        // SAFETY CHECK: Block any write operation attempts
        if (userMessage.matches(".*\\\\b(delete|drop|update|insert|alter|create|truncate|replace|grant|revoke|set)\\\\b.*")) {
            response.getWriter().write("{\"reply\":\"Sorry, for your safety, I can only answer questions and cannot modify the database. If you have any questions about movies, genres, or stars, feel free to ask!\"}");
            return;
        }
        // Handle YES/NO follow-up logic
        String lastFollowUp = reqJson.has("lastFollowUp") ? reqJson.get("lastFollowUp").getAsString() : null;
        if (lastFollowUp != null) {
            if (userMessage.matches("^(k|yes|yeah|yep|sure|of course|please|ok|okay|y|go ahead|why not)[!\\.]?$") && lastFollowUp.equals("most_movies_genre")) {
                try (java.sql.Connection conn = org.example.DatabaseConnection.getConnection()) {
                    String sql = "SELECT g.name, COUNT(gm.movieId) AS movie_count FROM genres g JOIN genres_in_movies gm ON g.id = gm.genreId GROUP BY g.id ORDER BY movie_count DESC LIMIT 1";
                    try (java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
                        try (java.sql.ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) {
                                String genre = rs.getString("name");
                                int count = rs.getInt("movie_count");
                                response.getWriter().write("{\"reply\":\"Awesome! The genre with the most movies is '" + genre + "' with " + count + " movies. Would you like to see a list of movies in this genre?\",\"lastFollowUp\":\"list_movies_in_genre:" + genre + "\"}");
                                return;
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    response.getWriter().write("{\"reply\":\"Sorry, I couldn't fetch the genre with the most movies right now. If you have any other questions, let me know!\"}");
                    return;
                }
            } else if (userMessage.matches("^(no|nope|nah|not now|n|never)[!\\.]?$")) {
                String[] friendly = {"No problem! If you have more questions, just ask! 😊", "Alright, let me know if you want to explore more!", "Got it, I'm here if you need anything else!", "Okay! Feel free to ask about movies, genres, or stars anytime."};
                int idx = (int)(Math.random() * friendly.length);
                response.getWriter().write("{\"reply\":\"" + friendly[idx] + "\"}");
                return;
            } else if (lastFollowUp.startsWith("list_movies_in_genre:")) {
                String genre = lastFollowUp.substring("list_movies_in_genre:".length());
                if (userMessage.matches("^(k|yes|yeah|yep|sure|of course|please|ok|okay|y|go ahead|why not)[!\\.]?$")) {
                    try (java.sql.Connection conn = org.example.DatabaseConnection.getConnection()) {
                        String sql = "SELECT m.title FROM movies m JOIN genres_in_movies gm ON m.id = gm.movieId JOIN genres g ON g.id = gm.genreId WHERE g.name = ? LIMIT 10";
                        try (java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
                            stmt.setString(1, genre);
                            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                                java.util.List<String> movies = new java.util.ArrayList<>();
                                while (rs.next()) {
                                    movies.add(rs.getString("title"));
                                }
                                if (!movies.isEmpty()) {
                                    response.getWriter().write("{\"reply\":\"Here are some movies in the '" + genre + "' genre: " + String.join(", ", movies) + ". If you want more, just ask!\"}");
                                    return;
                                } else {
                                    response.getWriter().write("{\"reply\":\"Sorry, I couldn't find movies in that genre right now. If you have any other questions, feel free to ask!\"}");
                                    return;
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        response.getWriter().write("{\"reply\":\"Sorry, I couldn't fetch movies in that genre right now. If you have any other questions, please ask!\"}");
                        return;
                    }
                } else if (userMessage.matches("^(no|nope|nah|not now|n|never)[!\\.]?$")) {
                    String[] friendly = {"No worries! Let me know if you want to explore something else.", "Alright, just ask if you want to know more!", "Okay! I'm here if you have more questions."};
                    int idx = (int)(Math.random() * friendly.length);
                    response.getWriter().write("{\"reply\":\"" + friendly[idx] + "\"}");
                    return;
                }
            }
        }
        // Friendly intro phrases
        String[] friendlyIntros = {
            "Great question!", "Happy to help!", "Here's what I found!", "Let's see!"
        };
        int introIdx;
        // Custom DB-powered answers (friendly, with follow-ups)
        try {
            // 1. Number of genres
            if (userMessage.matches(".*how (many|much) genres.*|.*number of genres.*|.*genres are there.*|.*list all genres.*|.*what genres.*")) {
                introIdx = (int)(Math.random() * friendlyIntros.length);
                response.getWriter().write("{\"reply\":\"" + friendlyIntros[introIdx] + " There are 22 genres available in Decurb. Want to see which genre has the most movies?\",\"lastFollowUp\":\"most_movies_genre\"}");
                return;
            }
            // 2. Number of movies
            if (userMessage.matches(".*how (many|much) movies.*|.*number of movies.*|.*movies are there.*|.*movies in the database.*|.*list all movies.*")) {
                try (java.sql.Connection conn = org.example.DatabaseConnection.getConnection()) {
                    String sql = "SELECT COUNT(*) AS count FROM movies";
                    try (java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
                        try (java.sql.ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) {
                                int count = rs.getInt("count");
                                introIdx = (int)(Math.random() * friendlyIntros.length);
                                response.getWriter().write("{\"reply\":\"" + friendlyIntros[introIdx] + " There are " + count + " movies available in Decurb. Would you like to see the top-rated movies or explore by genre?\",\"lastFollowUp\":\"top_or_genre\"}");
                                return;
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    response.getWriter().write("{\"reply\":\"Oops! I couldn't fetch the movie count right now.\"}");
                    return;
                }
            }
            // 3. Year a movie was filmed (expanded)
            java.util.regex.Matcher yearMatcher = java.util.regex.Pattern.compile("(when (was|did)|what year (did|was)|which year (did|was)|year of|when is|when did|when was)[^a-zA-Z0-9]*([a-zA-Z0-9 \\-:]+)", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(userMessage);
            if (yearMatcher.find()) {
                String movieTitle = yearMatcher.group(4).replaceAll("[?]", "").replaceAll("(movie|film)", "").replaceAll("[\"'\\\\]", "").trim();
                if (!movieTitle.isEmpty()) {
                    try (java.sql.Connection conn = org.example.DatabaseConnection.getConnection()) {
                        String sql = "SELECT year FROM movies WHERE LOWER(title) = ?";
                        try (java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
                            stmt.setString(1, movieTitle.toLowerCase());
                            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                                if (rs.next()) {
                                    int year = rs.getInt("year");
                                    introIdx = (int)(Math.random() * friendlyIntros.length);
                                    response.getWriter().write("{\"reply\":\"" + friendlyIntros[introIdx] + " From movies in Decurb, " + movieTitle + " was filmed in " + year + ".\"}");
                                    return;
                                } else {
                                    response.getWriter().write("{\"reply\":\"Sorry, I couldn't find that movie in Decurb. If you have any other questions, please do so!\"}");
                                    return;
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        response.getWriter().write("{\"reply\":\"Error querying the database for movie year.\"}");
                        return;
                    }
                }
            }
            // 4. Actor who filmed the most
            if (userMessage.matches(".*(actor filmed the most|most movies|star filmed the most|who acted in the most|who starred in the most|who has the most movies|who appeared in the most).*")) {
                try (java.sql.Connection conn = org.example.DatabaseConnection.getConnection()) {
                    String sql = "SELECT s.name, COUNT(sim.movieId) AS movie_count FROM stars s JOIN stars_in_movies sim ON s.id = sim.starId GROUP BY s.id ORDER BY movie_count DESC LIMIT 1";
                    try (java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
                        try (java.sql.ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) {
                                String name = rs.getString("name");
                                int count = rs.getInt("movie_count");
                                introIdx = (int)(Math.random() * friendlyIntros.length);
                                response.getWriter().write("{\"reply\":\"" + friendlyIntros[introIdx] + " " + name + " filmed the most, with " + count + " movies in Decurb.\"}");
                                return;
                            } else {
                                response.getWriter().write("{\"reply\":\"Sorry, I couldn't find any actors in Decurb. If you have any other questions, Let me know!\"}");
                                return;
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    response.getWriter().write("{\"reply\":\"Error querying the database for actors." + "\"}");
                    return;
                }
            }
            // 5. Price/cost of a movie (expanded)
            java.util.regex.Matcher priceMatcher = java.util.regex.Pattern.compile("(how much( is| does| for| to buy)?|price of|cost of|what is the price of|what does|how much to buy|what's the price of|what is the cost of|what's the cost of|how much does)[^a-zA-Z0-9]*([a-zA-Z0-9 \\-:]+)|how much is ([a-zA-Z0-9 \\-:]+)", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(userMessage);
            if (priceMatcher.find()) {
                String movieTitle = null;
                if (priceMatcher.group(3) != null) {
                    movieTitle = priceMatcher.group(3);
                } else if (priceMatcher.group(4) != null) {
                    movieTitle = priceMatcher.group(4);
                }
                if (movieTitle != null) {
                    movieTitle = movieTitle.replaceAll("[?]", "").replaceAll("(movie|film)", "").replaceAll("[\"'\\\\]", "").trim();
                }
                if (movieTitle != null && !movieTitle.isEmpty()) {
                    try (java.sql.Connection conn = org.example.DatabaseConnection.getConnection()) {
                        String sql = "SELECT price FROM movies WHERE LOWER(title) = ?";
                        try (java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
                            stmt.setString(1, movieTitle.toLowerCase());
                            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                                if (rs.next()) {
                                    double price = rs.getDouble("price");
                                    response.getWriter().write("{\"reply\":\"The price of " + movieTitle + " is $" + String.format("%.2f", price) + " at Decurb. If you have any other questions, feel free to ask!\"}");
                                    return;
                                } else {
                                    response.getWriter().write("{\"reply\":\"Sorry, I couldn't find the price for that movie in Decurb. Do you have any other questions? Let me know!" + "\"}");
                                    return;
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        response.getWriter().write("{\"reply\":\"Error querying the database for movie price." + "\"}");
                        return;
                    }
                }
            }
            // 6. Director of a movie
            java.util.regex.Matcher directorMatcher = java.util.regex.Pattern.compile("(who directed|director of)[^a-zA-Z0-9]*([a-zA-Z0-9 \\-:]+)", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(userMessage);
            if (directorMatcher.find()) {
                String movieTitle = directorMatcher.group(2).replaceAll("[?]", "").replaceAll("(movie|film)", "").replaceAll("[\"'\\\\]", "").trim();
                if (!movieTitle.isEmpty()) {
                    try (java.sql.Connection conn = org.example.DatabaseConnection.getConnection()) {
                        String sql = "SELECT director FROM movies WHERE LOWER(title) = ?";
                        try (java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
                            stmt.setString(1, movieTitle.toLowerCase());
                            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                                if (rs.next()) {
                                    String director = rs.getString("director");
                                    introIdx = (int)(Math.random() * friendlyIntros.length);
                                    response.getWriter().write("{\"reply\":\"" + friendlyIntros[introIdx] + " The director of " + movieTitle + " is " + director + ".\"}");
                                    return;
                                } else {
                                    response.getWriter().write("{\"reply\":\"Sorry, I couldn't find the director for that movie in Decurb. Is there anything else you'd like to ask? Let me know!" + "\"}");
                                    return;
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        response.getWriter().write("{\"reply\":\"Error querying the database for movie director." + "\"}");
                        return;
                    }
                }
            }
            // 7. Genre of a movie
            java.util.regex.Matcher genreMatcher = java.util.regex.Pattern.compile("(what genre is|genre of)[^a-zA-Z0-9]*([a-zA-Z0-9 \\-:]+)", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(userMessage);
            if (genreMatcher.find()) {
                String movieTitle = genreMatcher.group(2).replaceAll("[?]", "").replaceAll("(movie|film)", "").replaceAll("[\"'\\\\]", "").trim();
                if (!movieTitle.isEmpty()) {
                    try (java.sql.Connection conn = org.example.DatabaseConnection.getConnection()) {
                        String sql = "SELECT GROUP_CONCAT(g.name SEPARATOR ', ') AS genres FROM genres g JOIN genres_in_movies gm ON g.id = gm.genreId JOIN movies m ON m.id = gm.movieId WHERE LOWER(m.title) = ? GROUP BY m.id";
                        try (java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
                            stmt.setString(1, movieTitle.toLowerCase());
                            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                                if (rs.next()) {
                                    String genres = rs.getString("genres");
                                    introIdx = (int)(Math.random() * friendlyIntros.length);
                                    response.getWriter().write("{\"reply\":\"" + friendlyIntros[introIdx] + " The genre(s) of " + movieTitle + " is/are: " + genres + ".\"}");
                                    return;
                                } else {
                                    response.getWriter().write("{\"reply\":\"Sorry, I couldn't find the genre for that movie in Decurb. Is there anything else you'd like to ask? Let me know!" + "\"}");
                                    return;
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        response.getWriter().write("{\"reply\":\"Error querying the database for movie genre." + "\"}");
                        return;
                    }
                }
            }
            // 8. Rating of a movie
            java.util.regex.Matcher ratingMatcher = java.util.regex.Pattern.compile("(what is the rating of|rating of|how good is)[^a-zA-Z0-9]*([a-zA-Z0-9 \\-:]+)", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(userMessage);
            if (ratingMatcher.find()) {
                String movieTitle = ratingMatcher.group(2).replaceAll("[?]", "").replaceAll("(movie|film)", "").replaceAll("[\"'\\\\]", "").trim();
                if (!movieTitle.isEmpty()) {
                    try (java.sql.Connection conn = org.example.DatabaseConnection.getConnection()) {
                        String sql = "SELECT rating FROM ratings r JOIN movies m ON r.movieId = m.id WHERE LOWER(m.title) = ?";
                        try (java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
                            stmt.setString(1, movieTitle.toLowerCase());
                            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                                if (rs.next()) {
                                    double rating = rs.getDouble("rating");
                                    introIdx = (int)(Math.random() * friendlyIntros.length);
                                    response.getWriter().write("{\"reply\":\"" + friendlyIntros[introIdx] + " The rating of " + movieTitle + " is " + String.format("%.1f", rating) + ".\"}");
                                    return;
                                } else {
                                    response.getWriter().write("{\"reply\":\"Sorry, I couldn't find the rating for that movie in Decurb. Is there anything else that I can help you with? Let me know!" + "\"}");
                                    return;
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        response.getWriter().write("{\"reply\":\"Error querying the database for movie rating." + "\"}");
                        return;
                    }
                }
            }
            // 9. List all movies by a director
            java.util.regex.Matcher moviesByDirectorMatcher = java.util.regex.Pattern.compile("(movies by|movies directed by|films by|films directed by)[^a-zA-Z0-9]*([a-zA-Z0-9 \\-:]+)", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(userMessage);
            if (moviesByDirectorMatcher.find()) {
                String director = moviesByDirectorMatcher.group(2).replaceAll("[?]", "").replaceAll("[\"'\\\\]", "").trim();
                if (!director.isEmpty()) {
                    try (java.sql.Connection conn = org.example.DatabaseConnection.getConnection()) {
                        String sql = "SELECT title FROM movies WHERE LOWER(director) = ?";
                        try (java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
                            stmt.setString(1, director.toLowerCase());
                            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                                java.util.List<String> movies = new java.util.ArrayList<>();
                                while (rs.next()) {
                                    movies.add(rs.getString("title"));
                                }
                                if (!movies.isEmpty()) {
                                    introIdx = (int)(Math.random() * friendlyIntros.length);
                                    response.getWriter().write("{\"reply\":\"" + friendlyIntros[introIdx] + " Movies by " + director + ": " + String.join(", ", movies) + ".\"}");
                                    return;
                                } else {
                                    response.getWriter().write("{\"reply\":\"Sorry, I couldn't find any movies by " + director + " in Decurb. Do you have any other questions? Feel free to ask!" + "\"}");
                                    return;
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        response.getWriter().write("{\"reply\":\"Error querying the database for movies by director." + "\"}");
                        return;
                    }
                }
            }
            // 10. List all movies by a star
            java.util.regex.Matcher moviesByStarMatcher = java.util.regex.Pattern.compile("(movies with|movies featuring|movies acted by|films with|films featuring|films acted by|movies starring|films starring)[^a-zA-Z0-9]*([a-zA-Z0-9 \\-:]+)", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(userMessage);
            if (moviesByStarMatcher.find()) {
                String star = moviesByStarMatcher.group(2).replaceAll("[?]", "").replaceAll("[\"'\\\\]", "").trim();
                if (!star.isEmpty()) {
                    try (java.sql.Connection conn = org.example.DatabaseConnection.getConnection()) {
                        String sql = "SELECT m.title FROM movies m JOIN stars_in_movies sim ON m.id = sim.movieId JOIN stars s ON s.id = sim.starId WHERE LOWER(s.name) = ?";
                        try (java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
                            stmt.setString(1, star.toLowerCase());
                            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                                java.util.List<String> movies = new java.util.ArrayList<>();
                                while (rs.next()) {
                                    movies.add(rs.getString("title"));
                                }
                                if (!movies.isEmpty()) {
                                    introIdx = (int)(Math.random() * friendlyIntros.length);
                                    response.getWriter().write("{\"reply\":\"" + friendlyIntros[introIdx] + " Movies with " + star + ": " + String.join(", ", movies) + ".\"}");
                                    return;
                                } else {
                                    response.getWriter().write("{\"reply\":\"Sorry, I couldn't find any movies with " + star + " in Decurb. Want to explore something else? I'm here to help!" + "\"}");
                                    return;
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        response.getWriter().write("{\"reply\":\"Error querying the database for movies by star." + "\"}");
                        return;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Fallback: send to OpenAI
        // Prepare OpenAI API request
        JsonObject payload = new JsonObject();
        payload.addProperty("model", MODEL);
        payload.add("messages", gson.toJsonTree(new Object[]{
            new Message("user", userMessage)
        }));
        try {
            URL url = new URL(OPENAI_API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
            }
            int status = conn.getResponseCode();
            InputStream is = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();
            StringBuilder respSb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = br.readLine()) != null) {
                    respSb.append(line);
                }
            }
            System.out.println("DEBUG: OpenAI API raw response: " + respSb.toString()); // Debug log
            JsonObject openaiResp = gson.fromJson(respSb.toString(), JsonObject.class);
            String reply = openaiResp.has("choices") && openaiResp.getAsJsonArray("choices").size() > 0
                ? openaiResp.getAsJsonArray("choices").get(0).getAsJsonObject().getAsJsonObject("message").get("content").getAsString()
                : "Sorry, I couldn't get a response.";
            JsonObject out = new JsonObject();
            out.addProperty("reply", reply);
            response.getWriter().write(out.toString());
        } catch (Exception e) {
            response.setStatus(500);
            response.getWriter().write("{\"error\":\"Failed to contact OpenAI: " + e.getMessage().replace("\"", "'") + "\"}");
            System.out.println("ERROR: Exception while contacting OpenAI"); // Debug log
            e.printStackTrace(); // Print stack trace
        }
    }
    // Helper class for message structure
    static class Message {
        String role;
        String content;
        Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}

@WebServlet(name = "UserInfoServlet", urlPatterns = "/api/user-info")
class UserInfoServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        String jwtToken = org.common.JwtUtil.getCookieValue(request, "jwtToken");
        io.jsonwebtoken.Claims claims = org.common.JwtUtil.validateToken(jwtToken);
        boolean loggedIn = (claims != null);
        String firstName = loggedIn && claims.get("firstName") != null ? claims.get("firstName").toString() : null;
        response.getWriter().write("{" +
            "\"loggedIn\":" + loggedIn +
            (firstName != null ? ",\"firstName\":\"" + firstName + "\"" : "") +
            "}");
    }
} 