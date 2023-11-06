import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class Request {

    public static final String GET = "GET";
    public static final String POST = "POST";
    final static List<String> allowedMethods = List.of(GET, POST);
    private String method;
    private String path;
    private List<NameValuePair> queryParams;
    private String version;
    private List<String> headers;
    private String body;
    private List<NameValuePair> postParams;
    private List<FileItem> parts;

    public Request(String method, String path, String version, List<String> headers, String body,
                   List<NameValuePair> queryParams, List<NameValuePair> postParams, List<FileItem> parts) {
        this.method = method;
        this.path = path;
        this.version = version;
        this.headers = headers;
        this.body = body;
        this.queryParams = queryParams;
        this.postParams = postParams;
        this.parts = parts;
    }

    public static Request parse(BufferedInputStream in) throws IOException {
        final var limit = 4096;
        in.mark(limit);
        final var buffer = new byte[limit];
        final var read = in.read(buffer);

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
        if (!path.startsWith("/")) {
            return null;
        }

        List<NameValuePair> queryParams = null;
        final var pathParts = path.split("\\?");
        if (pathParts.length == 2) {
            queryParams = URLEncodedUtils.parse(pathParts[1], StandardCharsets.UTF_8);
        }

        // ищем заголовки
        final var headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
        final var headersStart = requestLineEnd + requestLineDelimiter.length;
        final var headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
        if (headersEnd == -1) {
            return null;
        }

        // отматываем на начало буфера
        in.reset();
        // пропускаем requestLine
        in.skip(headersStart);

        final var headersBytes = in.readNBytes(headersEnd - headersStart);
        final var headers = Arrays.asList(new String(headersBytes).split("\r\n"));

        // для GET тела нет
        String body = null;
        List<NameValuePair> postParams = null;
        if (!method.equals(GET)) {
            in.skip(headersDelimiter.length);
            // вычитываем Content-Length, чтобы прочитать body
            final var contentLength = extractHeader(headers, "Content-Length");
            if (contentLength.isPresent()) {
                final var length = Integer.parseInt(contentLength.get());
                final var bodyBytes = in.readNBytes(length);
                body = new String(bodyBytes);
                postParams = URLEncodedUtils.parse(body, StandardCharsets.UTF_8);
            }
        }

        HttpServletRequest servletRequest = ;
        FileItemFactory factory = new DiskFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload(factory);
        List<FileItem> parts = upload.parseRequest(servletRequest);

        return new Request(requestLine[0], requestLine[1], requestLine[2], headers, body, queryParams, postParams, parts);
    }

    private static Optional<String> extractHeader(List<String> headers, String header) {
        return headers.stream()
                .filter(o -> o.startsWith(header))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim)
                .findFirst();
    }

    // from google guava with modifications
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

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getVersion() {
        return version;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }

    public List<NameValuePair> getQueryParams() {
        return queryParams;
    }

    public String getQueryParam(String name) {
        for (NameValuePair nameValuePair : queryParams) {
            if (nameValuePair.getName().equals(name)) {
                return nameValuePair.getValue();
            }
        }
        return null;
    }

    public List<NameValuePair> getPostParams() {
        return postParams;
    }

    public List<NameValuePair> getPostParam(String name) {
        if (postParams != null) {
            return postParams.
                    stream().
                    filter(nameValuePair -> nameValuePair.getName().equals(name)).
                    toList();
        }
        return null;
    }

    public List<FileItem> getParts() {
        return parts;
    }

    public List<FileItem> getPart(String name) {
        if (parts != null) {

        }
        return null;
    }

    @Override
    public String toString(){
        return method + "  " + path + "  " + version + "\n" + queryParams + "\n" + headers + "\n" + body + "\n" + postParams + "\n";
    }
}
