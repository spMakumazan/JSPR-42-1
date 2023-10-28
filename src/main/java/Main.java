import java.io.BufferedOutputStream;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        Server server = new Server(64);
        server.addHandler("GET", "/messages", new Handler() {
            @Override
            public void handle(Request request, BufferedOutputStream responseStream) {
                try (responseStream) {
                    String hello = "Hello from GET";
                    responseStream.write((
                            "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type: " + "text/plain" + "\r\n" +
                                    "Content-Length: " + hello.length() + "\r\n" +
                                    "Connection: close\r\n" +
                                    "\r\n"
                    ).getBytes());
                    responseStream.write(hello.getBytes());
                    responseStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        server.addHandler("POST", "/messages", new Handler() {
            @Override
            public void handle(Request request, BufferedOutputStream responseStream) {
                try (responseStream) {
                    String hello = "Hello from POST";
                    responseStream.write((
                            "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type: " + "text/plain" + "\r\n" +
                                    "Content-Length: " + hello.length() + "\r\n" +
                                    "Connection: close\r\n" +
                                    "\r\n"
                    ).getBytes());
                    responseStream.write(hello.getBytes());
                    responseStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        server.listen(9999);
    }
}


