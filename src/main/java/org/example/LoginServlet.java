package org.example;

import jakarta.annotation.Resource;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.*;

@WebServlet(name = "LoginServlet", urlPatterns = {"/api/login"})
public class LoginServlet extends HttpServlet {

    @Resource(name = "jdbc/moviedb")
    private DataSource dataSource;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String gRecaptchaResponse = request.getParameter("g-recaptcha-response");

        // Verify reCAPTCHA
        try {
            RecaptchaVerifyUtils.verify(gRecaptchaResponse);
        } catch (Exception e) {
            response.sendRedirect(request.getContextPath() + "/login.html?error=Failed+reCAPTCHA+verification");
            return;
        }

        // Authenticate user
        try (Connection conn = dataSource.getConnection()) {
            String query = "SELECT password FROM customers WHERE email = ?";
            PreparedStatement statement = conn.prepareStatement(query);
            statement.setString(1, email);
            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                String storedPassword = rs.getString("password");

                if (storedPassword != null && storedPassword.equals(password)) {
                    HttpSession session = request.getSession();
                    session.setAttribute("user", email);
                    response.sendRedirect(request.getContextPath() + "/main.html");
                } else {
                    response.sendRedirect(request.getContextPath() + "/login.html?error=Invalid+username+or+password");
                }
            } else {
                response.sendRedirect(request.getContextPath() + "/login.html?error=Invalid+username+or+password");
            }

            rs.close();
            statement.close();

        } catch (SQLException e) {
            e.printStackTrace();
            response.sendRedirect(request.getContextPath() + "/login.html?error=Login+failed+due+to+server+error");
        }
    }
}
