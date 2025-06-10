package org.example;

import jakarta.annotation.Resource;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.common.JwtUtil;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "LoginServlet", urlPatterns = {"/api/login"})
public class LoginServlet extends HttpServlet {

    @Resource(name = "jdbc/MySQLReadWrite")
    private DataSource dataSource;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String email = request.getParameter("email");
        String password = request.getParameter("password");

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try (Connection conn = dataSource.getConnection()) {
            String query = "SELECT id, password, firstName, lastName FROM customers WHERE email = ?";
            PreparedStatement statement = conn.prepareStatement(query);
            statement.setString(1, email);
            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                String storedPassword = rs.getString("password");

                if (storedPassword != null && storedPassword.equals(password)) {
                    // ✅ Success: Generate JWT token
                    Map<String, Object> claims = new HashMap<>();
                    claims.put("email", email);
                    claims.put("customerId", rs.getString("id"));
                    claims.put("firstName", rs.getString("firstName"));
                    claims.put("lastName", rs.getString("lastName"));

                    String jwtToken = JwtUtil.generateToken(email, claims);
                    JwtUtil.updateJwtCookie(request, response, jwtToken);

                    response.setStatus(HttpServletResponse.SC_OK);
                    response.getWriter().write("{\"message\": \"Login successful\"}");
                    
                    return;
                }
            }

            // ❌ Invalid credentials
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Invalid email or password\"}");

        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"Server error during login\"}");
        }
    }
}
