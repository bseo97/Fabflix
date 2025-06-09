package org.example;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.sql.DataSource;
import jakarta.annotation.Resource;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class BrowseServlet extends HttpServlet {

    @Resource(name = "jdbc/MySQLReadOnly")
    private DataSource dataSource;

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        // Parameters
        String startsWith = request.getParameter("startsWith");
        String genre = request.getParameter("genre");

        try (Connection conn = dataSource.getConnection()) {

            StringBuilder query = new StringBuilder();
            List<Object> parameters = new ArrayList<>();

            query.append("SELECT m.id, m.title, m.year, m.director, IFNULL(r.rating, 0.0) AS rating, ");
            query.append("GROUP_CONCAT(DISTINCT s.name SEPARATOR ', ') AS stars ");
            query.append("FROM movies m ");
            query.append("LEFT JOIN ratings r ON m.id = r.movieId ");
            query.append("LEFT JOIN stars_in_movies sim ON m.id = sim.movieId ");
            query.append("LEFT JOIN stars s ON sim.starId = s.id ");

            if (genre != null && !genre.isEmpty()) {
                query.append("JOIN genres_in_movies gim ON m.id = gim.movieId ");
                query.append("JOIN genres g ON gim.genreId = g.id ");
            }

            query.append("WHERE 1=1 ");

            if (startsWith != null && !startsWith.isEmpty()) {
                if (startsWith.equals("*")) {
                    query.append("AND m.title REGEXP '^[^a-zA-Z0-9]' ");
                } else {
                    query.append("AND m.title LIKE ? ");
                    parameters.add(startsWith + "%");
                }
            }

            if (genre != null && !genre.isEmpty()) {
                query.append("AND g.name = ? ");
                parameters.add(genre);
            }

            query.append("GROUP BY m.id, m.title, m.year, m.director, r.rating ");
            query.append("ORDER BY m.title ASC ");
            query.append("LIMIT 100");

            PreparedStatement statement = conn.prepareStatement(query.toString());
            for (int i = 0; i < parameters.size(); i++) {
                Object param = parameters.get(i);
                if (param instanceof Integer) {
                    statement.setInt(i + 1, (Integer) param);
                } else {
                    statement.setString(i + 1, param.toString());
                }
            }

            ResultSet rs = statement.executeQuery();
            JSONArray results = new JSONArray();

            while (rs.next()) {
                JSONObject movie = new JSONObject();
                movie.put("id", rs.getString("id"));
                movie.put("title", rs.getString("title"));
                movie.put("year", rs.getInt("year"));
                movie.put("director", rs.getString("director"));
                movie.put("rating", rs.getDouble("rating"));
                movie.put("stars", rs.getString("stars") == null ? "" : rs.getString("stars"));
                results.put(movie);
            }

            rs.close();
            statement.close();
            out.write(results.toString());
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
