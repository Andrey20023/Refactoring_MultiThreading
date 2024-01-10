package ru.netology;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Handler {
    public static final String GET = "GET";
    public static final String POST = "POST";//используем только неизменяемые методы GET и POST
    final List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");
    //список эндпойнтов их исходной задачи, не трогаем
    int portNumber;
    ExecutorService executor;
    String notFound = "HTTP/1.1 404 Not Found\r\n" +
            "Content-Length: 0\r\n" +
            "Connection: close\r\n" +
            "\r\n";
    private final Map<String, Map<String, Handler>> handlers = Map.of(GET, new HashMap<>(), POST, new HashMap<>());
    public Server(int portNumber) {
        this.executor = Executors.newFixedThreadPool(64);
        this.portNumber = portNumber;

    }
    public void listen() {
        executor.execute(() -> startingSocket(portNumber));
    } // метод для передачи прослушиваемого порта

    public void startingSocket(int port) {
        try (final var serverSocket = new ServerSocket(port)) {
            while (true) {
                try (
                        final var socket = serverSocket.accept();
                        final var in = new BufferedInputStream((socket.getInputStream()));
                        final var out = new BufferedOutputStream(socket.getOutputStream())
                ) {

                    Request request = Request.createRequest(in);
                    System.out.println(request);
                    if (request == null) {
                        out.write((notFound).getBytes());
                        out.flush();
                        continue;

                    }
                    final var methodHTTP = request.getMethod();
                    final var path = request.getPath().split("\\?")[0];
                    System.out.println(path);
                    final var filePath = Path.of(".", "public", path);
                    final var mimeType = Files.probeContentType(filePath);

                    if (handlers.get(methodHTTP).containsKey(path)) {
                        handlers.get(methodHTTP).get(path).handle(request, out);
                        continue;
                    }
                    if (!validPaths.contains(path) && !handlers.get(POST).containsKey(path) && !handlers.get(GET).containsKey(path)) {
                        out.write((notFound).getBytes());
                        out.flush();
                        continue;
                    }
                    if (handlers.get(methodHTTP).containsKey(path)) {
                        handlers.get(methodHTTP).get(path).handle(request, out);
                        continue;
                    }

                    // special case for classic
                    if (path.equals("/classic.html")) {
                        final var template = Files.readString(filePath);
                        final var content = template.replace(
                                "{time}",
                                LocalDateTime.now().toString()
                        ).getBytes();
                        out.write((
                                "HTTP/1.1 200 OK\r\n" +
                                        "Content-Type: " + mimeType + "\r\n" +
                                        "Content-Length: " + content.length + "\r\n" +
                                        "Connection: close\r\n" +
                                        "\r\n"
                        ).getBytes());
                        out.write(content);
                        out.flush();
                        continue;
                    }
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
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (
                IOException e) {
            e.printStackTrace();
        }
    }

    public void addHandler(String method, String path, Handler handler) {
        if (handlers.containsKey(method)) {
            handlers.get(method).put(path, handler);

        } else {
            System.out.println("Сервер принимает запросы только по методам GET и POST");
        }
    }

    @Override
    public void handle(Request request, BufferedOutputStream responseStream) throws IOException {
        /*final var path = request.getPath();
        final var filePath = Path.of(".", "public", path);
        final var mimeType = Files.probeContentType(filePath);
        final var length = Files.size(filePath);*/
    }
}

