package ru.netology;

import java.io.*;
import java.net.Socket;
public class ConnectedClient extends Thread {
    private Socket socket;
    private BufferedReader in;
    private BufferedOutputStream out;

    public ConnectedClient(Socket socket) throws IOException, InterruptedException {
        this.socket = socket;
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new BufferedOutputStream(socket.getOutputStream());
        start();
    }

    @Override
    public void run() {
        try {
            final String requestLine = in.readLine();
            if (requestLine==null){
                badRequest(out);
            }else {
                final var parts = requestLine.split(" ");
                System.out.println(requestLine);
                Request request = new Request(parts[0], parts[1], parts[2]);

                if (parts.length != 3) {
                    badRequest(out);
                } else if (!Server.requests.keySet().contains(request)) {
                    notFound(out);
                } else {
                    Server.requests.get(request).handle(request, out);
                }
            }
        } catch (IOException e) {
            downService();
            throw new RuntimeException(e);
        }
    }
    private static void badRequest(BufferedOutputStream streamOut) {
        try (final var out = streamOut) {
            out.write((
                    "HTTP/1.1 400 Bad Request\r\n" +
                            "Content-Length: 0\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private static void notFound(BufferedOutputStream streamOut) {
        try (final var out = streamOut) {
            out.write((
                    "HTTP/1.1 404 Not Found\r\n" +
                            "Content-Length: 0\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
