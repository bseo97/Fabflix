package org.example;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.sql.DataSource;
import jakarta.annotation.Resource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(name = "MovieListServlet", urlPatterns = {"/api/top20"})
public class MovieListServlet extends HttpServlet {
    @Resource(name = "jdbc/MySQLReadOnly")
    private DataSource dataSource;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
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
        System.out.println("🟡 DEBUG: Entering MovieListServlet");
System.out.println("🟡 DataSource is null? " + (dataSource == null));

        try (Connection conn = dataSource.getConnection()) {
            String query = "SELECT m.title, m.year, m.director, r.rating FROM movies m, ratings r WHERE m.id = r.movieId ORDER BY r.rating DESC LIMIT 20";
            PreparedStatement statement = conn.prepareStatement(query);
            ResultSet rs = statement.executeQuery();

            JSONArray movies = new JSONArray();

            while (rs.next()) {
                JSONObject movie = new JSONObject();
                movie.put("title", rs.getString("title"));
                movie.put("year", rs.getInt("year"));
                movie.put("director", rs.getString("director"));
                movie.put("rating", rs.getDouble("rating"));
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
