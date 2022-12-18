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
    List validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");
    private final ExecutorService threadPool = Executors.newFixedThreadPool(64);
    public static ConcurrentHashMap<Request, Handler> requests = new ConcurrentHashMap<>();

    public Server() {
        for (int i = 0; i < validPaths.size(); i++) {
            Request request = new Request("GET", (String) validPaths.get(i), "HTTP/1.1");
            requests.put(request, new Handler() {
                //@Override
                public void handle(Request request, BufferedOutputStream responseStream) {
                    getFile(request, responseStream);
                }
            });
        }
    }

    public void listen(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                threadPool.submit(new ConnectedClient(serverSocket.accept()));
            }
        } catch (IOException | InterruptedException e) {

        }
    }

    public void addHandler(String type, String path, Handler handler) {
        Request request = new Request(type, path, "HTTP/1.1");
        requests.put(request, handler);
    }

    private void getFile(Request request, BufferedOutputStream streamOut) {
        final var filePath = Path.of(".", "public", request.getPath());
        try {
            final String mimeType = Files.probeContentType(filePath);
            final var out = streamOut;
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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
