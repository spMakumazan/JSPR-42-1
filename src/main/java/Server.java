import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Handler>> handlers = new ConcurrentHashMap<>();
    private ExecutorService threadPool;

    public Server(int threadPoolSize) {
        threadPool = Executors.newFixedThreadPool(threadPoolSize);
    }

    public void listen(int port) {
        try (final var serverSocket = new ServerSocket(port)) {
            while (true) {
                final var socket = serverSocket.accept();
                threadPool.execute(connect(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Runnable connect(Socket socket) {
        return () -> {
            try (
                    socket;
                    final var in = new BufferedInputStream(socket.getInputStream());
                    final var out = new BufferedOutputStream(socket.getOutputStream())
            ) {
                final Request request = Request.parse(in);

                if (request == null) {
                    out.write((
                            "HTTP/1.1 400 BAD REQUEST\r\n" +
                                    "Content-Length: " + 0 + "\r\n" +
                                    "Connection: close\r\n" +
                                    "\r\n"
                    ).getBytes());
                    out.flush();
                    return;
                }

                System.out.println(request);

                var methodMap = handlers.get(request.getMethod());
                if (methodMap == null) {
                    out.write((
                            "HTTP/1.1 404 NOT FOUND\r\n" +
                                    "Content-Length: " + 0 + "\r\n" +
                                    "Connection: close\r\n" +
                                    "\r\n"
                    ).getBytes());
                    out.flush();
                    return;
                }

                var handler = methodMap.get(request.getPath().split("\\?")[0]);
                if (handler == null) {
                    out.write((
                            "HTTP/1.1 404 NOT FOUND\r\n" +
                                    "Content-Length: " + 0 + "\r\n" +
                                    "Connection: close\r\n" +
                                    "\r\n"
                    ).getBytes());
                    out.flush();
                    return;
                }

                handler.handle(request, out);

            } catch (IOException e) {
                e.printStackTrace();
            }
        };
    }

    public void addHandler(String method, String path, Handler handler) {
        var methodMap = handlers.get(method);
        if (methodMap == null) {
            methodMap = new ConcurrentHashMap<>();
            methodMap.put(path, handler);
            handlers.put(method, methodMap);
            return;
        }
        if (methodMap.get(path) == null) {
            methodMap.put(path, handler);
        }
    }
}
