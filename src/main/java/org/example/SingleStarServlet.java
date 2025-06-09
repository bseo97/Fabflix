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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Servlet implementation class SingleStarServlet
 */
@WebServlet(name = "SingleStarServlet", urlPatterns = "/api/single-star")
public class SingleStarServlet extends HttpServlet {
    private static final long serialVersionUID = 3L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        try {
            String starName = request.getParameter("name");
            if (starName == null || starName.trim().isEmpty()) {
                throw new ServletException("Star name not provided");
            }

            Context initCtx = new InitialContext();
            Context envCtx = (Context) initCtx.lookup("java:comp/env");
            DataSource ds = (DataSource) envCtx.lookup("jdbc/MySQLReadOnly");
            Connection conn = ds.getConnection();

            // Get star info
            String starQuery = "SELECT s.id, s.name, s.birthYear FROM stars s WHERE s.name = ?";
            PreparedStatement starStmt = conn.prepareStatement(starQuery);
            starStmt.setString(1, starName);
            ResultSet starRs = starStmt.executeQuery();

            JSONObject starJson = new JSONObject();
            String starId = null;

            if (starRs.next()) {
                starId = starRs.getString("id");
                starJson.put("name", starRs.getString("name"));
                int birthYear = starRs.getInt("birthYear");
                starJson.put("birthYear", starRs.wasNull() ? JSONObject.NULL : birthYear);
            } else {
                starJson.put("error", "Star not found.");
                out.write(starJson.toString());
                response.setStatus(404);
                return;
            }

            // Get movies acted in by star
            String movieQuery = "SELECT m.id, m.title, m.year, m.director, IFNULL(r.rating, 0.0) AS rating " +
                    "FROM movies m " +
                    "JOIN stars_in_movies sim ON m.id = sim.movieId " +
                    "JOIN ratings r ON m.id = r.movieId " +
                    "WHERE sim.starId = ? " +
                    "ORDER BY m.year DESC";

            // Use LEFT JOIN to include movies without ratings
            movieQuery = movieQuery.replace("JOIN ratings r", "LEFT JOIN ratings r");

            PreparedStatement movieStmt = conn.prepareStatement(movieQuery);
            movieStmt.setString(1, starId);
            ResultSet movieRs = movieStmt.executeQuery();

            JSONArray movies = new JSONArray();

            while (movieRs.next()) {
                JSONObject movie = new JSONObject();
                movie.put("id", movieRs.getString("id"));
                movie.put("title", movieRs.getString("title"));
                movie.put("year", movieRs.getInt("year"));
                movie.put("director", movieRs.getString("director"));
                movie.put("rating", movieRs.getDouble("rating"));
                movies.put(movie);
            }

            starJson.put("movies", movies);

            movieRs.close();
            movieStmt.close();
            starRs.close();
            starStmt.close();
            conn.close();

            out.write(starJson.toString());
            response.setStatus(200);

        } catch (Exception e) {
            e.printStackTrace();
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("errorMessage", e.getMessage());
            out.write(jsonObject.toString());
            response.setStatus(500);
        } finally {
            out.close();
        }
    }
}
