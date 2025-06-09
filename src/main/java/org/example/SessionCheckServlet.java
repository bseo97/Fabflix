package org.example;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import org.common.JwtUtil;
import io.jsonwebtoken.Claims;

import java.io.IOException;

@WebServlet(name = "SessionCheckServlet", urlPatterns = {"/api/session-check"})
public class SessionCheckServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String token = JwtUtil.getCookieValue(request, "jwtToken");
        Claims claims = JwtUtil.validateToken(token);

        boolean loggedIn = (claims != null);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");

        response.getWriter().write("{\"loggedIn\": " + loggedIn + "}");
    }
}
