package org.example;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.annotation.Resource;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet(name = "CartServlet", urlPatterns = "/api/cart")
public class CartServlet extends HttpServlet {
    @Resource(name = "jdbc/moviedb")
    private DataSource dataSource;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession();
        Map<String, Integer> cart = (Map<String, Integer>) session.getAttribute("cart");
        
        if (cart == null) {
            cart = new HashMap<>();
            session.setAttribute("cart", cart);
        }

        JsonArray jsonArray = new JsonArray();
        
        try (Connection conn = dataSource.getConnection()) {
            for (Map.Entry<String, Integer> entry : cart.entrySet()) {
                String movieId = entry.getKey();
                int quantity = entry.getValue();
                
                String query = "SELECT id, title, price FROM movies WHERE id = ?";
                try (PreparedStatement statement = conn.prepareStatement(query)) {
                    statement.setString(1, movieId);
                    try (ResultSet rs = statement.executeQuery()) {
                        if (rs.next()) {
                            JsonObject jsonObject = new JsonObject();
                            jsonObject.addProperty("movieId", rs.getString("id"));
                            jsonObject.addProperty("title", rs.getString("title"));
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

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession();
        Map<String, Integer> cart = (Map<String, Integer>) session.getAttribute("cart");
        
        if (cart == null) {
            cart = new HashMap<>();
            session.setAttribute("cart", cart);
        }

        String movieId = request.getParameter("movieId");
        String action = request.getParameter("action");
        
        JsonObject jsonResponse = new JsonObject();
        
        if (movieId != null && action != null) {
            try {
                switch (action) {
                    case "add":
                        cart.put(movieId, cart.getOrDefault(movieId, 0) + 1);
                        jsonResponse.addProperty("message", "Movie added to cart");
                        break;
                    case "remove":
                        cart.remove(movieId);
                        jsonResponse.addProperty("message", "Movie removed from cart");
                        break;
                    case "update":
                        String quantityStr = request.getParameter("quantity");
                        if (quantityStr != null) {
                            int quantity = Integer.parseInt(quantityStr);
                            if (quantity > 0) {
                                cart.put(movieId, quantity);
                                jsonResponse.addProperty("message", "Quantity updated");
                            } else {
                                cart.remove(movieId);
                                jsonResponse.addProperty("message", "Movie removed from cart");
                            }
                        }
                        break;
                }
                jsonResponse.addProperty("status", "success");
            } catch (Exception e) {
                jsonResponse.addProperty("status", "error");
                jsonResponse.addProperty("message", "Error updating cart: " + e.getMessage());
            }
        } else {
            jsonResponse.addProperty("status", "error");
            jsonResponse.addProperty("message", "Invalid request parameters");
        }
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonResponse.toString());
    }
} 