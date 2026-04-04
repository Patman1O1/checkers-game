package edu.cs342.project2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.net.URL;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import org.json.JSONObject;

public class Server extends org.eclipse.jetty.server.Server {
    /* ----------------------------------------------------Servlet--------------------------------------------------- */
    private static class Servlet extends HttpServlet {
        /* ------------------------------------------------Methods--------------------------------------------------- */
        private String readResource(String path) throws IOException {
            InputStream is = getClass().getClassLoader().getResourceAsStream(path);
            if (is == null) {
                throw new IOException("Resource not found: " + path);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        }

        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
            String path = request.getPathInfo();

            // Serve the JS file when browser requests it
            if (path != null && path.equals("/location.js")) {
                response.setContentType("application/javascript");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write(readResource("javascript/location.js"));
                return;
            }

            // Serve the HTML page by default
            response.setContentType("text/html");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(readResource("html/location.html"));
        }

        protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
            BufferedReader reader = request.getReader();
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) body.append(line);

            JSONObject json = new JSONObject(body.toString());
            double lat = json.getDouble("lat");
            double lon = json.getDouble("lon");

            System.out.println("Lat: " + lat + ", Lon: " + lon);

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            JSONObject responseJson = new JSONObject();
            responseJson.put("status", "success");
            responseJson.put("lat", lat);
            responseJson.put("lon", lon);

            response.getWriter().write(responseJson.toString());
        }
    }

    /* ----------------------------------------------------Fields---------------------------------------------------- */
    private final int port;

    /* -------------------------------------------------Constructors------------------------------------------------- */
    public Server(int port) {
        // Set the port number
        super(port);
        this.port = port;

        // Set up the servlet context
        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");

        // Register the servlet
        context.addServlet(new ServletHolder(new Servlet()), "/location/*");

        super.setHandler(context);
    }

    /* ---------------------------------------------------Getters---------------------------------------------------- */
    public int getPort() { return this.port; }

    /* ---------------------------------------------------Methods---------------------------------------------------- */
    public URL url() throws MalformedURLException {
        return new URL("http://localhost:" + this.port + "/location");
    }
}
