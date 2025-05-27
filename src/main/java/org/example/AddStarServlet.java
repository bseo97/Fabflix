package org.example;

import jakarta.annotation.Resource;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.sql.*;

import com.google.gson.JsonObject;
import com.google.gson.Gson;

@WebServlet(name = "AddStarServlet", urlPatterns = {"/_dashboard/add-star"})
public class AddStarServlet extends HttpServlet {

    @Resource(name = "jdbc/moviedb")
    private DataSource dataSource;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        JsonObject responseJsonObject = new JsonObject();

        // Ensure employee is logged in
        HttpSession session = request.getSession();
        String employee = (String) session.getAttribute("employee");
        if (employee == null) {
            responseJsonObject.addProperty("status", "error");
            responseJsonObject.addProperty("message", "Not authorized. Please log in as an employee.");
            out.write(responseJsonObject.toString());
            return;
        }

        try {
            // Parse request JSON
            StringBuilder requestBody = new StringBuilder();
            String line;
            try (BufferedReader reader = request.getReader()) {
                while ((line = reader.readLine()) != null) {
                    requestBody.append(line);
                }
            }

            Gson gson = new Gson();
            JsonObject jsonRequest = gson.fromJson(requestBody.toString(), JsonObject.class);

            String name = jsonRequest.get("name").getAsString();
            Integer birthYear = null;
            if (jsonRequest.has("birthYear") && !jsonRequest.get("birthYear").isJsonNull()) {
                try {
                    birthYear = jsonRequest.get("birthYear").getAsInt();
                } catch (NumberFormatException e) {
                    // Ignore invalid input for birth year
                }
            }

            if (name == null || name.trim().isEmpty()) {
                responseJsonObject.addProperty("status", "error");
                responseJsonObject.addProperty("message", "Star name cannot be empty");
                out.write(responseJsonObject.toString());
                return;
            }

            try (Connection conn = dataSource.getConnection()) {
                conn.setAutoCommit(false);

                // Generate new star ID
                String newStarId = null;
                String maxIdQuery = "SELECT MAX(id) AS max_id FROM stars";

                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(maxIdQuery)) {

                    if (rs.next()) {
                        String maxId = rs.getString("max_id");
                        int newNumber = 1;

                        if (maxId != null && maxId.startsWith("nm")) {
                            try {
                                newNumber = Integer.parseInt(maxId.substring(2)) + 1;
                            } catch (NumberFormatException e) {
                                newNumber = 1;
                            }
                        }

                        newStarId = "nm" + String.format("%07d", newNumber);
                    } else {
                        newStarId = "nm0000001";
                    }
                }

                // Insert into database
                String insertQuery = "INSERT INTO stars (id, name, birthYear) VALUES (?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {
                    pstmt.setString(1, newStarId);
                    pstmt.setString(2, name);
                    if (birthYear != null) {
                        pstmt.setInt(3, birthYear);
                    } else {
                        pstmt.setNull(3, Types.INTEGER);
                    }

                    int rowsAffected = pstmt.executeUpdate();
                    if (rowsAffected > 0) {
                        conn.commit();
                        responseJsonObject.addProperty("status", "success");
                        responseJsonObject.addProperty("message", "Star added successfully");
                        responseJsonObject.addProperty("starId", newStarId);
                        responseJsonObject.addProperty("name", name);
                        if (birthYear != null) {
                            responseJsonObject.addProperty("birthYear", birthYear);
                        }
                    } else {
                        conn.rollback();
                        responseJsonObject.addProperty("status", "error");
                        responseJsonObject.addProperty("message", "Failed to insert star.");
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            responseJsonObject.addProperty("status", "error");
            responseJsonObject.addProperty("message", "Error processing request: " + e.getMessage());
        }

        out.write(responseJsonObject.toString());
    }
}