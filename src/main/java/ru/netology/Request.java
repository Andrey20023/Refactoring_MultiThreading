package ru.netology;


import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URLEncodedUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;


public class Request {

    public static final String GET = "GET";
    public static final String POST = "POST";
    static String method;
    static private String path;
    private final List<String> headers;
    private List<NameValuePair> params;


    public Request(String method, String path) {
        Request.method = method;
        Request.path = path;
        headers = null;
    }

    public Request(String method, String path, List<String> headers, List<NameValuePair> params) {
        Request.method = method;
        Request.path = path;
        this.headers = headers;
        this.params = params;
    }

    static Request createRequest(BufferedInputStream in) throws IOException, URISyntaxException {
        final List<String> allowedMethods = List.of(GET, POST);
        final var limit = 4096;
        in.mark(limit);
        final var buffer = new byte[limit];
        final var read = in.read(buffer);

        // ищем request line
        final var requestLineDelimiter = new byte[]{'\r', '\n'};
        final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
        if (requestLineEnd == -1) {
            return null;
        }
        // читаем request line
        final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
        if (requestLine.length != 3) {
            return null;
        }

        final var method = requestLine[0];
        if (!allowedMethods.contains(method)) {
            return null;
        }

        final var path = requestLine[1];
        System.out.println("path = " + path);

        /*// ищем заголовки
        final var headerDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
        final var headersStart = requestLineEnd + requestLineDelimiter.length;
        System.out.println("headersStart = "+ headersStart);

        final var headersEnd = indexOf(buffer, headerDelimiter, headersStart, read);
        System.out.println("headersEnd = "+ headersEnd);
        if (headersEnd == -1) {
            return null;
        }
        // отматываем на начало буфера
        in.reset();
        // пропускаем requestLine
        in.skip(headersStart);

        final var headersBytes = in.readNBytes(headersEnd - headersStart);

        List<String> headers = Arrays.asList(new String(headersBytes).split("\r\n"));*/

        List<String> headers = null;
        List<NameValuePair> params = URLEncodedUtils.parse(new URI(path), StandardCharsets.UTF_8);
        return new Request(method, path, headers, params);
    }

    private static int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    public String getPath() {
        return path;
    }

    public String getMethod() {
        return method;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public List<NameValuePair> getQueryParams() {
        return params;
    }

    public NameValuePair getQueryParam(String name) {
        return getQueryParams().stream()
                .filter(param -> param.getName().equalsIgnoreCase(name))
                .findFirst().orElse(new NameValuePair() {
                    public String getName() {
                        return null;
                    }

                    public String getValue() {
                        return null;
                    }
                });
    }
}
