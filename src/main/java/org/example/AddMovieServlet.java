package org.example;

import jakarta.annotation.Resource;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import com.google.gson.JsonObject;
import com.google.gson.Gson;

@WebServlet(name = "AddMovieServlet", urlPatterns = {"/_dashboard/add-movie"})
public class AddMovieServlet extends HttpServlet {

    @Resource(name = "jdbc/moviedb")
    private DataSource dataSource;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Set response type to JSON
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        JsonObject responseJson = new JsonObject();

        // Check if user is logged in as employee
        HttpSession session = request.getSession();
        String employee = (String) session.getAttribute("employee");
        if (employee == null) {
            responseJson.addProperty("status", "error");
            responseJson.addProperty("message", "Not authorized. Please log in as an employee.");
            out.write(responseJson.toString());
            return;
        }

        try {
            // Parse request body
            StringBuilder requestBody = new StringBuilder();
            String line;
            try (java.io.BufferedReader reader = request.getReader()) {
                while ((line = reader.readLine()) != null) {
                    requestBody.append(line);
                }
            }
            
            Gson gson = new Gson();
            JsonObject jsonRequest = gson.fromJson(requestBody.toString(), JsonObject.class);

            // Extract parameters
            String title = jsonRequest.get("title").getAsString();
            int year = jsonRequest.get("year").getAsInt();
            String director = jsonRequest.get("director").getAsString();
            String starName = jsonRequest.get("starName").getAsString();
            String genreName = jsonRequest.get("genreName").getAsString();

            // Log operation for debugging
            System.out.println("[DEBUG] Adding movie: " + title + ", Year: " + year + ", Director: " + 
                            director + ", Star: " + starName + ", Genre: " + genreName);

            // Validation
            if (title == null || title.trim().isEmpty() || 
                director == null || director.trim().isEmpty() || 
                starName == null || starName.trim().isEmpty() || 
                genreName == null || genreName.trim().isEmpty()) {
                responseJson.addProperty("status", "error");
                responseJson.addProperty("message", "All fields are required except birth year");
                out.write(responseJson.toString());
                return;
            }

            try (Connection conn = dataSource.getConnection()) {
                // Start transaction
                conn.setAutoCommit(false);
                
                try {
                    // 1. Generate new movie ID
                    String movieId = generateUniqueId(conn, "movies");
                    
                    // 2. Insert new movie
                    String insertMovieQuery = "INSERT INTO movies (id, title, year, director) VALUES (?, ?, ?, ?)";
                    try (PreparedStatement pstmt = conn.prepareStatement(insertMovieQuery)) {
                        pstmt.setString(1, movieId);
                        pstmt.setString(2, title);
                        pstmt.setInt(3, year);
                        pstmt.setString(4, director);
                        pstmt.executeUpdate();
                    }
                    
                    // 3. Get or create genre
                    int genreId = getOrCreateGenre(conn, genreName);
                    
                    // 4. Insert genre-movie relation
                    String insertGenreInMovieQuery = "INSERT INTO genres_in_movies (genreId, movieId) VALUES (?, ?)";
                    try (PreparedStatement pstmt = conn.prepareStatement(insertGenreInMovieQuery)) {
                        pstmt.setInt(1, genreId);
                        pstmt.setString(2, movieId);
                        pstmt.executeUpdate();
                    }
                    
                    // 5. Get or create star
                    String starId = generateUniqueId(conn, "stars");
                    String insertStarQuery = "INSERT INTO stars (id, name) VALUES (?, ?)";
                    try (PreparedStatement pstmt = conn.prepareStatement(insertStarQuery)) {
                        pstmt.setString(1, starId);
                        pstmt.setString(2, starName);
                        pstmt.executeUpdate();
                    }
                    
                    // 6. Insert star-movie relation
                    String insertStarInMovieQuery = "INSERT INTO stars_in_movies (starId, movieId) VALUES (?, ?)";
                    try (PreparedStatement pstmt = conn.prepareStatement(insertStarInMovieQuery)) {
                        pstmt.setString(1, starId);
                        pstmt.setString(2, movieId);
                        pstmt.executeUpdate();
                    }
                    
                    // Commit transaction
                    conn.commit();
                    
                    // Return success response
                    responseJson.addProperty("status", "success");
                    responseJson.addProperty("message", "Movie added successfully");
                    responseJson.addProperty("movieId", movieId);
                    responseJson.addProperty("title", title);
                    responseJson.addProperty("year", year);
                    responseJson.addProperty("director", director);
                    responseJson.addProperty("starId", starId);
                    responseJson.addProperty("starName", starName);
                    responseJson.addProperty("genreId", genreId);
                    responseJson.addProperty("genreName", genreName);
                } catch (SQLException e) {
                    // Rollback transaction on error
                    conn.rollback();
                    throw e;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            responseJson.addProperty("status", "error");
            responseJson.addProperty("message", "Error: " + e.getMessage());
        }
        
        out.write(responseJson.toString());
    }
    
    // Helper method to get or create a genre
    private int getOrCreateGenre(Connection conn, String genreName) throws SQLException {
        // Check if genre exists
        String checkGenreQuery = "SELECT id FROM genres WHERE name = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(checkGenreQuery)) {
            pstmt.setString(1, genreName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        
        // Genre doesn't exist, create new one
        int genreId;
        String maxGenreIdQuery = "SELECT MAX(id) AS max_id FROM genres";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(maxGenreIdQuery)) {
            if (rs.next()) {
                genreId = rs.getInt("max_id") + 1;
            } else {
                genreId = 1; // Default if no genres exist
            }
        }
        
        String insertGenreQuery = "INSERT INTO genres (id, name) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertGenreQuery)) {
            pstmt.setInt(1, genreId);
            pstmt.setString(2, genreName);
            pstmt.executeUpdate();
            return genreId;
        }
    }
    
    // Helper method to generate unique IDs
    private String generateUniqueId(Connection conn, String tableName) throws SQLException {
        String query = "SELECT MAX(CAST(SUBSTRING(id, 3) AS UNSIGNED)) + 1 AS nextId FROM " + tableName;
        try (PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return String.format("nm%07d", rs.getInt("nextId"));
            }
        }
        return "nm0000001"; // Default to nm0000001 if no entries exist
    }
}
