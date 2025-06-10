package org.example;

import jakarta.annotation.Resource;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.*;

public class SearchServlet extends HttpServlet {

    @Resource(name = "jdbc/MySQLReadOnly")
    private DataSource dataSource;

    @Override
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

        String input = request.getParameter("title");
        List<String> titleList = new ArrayList<>();
        if (input != null && !input.trim().isEmpty()) {
            titleList = Arrays.asList(input.trim().split("\\s+"));
        }

        String startsWith = request.getParameter("startsWith");
        String genre = request.getParameter("genre");
        String sort = request.getParameter("sort");
        String order = request.getParameter("order");
        String limit = request.getParameter("limit");
        String ac = request.getParameter("ac"); // autocomplete flag

        List<String> validSort = List.of("title", "rating");
        List<String> validOrder = List.of("asc", "desc");

        String sortField = validSort.contains((sort != null ? sort.toLowerCase() : "")) ? sort : "title";
        String sortOrder = validOrder.contains((order != null ? order.toLowerCase() : "")) ? order : "asc";
        int pageLimit = 10;

        try {
            pageLimit = (limit != null) ? Math.max(1, Math.min(100, Integer.parseInt(limit))) : 10;
        } catch (NumberFormatException e) {
            pageLimit = 10;
        }

        try (Connection conn = dataSource.getConnection()) {
            StringBuilder query = new StringBuilder();
            List<String> conditions = new ArrayList<>();
            List<Object> params = new ArrayList<>();

            query.append("SELECT m.id, m.title, m.year, m.director, IFNULL(r.rating, 0.0) AS rating, ");
            query.append("GROUP_CONCAT(DISTINCT s.name SEPARATOR ', ') AS stars, ");
            query.append("GROUP_CONCAT(DISTINCT g.name SEPARATOR ', ') AS genres ");
            query.append("FROM movies m ");
            query.append("LEFT JOIN ratings r ON m.id = r.movieId ");
            query.append("LEFT JOIN stars_in_movies sim ON m.id = sim.movieId ");
            query.append("LEFT JOIN stars s ON sim.starId = s.id ");
            query.append("LEFT JOIN genres_in_movies gim ON m.id = gim.movieId ");
            query.append("LEFT JOIN genres g ON gim.genreId = g.id ");

            if (genre != null && !genre.isEmpty()) {
                conditions.add("g.name = ?");
                params.add(genre);
            }

            if (input != null && !input.trim().isEmpty()) {
                StringBuilder fullTextQuery = new StringBuilder();
                for (String word : titleList) {
                    if (word.length() >= 1) {
                        if (fullTextQuery.length() > 0) fullTextQuery.append(" ");
                        fullTextQuery.append("+").append(word).append("*");
                    }
                }

                List<String> subConditions = new ArrayList<>();

                if (fullTextQuery.length() > 0)
                    subConditions.add("MATCH(m.title) AGAINST (? IN BOOLEAN MODE)");

                subConditions.add("m.title LIKE ?");

                conditions.add("(" + String.join(" OR ", subConditions) + ")");

                if (fullTextQuery.length() > 0)
                    params.add(fullTextQuery.toString());

                params.add("%" + input + "%");
            }

            if (startsWith != null && !startsWith.isEmpty()) {
                if (startsWith.equals("*")) {
                    conditions.add("m.title REGEXP '^[^A-Za-z0-9]'");
                } else {
                    conditions.add("m.title LIKE ?");
                    params.add(startsWith + "%");
                }
            }

            if (!conditions.isEmpty()) {
                query.append("WHERE ").append(String.join(" AND ", conditions)).append(" ");
            }

            query.append("GROUP BY m.id, m.title, m.year, m.director, r.rating ");
            query.append("ORDER BY ").append(sortField).append(" ").append(sortOrder).append(" ");
            query.append("LIMIT ?");

            PreparedStatement statement = conn.prepareStatement(query.toString());

            int paramIndex = 1;
            for (Object param : params) {
                if (param instanceof String) {
                    statement.setString(paramIndex++, (String) param);
                } else if (param instanceof Integer) {
                    statement.setInt(paramIndex++, (Integer) param);
                }
            }
            statement.setInt(paramIndex, pageLimit);

            ResultSet rs = statement.executeQuery();

            if ("yes".equals(ac)) {
                JSONArray suggestions = new JSONArray();
                while (rs.next()) {
                    JSONObject suggestion = new JSONObject();
                    suggestion.put("value", rs.getString("title") + " (" + rs.getInt("year") + ")");
                    suggestion.put("data", rs.getString("id"));
                    suggestions.put(suggestion);
                }

                JSONObject result = new JSONObject();
                result.put("query", input);
                result.put("suggestions", suggestions);
                out.write(result.toString());
            } else {
                JSONArray movies = new JSONArray();
                while (rs.next()) {
                    JSONObject movie = new JSONObject();
                    movie.put("id", rs.getString("id"));
                    movie.put("title", rs.getString("title"));
                    movie.put("year", rs.getInt("year"));
                    movie.put("director", rs.getString("director"));
                    movie.put("rating", rs.getDouble("rating"));
                    movie.put("stars", rs.getString("stars") == null ? "" : rs.getString("stars"));
                    movie.put("genres", rs.getString("genres") == null ? "" : rs.getString("genres"));
                    movies.put(movie);
                }
                out.write(movies.toString());
            }

            response.setStatus(200);
            rs.close();
            statement.close();

        } catch (SQLException e) {
            e.printStackTrace();
            JSONObject error = new JSONObject();
            error.put("errorMessage", "DB error: " + e.getMessage());
            out.write(error.toString());
            response.setStatus(500);
        } finally {
            out.close();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }
}
