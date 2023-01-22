package ru.netology;

import org.apache.commons.fileupload.RequestContext;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.*;

public class Request implements RequestContext {
    private final String method;
    private final List<String> headers;
    private final String body;
    private final String path;
    private final String protocol;
    private final String boundary;
    private static List<NameValuePair> queryParams;
    private Map<String, List<String>> postParams;

    public Request(String method, String path, String protocol, List<String> headers, String body, String boundary) {
        this.method = method;
        this.protocol = protocol;
        this.headers = headers;
        this.body = body;
        this.path = path;
        this.boundary = boundary;
    }

    public String getBoundary() {
        return boundary;
    }

    public String getPath() {
        return path;
    }

    public String getBody() {
        return body;
    }

    public String getkey() {
        return method + path;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public void setPostParams(Map<String, List<String>> postParams) {
        this.postParams = postParams;
    }

    public List<String> getQueryParam(String name) {
        List<String> rez = null;
        for (NameValuePair nameValue : queryParams) {
            rez.add(nameValue.getName());
        }
        return rez;
    }

    public List<String> getQueryParams() {
        List<String> rez = null;
        for (NameValuePair nameValue : queryParams) {
            rez.add(nameValue.getName());
        }
        return rez;
    }

    public List<String> getPostParam(String name) {
        return postParams.get(name);
    }

    public Map<String, List<String>> getPostParams() {
        return postParams;
    }

    public static Request parse(BufferedInputStream in) throws IOException {

        final var limit = 4096;

        in.mark(limit);
        final var buffer = new byte[limit];
        final var read = in.read(buffer);

        // ищем request line
        final var requestLineDelimiter = new byte[]{'\r', '\n'};
        final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
        if (requestLineEnd == -1)
            return null;

        // читаем request line
        final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
        if (requestLine.length != 3)
            return null;

        final var method = requestLine[0];
        System.out.println("Метод: " + method);
        final var path = requestLine[1];
        if (!path.startsWith("/"))
            return null;
        System.out.println("Путь: " + path);
        // ищем заголовки
        final var headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
        final var headersStart = requestLineEnd + requestLineDelimiter.length;
        final var headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
        if (headersEnd == -1)
            return null;

        // отматываем на начало буфера
        in.reset();
        // пропускаем requestLine
        in.skip(headersStart);

        final var headersBytes = in.readNBytes(headersEnd - headersStart);
        final var headers = Arrays.asList(new String(headersBytes).split("\r\n"));
        System.out.println("Хэдеры: " + headers);
        String body = null;
        String boundary = null;
        // для GET тела нет
        if (!method.equals("GET")) {
            in.skip(headersDelimiter.length);
            // вычитываем Content-Length, чтобы прочитать body
            final var contentLength = extractHeader(headers, "Content-Length");
            if (contentLength.isPresent()) {
                final var length = Integer.parseInt(contentLength.get());
                final var bodyBytes = in.readNBytes(length);
                body = new String(bodyBytes);
                boundary = body.substring(2, body.indexOf('\n')).trim();
            }
        }
        System.out.println("Тело: " + body);

        if (requestLine[1].contains("?")) {
            final var query = requestLine[1].split("\\?");
            queryParams = URLEncodedUtils.parse(query[1], Charset.defaultCharset());
            return new Request(requestLine[0], query[0], requestLine[2], headers, body, boundary);
        } else
            return new Request(requestLine[0], requestLine[1], requestLine[2], headers, body, boundary);
    }

    private static int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    static Optional<String> extractHeader(List<String> headers, String header) {
        return headers.stream()
                .filter(o -> o.startsWith(header))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim)
                .findFirst();
    }

    @Override
    public String getCharacterEncoding() {return "UTF-8";}
    @Override
    public String getContentType() {return "multipart/form-data, boundary=" + boundary;}
    @Override
    public int getContentLength() {return -1;}
    @Override
    public InputStream getInputStream() {return new ByteArrayInputStream(body.getBytes());}
}

