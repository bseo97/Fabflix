package org.example;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.common.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "CartServlet", urlPatterns = "/api/cart")
public class CartServlet extends HttpServlet {
    @Resource(name = "jdbc/MySQLReadOnly")
    private DataSource dataSource;

    // GET: expects cart as JSON in request body, returns cart details
    // @Override
    // protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    //     // JWT authentication
    //     String jwtToken = JwtUtil.getCookieValue(request, "jwtToken");
    //     Claims claims = JwtUtil.validateToken(jwtToken);

    //     if (claims == null) {
    //         response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    //         response.setContentType("application/json");
    //         response.getWriter().write("{\"error\": \"Unauthorized. Please login.\"}");
    //         return;
    //     }

    //     // Parse cart from request body
    //     BufferedReader reader = request.getReader();
    //     StringBuilder json = new StringBuilder();
    //     String line;
    //     while ((line = reader.readLine()) != null) {
    //         json.append(line);
    //     }
    //     JsonObject cartData = json.length() > 0 ? JsonParser.parseString(json.toString()).getAsJsonObject() : new JsonObject();
    //     JsonObject cartJson = cartData.has("cart") ? cartData.getAsJsonObject("cart") : new JsonObject();
    //     Map<String, Integer> cart = new HashMap<>();
    //     for (String key : cartJson.keySet()) {
    //         cart.put(key, cartJson.get(key).getAsInt());
    //     }

    //     JsonArray jsonArray = new JsonArray();
    //     try (Connection conn = dataSource.getConnection()) {
    //         for (Map.Entry<String, Integer> entry : cart.entrySet()) {
    //             String movieId = entry.getKey();
    //             int quantity = entry.getValue();
    //             String query = "SELECT id, title, price FROM movies WHERE id = ?";
    //             try (PreparedStatement statement = conn.prepareStatement(query)) {
    //                 statement.setString(1, movieId);
    //                 try (ResultSet rs = statement.executeQuery()) {
    //                     if (rs.next()) {
    //                         JsonObject jsonObject = new JsonObject();
    //                         jsonObject.addProperty("movieId", rs.getString("id"));
    //                         jsonObject.addProperty("title", rs.getString("title"));
    //                         jsonObject.addProperty("price", rs.getDouble("price"));
    //                         jsonObject.addProperty("quantity", quantity);
    //                         jsonObject.addProperty("subtotal", rs.getDouble("price") * quantity);
    //                         jsonArray.add(jsonObject);
    //                     }
    //                 }
    //             }
    //         }
    //     } catch (Exception e) {
    //         JsonObject error = new JsonObject();
    //         error.addProperty("error", "Database error: " + e.getMessage());
    //         jsonArray.add(error);
    //     }

    //     response.setContentType("application/json");
    //     response.setCharacterEncoding("UTF-8");
    //     response.getWriter().write(jsonArray.toString());
    // }

    // POST: expects cart as JSON in request body, returns success
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // JWT authentication
        String jwtToken = JwtUtil.getCookieValue(request, "jwtToken");
        Claims claims = JwtUtil.validateToken(jwtToken);

        if (claims == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Unauthorized. Please login.\"}");
            return;
        }

        // Parse cart from request body
        BufferedReader reader = request.getReader();
        StringBuilder json = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            json.append(line);
        }
        JsonObject cartData = json.length() > 0 ? JsonParser.parseString(json.toString()).getAsJsonObject() : new JsonObject();
        JsonObject cartJson = cartData.has("cart") ? cartData.getAsJsonObject("cart") : new JsonObject();
        Map<String, Integer> cart = new HashMap<>();
        for (String key : cartJson.keySet()) {
            cart.put(key, cartJson.get(key).getAsInt());
        }

        JsonArray jsonArray = new JsonArray();
        try (Connection conn = dataSource.getConnection()) {
            for (Map.Entry<String, Integer> entry : cart.entrySet()) {
                String movieId = entry.getKey();
                int quantity = entry.getValue();
                String query = "SELECT id, title, price, year FROM movies WHERE id = ?";
                try (PreparedStatement statement = conn.prepareStatement(query)) {
                    statement.setString(1, movieId);
                    try (ResultSet rs = statement.executeQuery()) {
                        if (rs.next()) {
                            JsonObject jsonObject = new JsonObject();
                            jsonObject.addProperty("movieId", rs.getString("id"));
                            jsonObject.addProperty("title", rs.getString("title"));
                            jsonObject.addProperty("year", rs.getString("year")); // Added year support
                            jsonObject.addProperty("price", rs.getDouble("price"));
                            jsonObject.addProperty("quantity", quantity);
                            jsonObject.addProperty("subtotal", rs.getDouble("price") * quantity);
                            jsonArray.add(jsonObject);
                        }
                    }
                }
            }
        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("error", "Database error: " + e.getMessage());
            jsonArray.add(error);
        }
    
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonArray.toString());
    }
} 