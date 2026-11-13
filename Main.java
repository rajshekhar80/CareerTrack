
package backend;

import java.util.*;
import java.io.*;
import java.net.*;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.nio.charset.StandardCharsets;

// ABSTRACTION
abstract class User {
    private String name;
    private String email;

    // CONSTRUCTOR
    User(String name, String email) {
        this.name = name;
        this.email = email;
    }

    // ENCAPSULATION
    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    abstract void displayDashboard();
}

// INHERITANCE
class Student extends User {
    private double cgpa;

    Student(String name, String email, double cgpa) {
        super(name, email);
        this.cgpa = cgpa;
    }

    // METHOD OVERRIDING
    @Override
    void displayDashboard() {
        System.out.println("Student: " + getName());
        System.out.println("CGPA: " + cgpa);
    }
}

class Admin extends User {
    Admin(String name, String email) {
        super(name, email);
    }

    // METHOD OVERRIDING
    @Override
    void displayDashboard() {
        System.out.println("Admin: " + getName());
    }
}

// APPLICATION CLASS
class Application {
    private int id;
    private String company;
    private String role;
    private String status;
    private String stipend;

    Application(int id, String company, String role,
                String status, String stipend) {
        this.id = id;
        this.company = company;
        this.role = role;
        this.status = status;
        this.stipend = stipend;
    }

    public int getId() { return id; }
    public String getCompany() { return company; }
    public String getRole() { return role; }
    public String getStatus() { return status; }
    public String getStipend() { return stipend; }

    public void setStatus(String status) {
        this.status = status;
    }
}

// APPLICATION MANAGER
class ApplicationManager {
    private List<Application> applications = new ArrayList<>();
    private int nextId = 1;

    // METHOD OVERLOADING - VERSION 1
    public void addApplication(String company, String role) {
        addApplication(company, role, "Applied", "");
    }

    // METHOD OVERLOADING - VERSION 2
    public void addApplication(String company, String role,
                               String status, String stipend) {
        Application app = new Application(
            nextId++, company, role, status, stipend
        );
        applications.add(app);
    }

    public List<Application> getApplications() {
        return applications;
    }

    public void deleteApplication(int id) {
        applications.removeIf(app -> app.getId() == id);
    }

    public void updateStatus(int id, String status) {
        for (Application app : applications) {
            if (app.getId() == id) {
                app.setStatus(status);
            }
        }
    }
}

// MAIN SERVER
public class Main {

    static ApplicationManager manager = new ApplicationManager();

    // READ FORM DATA
    static Map<String, String> parseParams(String data)
            throws Exception {
        Map<String, String> map = new HashMap<>();

        if (data == null || data.isEmpty()) return map;

        for (String pair : data.split("&")) {
            String[] parts = pair.split("=", 2);

            String key = URLDecoder.decode(
                parts[0], StandardCharsets.UTF_8
            );

            String value = parts.length > 1
                ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
                : "";

            map.put(key, value);
        }

        return map;
    }

    // JSON TEXT ESCAPING
    static String escape(String value) {
        if (value == null) return "";

        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
    }

    // SEND RESPONSE
    static void send(HttpExchange exchange, int status,
                     String response) throws IOException {

        exchange.getResponseHeaders().set(
            "Content-Type", "application/json; charset=UTF-8"
        );

        exchange.getResponseHeaders().set(
            "Access-Control-Allow-Origin", "*"
        );

        exchange.getResponseHeaders().set(
            "Access-Control-Allow-Methods",
            "GET, POST, PUT, DELETE, OPTIONS"
        );

        exchange.getResponseHeaders().set(
            "Access-Control-Allow-Headers", "Content-Type"
        );

        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

        exchange.sendResponseHeaders(status, bytes.length);

        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    // START SERVER
    public static void main(String[] args) throws Exception {

        // POLYMORPHISM
        User student = new Student(
            "Raj", "raj@gmail.com", 8.0
        );

        student.displayDashboard();

        HttpServer server = HttpServer.create(
            new InetSocketAddress(8080), 0
        );

        server.createContext("/api/applications", exchange -> {
            try {
                String method = exchange.getRequestMethod();

                if (method.equals("OPTIONS")) {
                    send(exchange, 204, "");
                    return;
                }

                URI uri = exchange.getRequestURI();

                Map<String, String> params =
                    parseParams(uri.getRawQuery());

                if (method.equals("GET")) {

                    StringBuilder json =
                        new StringBuilder("{\"applications\":[");

                    List<Application> list =
                        manager.getApplications();

                    for (int i = 0; i < list.size(); i++) {
                        Application app = list.get(i);

                        if (i > 0) json.append(",");

                        json.append("{")
                            .append("\"id\":").append(app.getId()).append(",")
                            .append("\"company\":\"")
                            .append(escape(app.getCompany())).append("\",")
                            .append("\"role\":\"")
                            .append(escape(app.getRole())).append("\",")
                            .append("\"status\":\"")
                            .append(escape(app.getStatus())).append("\",")
                            .append("\"stipend\":\"")
                            .append(escape(app.getStipend())).append("\"")
                            .append("}");
                    }

                    json.append("]}");

                    send(exchange, 200, json.toString());

                } else if (method.equals("POST")) {

                    String body = new String(
                        exchange.getRequestBody().readAllBytes(),
                        StandardCharsets.UTF_8
                    );

                    Map<String, String> form = parseParams(body);

                    String company = form.getOrDefault("company", "");
                    String role = form.getOrDefault("role", "");
                    String status = form.getOrDefault("status", "Applied");
                    String stipend = form.getOrDefault("stipend", "");

                    if (company.isBlank() || role.isBlank()) {
                        send(exchange, 400,
                            "{\"error\":\"Company and role required\"}");
                        return;
                    }

                    manager.addApplication(
                        company, role, status, stipend
                    );

                    send(exchange, 201,
                        "{\"message\":\"Application added successfully\"}");

                } else if (method.equals("DELETE")) {

                    int id = Integer.parseInt(
                        params.getOrDefault("id", "0")
                    );

                    manager.deleteApplication(id);

                    send(exchange, 200,
                        "{\"message\":\"Application deleted\"}");

                } else if (method.equals("PUT")) {

                    int id = Integer.parseInt(
                        params.getOrDefault("id", "0")
                    );

                    String status = params.getOrDefault("status", "");

                    if (status.isBlank()) {
                        send(exchange, 400,
                            "{\"error\":\"Status required\"}");
                        return;
                    }

                    manager.updateStatus(id, status);

                    send(exchange, 200,
                        "{\"message\":\"Status updated\"}");

                } else {
                    send(exchange, 405,
                        "{\"error\":\"Method not allowed\"}");
                }

            } catch (Exception e) {
                send(exchange, 400,
                    "{\"error\":\"Invalid request\"}");
            }
        });

        server.start();

        System.out.println("--------------------------------");
        System.out.println("CAREERTRACK JAVA SERVER STARTED");
        System.out.println("Server: http://localhost:8080");
        System.out.println("API: http://localhost:8080/api/applications");
        System.out.println("--------------------------------");
    }
}
