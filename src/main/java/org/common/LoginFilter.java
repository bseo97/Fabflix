package org.common;

import io.jsonwebtoken.Claims;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Servlet Filter implementation class LoginFilter
 */
@WebFilter(filterName = "LoginFilter", urlPatterns = "/*")
public class LoginFilter implements Filter {
    private final ArrayList<String> allowedURIs = new ArrayList<>();

    /**
     * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
     */
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        System.out.println("LoginFilter: " + httpRequest.getRequestURI());

        // Check if this URL is allowed to access without logging in
        if (this.isUrlAllowedWithoutLogin(httpRequest.getRequestURI())) {
            // Keep default action: pass along the filter chain
            chain.doFilter(request, response);
            return;
        }

        String token = JwtUtil.getCookieValue(httpRequest, "jwtToken");
        Claims claims = JwtUtil.validateToken(token);

        if (claims != null) {
            // Store claims in request attributes
            // Downstream servlets can use claims as the session storage
            httpRequest.setAttribute("claims", claims);

            // Proceed with the request
            chain.doFilter(request, response);
        } else {
            httpResponse.sendRedirect("login.html");
        }
    }

    private boolean isUrlAllowedWithoutLogin(String requestURI) {
        /*
         Setup your own rules here to allow accessing some resources without logging in
         Always allow your own login related requests(html, js, servlet, etc..)
         You might also want to allow some CSS files, etc..
         */
        return requestURI.startsWith("/api/login")
            || requestURI.startsWith("/api/signup")
            || requestURI.endsWith("login.html")
            || requestURI.startsWith("/images/")
            || requestURI.endsWith(".css")
            || requestURI.endsWith(".js")
            || requestURI.endsWith(".png")
            || requestURI.endsWith(".jpg")
            || requestURI.endsWith(".jpeg")
            || requestURI.endsWith(".gif")
            || requestURI.endsWith(".woff2")
            || requestURI.endsWith(".woff")
            || requestURI.endsWith(".ttf");
    }

    public void init(FilterConfig fConfig) {
        allowedURIs.add("login.html");
        allowedURIs.add("api/login");
        allowedURIs.add("api/signup");
    }

    public void destroy() {
        // ignored.
    }

    
}