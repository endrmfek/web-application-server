package webserver;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Map;
import java.util.StringTokenizer;

import controller.Controller;
import db.DataBase;
import http.HttpRequest;
import http.HttpResponse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpRequestUtils;
import util.IOUtils;

@Slf4j
public class RequestHandler extends Thread {

    private Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            HttpRequest request = new HttpRequest(in);
            HttpResponse response = new HttpResponse(out);

            Controller controller = RequestMapping.getController(request.getPath());

            if (controller == null) {
                String path = getDefaultUrl(request.getPath());
                response.forward(path);
            } else {
                controller.service(request, response);
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

//    private void listUser(HttpRequest request, HttpResponse response) {
//        if (!isLogin(request.getHeader("Cookie"))) {
//            response.sendRedirect("/user/login_failed.html");
//            return;
//        }
//
//        Collection<User> users = DataBase.findAll();
//        StringBuilder sb = new StringBuilder();
//        sb.append("<table border='1'>");
//        for (User user : users) {
//            sb.append("<tr>");
//            sb.append("<td>" + user.getUserId() + "</td>");
//            sb.append("<td>" + user.getName() + "</td>");
//            sb.append("<td>" + user.getEmail() + "</td>");
//            sb.append("</tr>");
//        }
//        sb.append("</table>");
//        response.forwardBody(sb.toString());
//    }
//
//    private void login(HttpRequest request, HttpResponse response) {
//        User user = DataBase.findUserById(request.getParameter("userId"));
//        if (user != null) {
//            if (user.login(request.getParameter("password"))) {
//                response.addHeader("Set-Cookie" , "logined=true");
//                response.sendRedirect("/index.html");
//            } else {
//                response.sendRedirect("/user/login_failed.html");
//            }
//        } else {
//            response.sendRedirect("/user/login_failed.html");
//        }
//    }
//
//    private void createUser(HttpRequest request, HttpResponse response) {
//        User user = new User(
//                request.getParameter("userId"),
//                request.getParameter("password"),
//                request.getParameter("name"),
//                request.getParameter("email")
//        );
//        log.debug("user : {}", user);
//        DataBase.addUser(user);
//        response.sendRedirect("/index.html");
//    }

    private boolean isLogin(String cookieValue) {

        Map<String, String> cookies = HttpRequestUtils.parseCookies(cookieValue);
        String value = cookies.get("logined");
        if (value == null) {
            return false;
        }
        return Boolean.parseBoolean(value);
    }

    private void responseResource(OutputStream out, String url) throws IOException {
        DataOutputStream dos = new DataOutputStream(out);
        byte[] body = Files.readAllBytes(new File("./webapp" + url).toPath());
        response200Header(dos, body.length);
        responseBody(dos, body);
    }

    private void responseCssResource(OutputStream out, String url) throws IOException {
        DataOutputStream dos = new DataOutputStream(out);
        byte[] body = Files.readAllBytes(new File("./webapp" + url).toPath());
        response200CssHeader(dos, body.length);
        responseBody(dos, body);
    }

    private int getContentLength(String line) {
        String[] headerTokens = line.split(":");
        return Integer.parseInt(headerTokens[1].trim());
    }

    private String getDefaultUrl(String url) {

        if (url.equals("/")) {
            url = "/index.html";
        }
        return url;
    }

    private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response200CssHeader(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/css;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response302Header(DataOutputStream dos) {
        try {
            dos.writeBytes("HTTP/1.1 302 Redirect \r\n");
            dos.writeBytes("Location: /index.html \r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response302LoginSuccessHeader(DataOutputStream dos) {
        try {
            dos.writeBytes("HTTP/1.1 302 Redirect \r\n");
            dos.writeBytes("Set-Cookie: logined=true \r\n");
            dos.writeBytes("Location: /index.html \r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.writeBytes("\r\n");
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}