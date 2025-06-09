// File: src/main/java/org/example/MovieServlet.java
package org.example;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

@WebServlet(name = "MovieServlet", urlPatterns = {"/api/movielist"})
public class MovieServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String jwtToken = org.common.JwtUtil.getCookieValue(request, "jwtToken");
        io.jsonwebtoken.Claims claims = org.common.JwtUtil.validateToken(jwtToken);
        
        if (claims == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Unauthorized. Please login.\"}");
            return;
        }

        try {
            Context initCtx = new InitialContext();
            Context envCtx = (Context) initCtx.lookup("java:comp/env");
            DataSource ds = (DataSource) envCtx.lookup("jdbc/MySQLReadOnly");
            Connection conn = ds.getConnection();

            String query = "SELECT m.id, m.title, m.year, m.director, r.rating, " +
                    "GROUP_CONCAT(DISTINCT g.name) AS genres, " +
                    "GROUP_CONCAT(DISTINCT s.name) AS stars " +
                    "FROM movies m " +
                    "JOIN ratings r ON m.id = r.movieId " +
                    "LEFT JOIN genres_in_movies gim ON m.id = gim.movieId " +
                    "LEFT JOIN genres g ON gim.genreId = g.id " +
                    "LEFT JOIN stars_in_movies sim ON m.id = sim.movieId " +
                    "LEFT JOIN stars s ON sim.starId = s.id " +
                    "GROUP BY m.id, r.rating " +
                    "ORDER BY r.rating DESC LIMIT 20";

            PreparedStatement statement = conn.prepareStatement(query);
            ResultSet rs = statement.executeQuery();

            JSONArray movieList = new JSONArray();

            while (rs.next()) {
                JSONObject movie = new JSONObject();
                movie.put("id", rs.getString("id"));
                movie.put("title", rs.getString("title"));
                movie.put("year", rs.getInt("year"));
                movie.put("director", rs.getString("director"));
                movie.put("rating", rs.getFloat("rating"));

                String[] genres = rs.getString("genres") != null ? 
                    rs.getString("genres").split(",") : new String[0];
                String[] stars = rs.getString("stars") != null ? 
                    rs.getString("stars").split(",") : new String[0];

                movie.put("genres", genres);
                movie.put("stars", stars);

                movieList.put(movie);
            }

            rs.close();
            statement.close();
            conn.close();

            out.write(movieList.toString());
            response.setStatus(200);

        } catch (Exception e) {
            JSONObject error = new JSONObject();
            error.put("errorMessage", e.getMessage());
            response.setStatus(500);
            out.write(error.toString());
        } finally {
            out.close();
        }
    }
}
