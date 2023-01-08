package ru.netology;

import java.io.*;

public class Main {
    public static void main(String[] args) {
        final var server = new Server();
        // код инициализации сервера (из вашего предыдущего ДЗ)

        // добавление handler'ов (обработчиков)
        server.addHandler("GET", "/message",
                (request,out) ->sendResponse(out,"Hello from GET /message"));

        server.addHandler("POST", "/message",
                (request,out) ->sendResponse(out,"Hello from GET /message"));

        server.listen(9999);
    }

    public static void sendResponse(BufferedOutputStream out, String response) throws IOException {
        out.write((
                "HTTP/1.1 200 OK\r\n" +
                        "Content-Length: " + response.length() + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.write(response.getBytes());
        out.flush();
    }
}


