package org.example;

import com.google.gson.Gson;
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
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "PlaceOrderServlet", urlPatterns = "/api/place-order")
public class PlaceOrderServlet extends HttpServlet {
    @Resource(name = "jdbc/MySQLReadWrite")
    private DataSource dataSource;
    private Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        
        try {
            // JWT authentication
            String jwtToken = JwtUtil.getCookieValue(request, "jwtToken");
            Claims claims = JwtUtil.validateToken(jwtToken);

            if (claims == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Unauthorized. Please login.\"}");
                return;
            }

            // Extract customerId from claims
            String customerIdStr = claims.get("customerId", String.class);
            Integer customerId = Integer.parseInt(customerIdStr);
            if (customerId == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Unauthorized. No customerId in token.\"}");
                return;
            }

            // Read request body
            BufferedReader reader = request.getReader();
            StringBuilder json = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                json.append(line);
            }

            // Parse JSON
            JsonObject orderData = JsonParser.parseString(json.toString()).getAsJsonObject();
            String firstName = orderData.get("firstName").getAsString();
            String lastName = orderData.get("lastName").getAsString();
            String creditCard = orderData.get("creditCard").getAsString().replaceAll("\\s+", "");
            String expiration = orderData.get("expiration").getAsString();

            // Log request data
            System.out.println("Order Data: " + orderData);

            // Parse cart from request body
            JsonObject cartJson = orderData.getAsJsonObject("cart");
            Map<String, Integer> cart = new HashMap<>();
            for (String key : cartJson.keySet()) {
                cart.put(key, cartJson.get(key).getAsInt());
            }

            if (cart.isEmpty()) {
                sendError(response, "Cart is empty");
                return;
            }

            // Validate credit card
            try (Connection conn = dataSource.getConnection()) {
                String query = "SELECT * FROM creditcards WHERE firstName = ? AND lastName = ? AND id = ? AND expiration = ?";
                try (PreparedStatement statement = conn.prepareStatement(query)) {
                    statement.setString(1, firstName);
                    statement.setString(2, lastName);
                    statement.setString(3, creditCard);
                    statement.setString(4, expiration);

                    try (ResultSet rs = statement.executeQuery()) {
                        if (!rs.next()) {
                            Map<String, String> errors = new HashMap<>();
                            errors.put("creditCard", "Invalid credit card information");
                            sendError(response, errors);
                            return;
                        }

                        // Credit card is valid, process the order
                        String insertSale = "INSERT INTO sales (customerId, movieId, saleDate, quantity) VALUES (?, ?, ?, ?)";
                        try (PreparedStatement insertStatement = conn.prepareStatement(insertSale)) {
                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                            String currentDate = dateFormat.format(new Date());

                            for (Map.Entry<String, Integer> entry : cart.entrySet()) {
                                insertStatement.setInt(1, customerId);
                                insertStatement.setString(2, entry.getKey());
                                insertStatement.setString(3, currentDate);
                                insertStatement.setInt(4, entry.getValue());
                                insertStatement.executeUpdate();
                            }

                            // Send success response
                            JsonObject jsonResponse = new JsonObject();
                            jsonResponse.addProperty("status", "success");
                            jsonResponse.addProperty("message", "Order placed successfully");
                            jsonResponse.addProperty("orderId", customerId);

                            response.setContentType("application/json");
                            response.getWriter().write(jsonResponse.toString());
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendError(response, "Database error: " + e.getMessage());
            }
        } catch (Exception e) {
            JsonObject response_obj = new JsonObject();
            response_obj.addProperty("status", "error");
            response_obj.addProperty("message", "Server error: " + e.getMessage());
            out.write(gson.toJson(response_obj));
            e.printStackTrace();
        }
    }

    private void sendError(HttpServletResponse response, String message) throws IOException {
        JsonObject jsonResponse = new JsonObject();
        jsonResponse.addProperty("status", "error");
        jsonResponse.addProperty("message", message);

        response.setContentType("application/json");
        response.getWriter().write(jsonResponse.toString());
    }

    private void sendError(HttpServletResponse response, Map<String, String> errors) throws IOException {
        JsonObject jsonResponse = new JsonObject();
        jsonResponse.addProperty("status", "error");

        JsonObject errorsObj = new JsonObject();
        errors.forEach(errorsObj::addProperty);
        jsonResponse.add("errors", errorsObj);

        response.setContentType("application/json");
        response.getWriter().write(jsonResponse.toString());
    }
} 