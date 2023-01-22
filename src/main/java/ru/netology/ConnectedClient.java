package ru.netology;

import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.util.*;
import java.io.*;
import java.net.Socket;
import java.nio.charset.Charset;

public class ConnectedClient extends Thread {
    private static final String URLENCODED = "Content-Type: application/x-www-form-urlencoded";
    private static final String MULTIPART = "Content-Type: multipart/form-data";
    private final Socket socket;
    private BufferedInputStream in;
    private BufferedOutputStream out;

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
            if (request.getBody() != null) {
                if (request.getHeaders().contains(URLENCODED)) {
                    setURLEncoded(request);
                }
                if (request.getHeaders().contains(MULTIPART + "; boundary=" + request.getBoundary())) {
                    setMultipart(request);
                }
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

    private static void setURLEncoded(Request request) {
        Map<String, List<String>> postParams = new HashMap<>();
        List<NameValuePair> listValue = URLEncodedUtils.parse(request.getBody(), Charset.defaultCharset());
        for (NameValuePair nameValue : listValue) {
            postParams.computeIfAbsent(nameValue.getName(), k -> new ArrayList<>()).add(nameValue.getValue());
        }
        request.setPostParams(postParams);
    }

    private static void setMultipart(Request request) {
        Map<String, List<String>> postParams = new HashMap<>();
        try {
            FileItemFactory factory = new DiskFileItemFactory();
            FileUpload upload = new FileUpload(factory);
            List<FileItem> fileItems = upload.parseRequest(request);
            for (FileItem fileItem : fileItems) {
                if (fileItem.isFormField()) {
                    postParams.computeIfAbsent(fileItem.getFieldName(), k -> new ArrayList<>()).add(fileItem.getString());
                } else {
                    String fileName = fileItem.getName();
                    try {
                        fileItem.write(new File(fileItem.getName()));
                    } catch (Exception e) {
                        System.out.println("Cannot save file " + fileName + "!");
                    }
                }
            }
            request.setPostParams(postParams);
        } catch (FileUploadException e) {
            System.out.println("Cannot parse request!");
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
