package org.example;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * A servlet that handles HTTP GET requests and returns a single movie's information in JSON format.
 */
@WebServlet(name = "SingleMovieServlet", urlPatterns = "/api/single-movie")
public class SingleMovieServlet extends HttpServlet {
    private static final long serialVersionUID = 2L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        try {
            String movieId = request.getParameter("id");
            if (movieId == null || movieId.trim().isEmpty()) {
                throw new ServletException("Movie ID not provided");
            }

            Context initCtx = new InitialContext();
            Context envCtx = (Context) initCtx.lookup("java:comp/env");
            DataSource ds = (DataSource) envCtx.lookup("jdbc/MySQLReadOnly");
            Connection conn = ds.getConnection();

            String query = "SELECT m.id, m.title, m.year, m.director, MAX(r.rating) AS rating, " +
                    "GROUP_CONCAT(DISTINCT g.name) AS genres, " +
                    "GROUP_CONCAT(DISTINCT s.name) AS stars " +
                    "FROM movies m " +
                    "LEFT JOIN ratings r ON m.id = r.movieId " +
                    "LEFT JOIN genres_in_movies gim ON m.id = gim.movieId " +
                    "LEFT JOIN genres g ON gim.genreId = g.id " +
                    "LEFT JOIN stars_in_movies sim ON m.id = sim.movieId " +
                    "LEFT JOIN stars s ON sim.starId = s.id " +
                    "WHERE m.id = ? " +
                    "GROUP BY m.id";

            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, movieId);
            ResultSet rs = stmt.executeQuery();

            JSONObject movieJson = new JSONObject();

            if (rs.next()) {
                movieJson.put("title", rs.getString("title"));
                movieJson.put("year", rs.getInt("year"));
                movieJson.put("director", rs.getString("director"));
                double rating = rs.getDouble("rating");
                if (rs.wasNull()) {
                    movieJson.put("rating", JSONObject.NULL);
                } else {
                    movieJson.put("rating", rating);
                }
                
                String[] genres = rs.getString("genres") != null ? rs.getString("genres").split(",") : new String[0];
                String[] stars = rs.getString("stars") != null ? rs.getString("stars").split(",") : new String[0];
    
                movieJson.put("genres", genres);
                movieJson.put("stars", stars);
            }

            rs.close();
            stmt.close();
            conn.close();

            out.write(movieJson.toString());
            response.setStatus(200);

        } catch (Exception e) {
            e.printStackTrace();
            JSONObject error = new JSONObject();
            error.put("errorMessage", e.getMessage());
            out.write(error.toString());
            response.setStatus(500);
        } finally {
            out.close();
        }
    }
}
