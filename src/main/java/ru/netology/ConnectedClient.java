package ru.netology;

import java.io.*;
import java.net.Socket;

public class ConnectedClient extends Thread {
    private final Socket socket;
    private  BufferedInputStream in;
    private  BufferedOutputStream out;

    public ConnectedClient(Socket socket) {
        this.socket = socket;

    }

    public void handle() {
        try {
            System.out.println(Thread.currentThread().getName());
            in = new BufferedInputStream(socket.getInputStream());
            out = new BufferedOutputStream(socket.getOutputStream());
            var request = Request.parse(in);
            if (request == null) {
                badRequest(out);
                return;
            }
            var handler = Server.getRequest(request.getkey());
            if (handler == null) {
                notFound(out);
                return;
            }
            try {
                handler.handle(request, out);
            } catch (Exception e) {
                System.out.println(e.getMessage());
                send500InternalServerError(out);
            }
        } catch (IOException e) {
            downService();
            throw new RuntimeException(e);
        }
    }

    private static void badRequest(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 400 Bad Request\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();

    }

    private static void notFound(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 404 Not Found\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

    private static void send500InternalServerError(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 404 Not Found\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

    private void downService() {
        try {
            if (!socket.isClosed()) {
                socket.close();
                in.close();
                out.close();
            }
        } catch (
                IOException ignored) {
        }
    }
}
