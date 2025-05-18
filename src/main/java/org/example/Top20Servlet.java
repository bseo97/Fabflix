package org.example;

import jakarta.annotation.Resource;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Top20Servlet extends HttpServlet {
    @Resource(name = "jdbc/moviedb")
    private DataSource dataSource;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        try (Connection conn = dataSource.getConnection()) {
            String query =
                "SELECT m.id, m.title, m.year, m.director, r.rating, " +
                "GROUP_CONCAT(DISTINCT g.name ORDER BY g.name) AS genres, " +
                "GROUP_CONCAT(DISTINCT s.name ORDER BY s.name) AS stars " +
                "FROM movies m " +
                "JOIN ratings r ON m.id = r.movieId " +
                "LEFT JOIN genres_in_movies gim ON m.id = gim.movieId " +
                "LEFT JOIN genres g ON gim.genreId = g.id " +
                "LEFT JOIN stars_in_movies sim ON m.id = sim.movieId " +
                "LEFT JOIN stars s ON sim.starId = s.id " +
                "GROUP BY m.id, m.title, m.year, m.director, r.rating " +
                "ORDER BY r.rating DESC " +
                "LIMIT 20";

            PreparedStatement statement = conn.prepareStatement(query);
            ResultSet rs = statement.executeQuery();

            JSONArray movies = new JSONArray();
            while (rs.next()) {
                JSONObject movie = new JSONObject();
                movie.put("id", rs.getString("id"));
                movie.put("title", rs.getString("title"));
                movie.put("year", rs.getInt("year"));
                movie.put("director", rs.getString("director"));
                movie.put("rating", rs.getDouble("rating"));

                String[] genres = rs.getString("genres") != null ? rs.getString("genres").split(",") : new String[0];
                String[] stars = rs.getString("stars") != null ? rs.getString("stars").split(",") : new String[0];
                movie.put("genres", genres);
                movie.put("stars", stars);

                movies.put(movie);
            }

            rs.close();
            statement.close();
            out.write(movies.toString());
            response.setStatus(200);

        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(500);
        } finally {
            out.close();
        }
    }
}
