package Utils;

import java.awt.Desktop;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class GoogleDriveUploader {

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String DRIVE_UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files";
    private static final String DRIVE_FILES_URL = "https://www.googleapis.com/drive/v3/files";
    private static final String SCOPE = "https://www.googleapis.com/auth/drive.file";
    private static final int REDIRECT_PORT = 8765;
    private static final String REDIRECT_URI = "http://localhost:" + REDIRECT_PORT;

    private final String clientId;
    private final String clientSecret;
    private String refreshToken;

    public GoogleDriveUploader(String clientId, String clientSecret, String refreshToken) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.refreshToken = refreshToken;
    }

    public boolean isAuthorized() {
        return clientId != null && !clientId.isEmpty()
                && clientSecret != null && !clientSecret.isEmpty()
                && refreshToken != null && !refreshToken.isEmpty();
    }

    /**
     * Opens the browser for Google sign-in and starts a local server to capture
     * the OAuth2 redirect. Blocks until the user completes authorization or 2 minutes
     * elapse. Returns the new refresh token.
     */
    public String authorize() throws IOException, InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> authCodeRef = new AtomicReference<>();
        AtomicReference<String> errorRef = new AtomicReference<>();

        ServerSocket serverSocket = new ServerSocket(REDIRECT_PORT);
        serverSocket.setSoTimeout(120_000);

        Thread serverThread = new Thread(() -> {
            try (Socket client = serverSocket.accept()) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
                String requestLine = reader.readLine();
                if (requestLine != null && requestLine.contains("code=")) {
                    int start = requestLine.indexOf("code=") + 5;
                    int end = requestLine.indexOf('&', start);
                    if (end == -1) end = requestLine.indexOf(' ', start);
                    authCodeRef.set(requestLine.substring(start, end));
                } else {
                    errorRef.set("Authorization denied by user");
                }
                PrintWriter pw = new PrintWriter(new OutputStreamWriter(client.getOutputStream(), "UTF-8"), true);
                pw.print("HTTP/1.1 200 OK\r\nContent-Type: text/html\r\n\r\n");
                pw.print("<html><body><h2>Authorization successful! You can close this window.</h2></body></html>");
                pw.flush();
            } catch (Exception e) {
                errorRef.set(e.getMessage());
            } finally {
                latch.countDown();
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();

        try {
            String authUrl = AUTH_URL
                    + "?client_id=" + URLEncoder.encode(clientId, "UTF-8")
                    + "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, "UTF-8")
                    + "&response_type=code"
                    + "&scope=" + URLEncoder.encode(SCOPE, "UTF-8")
                    + "&access_type=offline"
                    + "&prompt=consent";
            Desktop.getDesktop().browse(new URI(authUrl));
        } catch (URISyntaxException e) {
            throw new IOException("Invalid auth URL", e);
        }

        latch.await(2, TimeUnit.MINUTES);
        try { serverSocket.close(); } catch (IOException ignored) {}

        String authCode = authCodeRef.get();
        if (authCode == null) {
            String err = errorRef.get();
            throw new IOException(err != null ? err : "Authorization timed out or was cancelled");
        }

        return exchangeCodeForRefreshToken(authCode);
    }

    private String exchangeCodeForRefreshToken(String authCode) throws IOException {
        String body = "code=" + URLEncoder.encode(authCode, "UTF-8")
                + "&client_id=" + URLEncoder.encode(clientId, "UTF-8")
                + "&client_secret=" + URLEncoder.encode(clientSecret, "UTF-8")
                + "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, "UTF-8")
                + "&grant_type=authorization_code";
        String response = httpPost(TOKEN_URL, body);
        String token = extractJsonString(response, "refresh_token");
        if (token == null) throw new IOException("No refresh token in response: " + response);
        this.refreshToken = token;
        return token;
    }

    private String getAccessToken() throws IOException {
        String body = "client_id=" + URLEncoder.encode(clientId, "UTF-8")
                + "&client_secret=" + URLEncoder.encode(clientSecret, "UTF-8")
                + "&refresh_token=" + URLEncoder.encode(refreshToken, "UTF-8")
                + "&grant_type=refresh_token";
        String response = httpPost(TOKEN_URL, body);
        String token = extractJsonString(response, "access_token");
        if (token == null) throw new IOException("Failed to get access token: " + response);
        return token;
    }

    /**
     * Uploads the given file to Google Drive. If a file with the same name already
     * exists (and is not trashed), its content is replaced. Returns the Drive file ID.
     */
    public String uploadFile(File file) throws IOException {
        String accessToken = getAccessToken();
        String existingId = findFileByName(file.getName(), accessToken);
        if (existingId != null) {
            return multipartUpload("PATCH",
                    DRIVE_UPLOAD_URL + "/" + existingId + "?uploadType=multipart",
                    "{\"name\":\"" + file.getName() + "\"}", file, accessToken);
        } else {
            return multipartUpload("POST",
                    DRIVE_UPLOAD_URL + "?uploadType=multipart",
                    "{\"name\":\"" + file.getName() + "\"}", file, accessToken);
        }
    }

    private String findFileByName(String name, String accessToken) throws IOException {
        String query = "name='" + name.replace("'", "\\'") + "' and trashed=false";
        String url = DRIVE_FILES_URL + "?q=" + URLEncoder.encode(query, "UTF-8") + "&fields=files(id)";
        String response = httpGet(url, accessToken);
        int filesIdx = response.indexOf("\"files\"");
        if (filesIdx == -1) return null;
        return extractJsonString(response.substring(filesIdx), "id");
    }

    private String multipartUpload(String method, String url, String metadata, File file, String accessToken) throws IOException {
        String boundary = "------GoogleDriveBoundary";
        byte[] fileBytes = Files.readAllBytes(file.toPath());

        ByteArrayOutputStream body = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(body, "UTF-8"), true);
        pw.print("--" + boundary + "\r\n");
        pw.print("Content-Type: application/json; charset=UTF-8\r\n\r\n");
        pw.print(metadata + "\r\n");
        pw.print("--" + boundary + "\r\n");
        pw.print("Content-Type: application/octet-stream\r\n\r\n");
        pw.flush();
        body.write(fileBytes);
        pw = new PrintWriter(new OutputStreamWriter(body, "UTF-8"), true);
        pw.print("\r\n--" + boundary + "--\r\n");
        pw.flush();
        byte[] bodyBytes = body.toByteArray();

        URL urlObj = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
        // HttpURLConnection doesn't support PATCH — use POST with override header instead
        if ("PATCH".equals(method)) {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("X-HTTP-Method-Override", "PATCH");
        } else {
            conn.setRequestMethod(method);
        }
        conn.setDoOutput(true);
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setRequestProperty("Content-Type", "multipart/related; boundary=" + boundary);
        conn.setRequestProperty("Content-Length", String.valueOf(bodyBytes.length));
        try (OutputStream os = conn.getOutputStream()) {
            os.write(bodyBytes);
        }

        int code = conn.getResponseCode();
        InputStream is = code < 400 ? conn.getInputStream() : conn.getErrorStream();
        String response = readStream(is);
        if (code >= 400) throw new IOException("Drive API error " + code + ": " + response);
        return extractJsonString(response, "id");
    }

    private String httpPost(String url, String body) throws IOException {
        URL urlObj = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes("UTF-8"));
        }
        int code = conn.getResponseCode();
        return readStream(code < 400 ? conn.getInputStream() : conn.getErrorStream());
    }

    private String httpGet(String url, String accessToken) throws IOException {
        URL urlObj = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        int code = conn.getResponseCode();
        return readStream(code < 400 ? conn.getInputStream() : conn.getErrorStream());
    }

    private String readStream(InputStream is) throws IOException {
        if (is == null) return "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line).append('\n');
        return sb.toString();
    }

    // Minimal JSON string value extractor (no library dependency needed for these simple responses)
    private String extractJsonString(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx == -1) return null;
        int colon = json.indexOf(':', idx + search.length());
        if (colon == -1) return null;
        int valueStart = colon + 1;
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) valueStart++;
        if (valueStart >= json.length() || json.charAt(valueStart) != '"') return null;
        valueStart++;
        int valueEnd = json.indexOf('"', valueStart);
        if (valueEnd == -1) return null;
        return json.substring(valueStart, valueEnd);
    }

    public String getRefreshToken() {
        return refreshToken;
    }
}
