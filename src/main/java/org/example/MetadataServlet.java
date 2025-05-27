package org.example;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;

@WebServlet(name = "MetadataServlet", urlPatterns = {"/_dashboard/metadata"})
public class MetadataServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Set response type to JSON
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        
        // Check session
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("employee") == null) {
            System.out.println("[WARNING] Unauthorized access attempt to metadata");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.write("{\"status\":\"error\",\"message\":\"Unauthorized access\"}");
            return;
        }
        
        try {
            // Get database connection
            Connection conn = DatabaseConnection.getConnection();
            DatabaseMetaData metaData = conn.getMetaData();
            
            // Debug logging
            System.out.println("[DEBUG] Fetching database metadata");
            
            // Get all tables
            ResultSet tables = metaData.getTables(null, null, "%", new String[]{"TABLE"});
            Map<String, List<Map<String, String>>> databaseInfo = new HashMap<>();
            
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                System.out.println("[DEBUG] Processing table: " + tableName);
                
                // Get columns for this table
                ResultSet columns = metaData.getColumns(null, null, tableName, "%");
                List<Map<String, String>> columnInfo = new ArrayList<>();
                
                while (columns.next()) {
                    Map<String, String> column = new HashMap<>();
                    column.put("name", columns.getString("COLUMN_NAME"));
                    column.put("type", columns.getString("TYPE_NAME"));
                    column.put("size", columns.getString("COLUMN_SIZE"));
                    column.put("nullable", columns.getString("IS_NULLABLE"));
                    columnInfo.add(column);
                }
                
                databaseInfo.put(tableName, columnInfo);
            }
            
            // Create response JSON
            JSONObject responseJson = new JSONObject();
            responseJson.put("status", "success");
            responseJson.put("tables", databaseInfo);
            
            System.out.println("[DEBUG] Metadata fetched successfully");
            out.write(responseJson.toString());
            
        } catch (Exception e) {
            System.out.println("[ERROR] Error fetching metadata: " + e.getMessage());
            out.write("{\"status\":\"error\",\"message\":\"Failed to fetch database metadata\"}");
        }
    }
} 