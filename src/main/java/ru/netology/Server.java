package ru.netology;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.List;

public class Server {
    List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");
    private static final ConcurrentHashMap<String, Handler> requests = new ConcurrentHashMap<>();
    private ExecutorService threadPool;

    public Server() {
        addMyFiles();
    }

    public void listen(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            threadPool = Executors.newFixedThreadPool(4);
            while (true) {
                try {
                    final var socket = serverSocket.accept();
                    threadPool.submit(() -> new ConnectedClient(socket).handle());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addHandler(String type, String path, Handler handler) {
        requests.put(type + path, handler);
    }

    public static Handler getRequest(String key) {
        return requests.get(key);
    }


    private void addMyFiles() {
        for (String validPath : validPaths) {
            addHandler("GET", validPath,
                    (request, out) -> sendFile(out, validPath));
        }
    }


    private void sendFile(BufferedOutputStream out, String fileName) throws IOException {
        final var filePath = Path.of(".", "public", fileName);
        final String mimeType = Files.probeContentType(filePath);
        final var length = Files.size(filePath);
        out.write((
                "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: " + mimeType + "\r\n" +
                        "Content-Length: " + length + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        Files.copy(filePath, out);
        out.flush();
    }
}
