package com.winlator.d2;

import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

public class GitHubClient {
    public static final String OWNER = "your-github-user";
    public static final String REPO = "your-test-repository";
    private static final String API = "https://api.github.com/repos/" + OWNER + "/" + REPO;

    private final String token;

    public GitHubClient(String token) {
        this.token = token;
    }

    public String getFileText(String path, String ref) throws IOException {
        URL url = new URL(API + "/contents/" + encodePath(path) + "?ref=" + URLEncoder.encode(ref, "UTF-8"));
        HttpURLConnection connection = open(url, "GET");
        connection.setRequestProperty("Accept", "application/vnd.github.raw+json");
        int code = connection.getResponseCode();
        if (code != 200) throw new HttpException("GET " + path + ": HTTP " + code + " " + readError(connection), code);
        return new String(readAll(connection.getInputStream()), StandardCharsets.UTF_8);
    }

    public static class HttpException extends IOException {
        public final int code;

        HttpException(String message, int code) {
            super(message);
            this.code = code;
        }
    }

    public static boolean isRetryable(IOException e) {
        if (!(e instanceof HttpException)) return true;
        int code = ((HttpException) e).code;
        if (code == 403) return String.valueOf(e.getMessage()).toLowerCase(Locale.ROOT).contains("rate limit");
        return code == 408 || code == 409 || code == 429 || code >= 500;
    }

    public void createFile(String path, byte[] content, String message, String branch) throws IOException {
        storeFile(path, content, message, branch, false);
    }

    public void replaceFile(String path, byte[] content, String message, String branch) throws IOException {
        storeFile(path, content, message, branch, true);
    }

    private void storeFile(String path, byte[] content, String message, String branch, boolean replace) throws IOException {
        try {
            putFile(path, content, message, branch, null);
        }
        catch (HttpException e) {
            if (e.code != 422) throw e;
            String sha = fileSha(path, branch);
            if (sha == null) throw e;
            if (sha.equals(blobSha(content))) return;
            if (!replace) throw new HttpException("PUT " + path + ": exists with different content", 422);
            putFile(path, content, message, branch, sha);
        }
    }

    private String fileSha(String path, String ref) throws IOException {
        URL url = new URL(API + "/contents/" + encodePath(path) + "?ref=" + URLEncoder.encode(ref, "UTF-8"));
        HttpURLConnection connection = open(url, "GET");
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        int code = connection.getResponseCode();
        if (code == 404) return null;
        if (code != 200) throw new HttpException("GET " + path + ": HTTP " + code + " " + readError(connection), code);
        try {
            String sha = new JSONObject(new String(readAll(connection.getInputStream()), StandardCharsets.UTF_8)).optString("sha", "");
            return sha.isEmpty() ? null : sha;
        }
        catch (JSONException e) {
            throw new IOException(e);
        }
    }

    static String blobSha(byte[] content) throws IOException {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            sha1.update(("blob " + content.length + "\0").getBytes(StandardCharsets.US_ASCII));
            StringBuilder sb = new StringBuilder();
            for (byte b : sha1.digest(content)) sb.append(String.format(Locale.ROOT, "%02x", b));
            return sb.toString();
        }
        catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
    }

    private void putFile(String path, byte[] content, String message, String branch, String sha) throws IOException {
        JSONObject body = new JSONObject();
        try {
            body.put("message", message);
            body.put("content", Base64.encodeToString(content, Base64.NO_WRAP));
            body.put("branch", branch);
            if (sha != null) body.put("sha", sha);
        }
        catch (JSONException e) {
            throw new IOException(e);
        }

        URL url = new URL(API + "/contents/" + encodePath(path));
        HttpURLConnection connection = open(url, "PUT");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
        connection.setFixedLengthStreamingMode(payload.length);
        try (OutputStream out = connection.getOutputStream()) {
            out.write(payload);
        }
        int code = connection.getResponseCode();
        if (code != 200 && code != 201) throw new HttpException("PUT " + path + ": HTTP " + code + " " + readError(connection), code);
        readAll(connection.getInputStream());
    }

    private HttpURLConnection open(URL url, String method) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(20000);
        connection.setReadTimeout(120000);
        connection.setRequestProperty("Authorization", "Bearer " + token);
        connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
        connection.setRequestProperty("User-Agent", "D2Android-RemoteTest");
        return connection;
    }

    private static String encodePath(String path) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (String segment : path.split("/")) {
            if (segment.isEmpty()) continue;
            if (sb.length() > 0) sb.append('/');
            sb.append(URLEncoder.encode(segment, "UTF-8").replace("+", "%20"));
        }
        return sb.toString();
    }

    private static String readError(HttpURLConnection connection) {
        try {
            InputStream err = connection.getErrorStream();
            if (err == null) return "";
            String text = new String(readAll(err), StandardCharsets.UTF_8);
            return text.length() > 300 ? text.substring(0, 300) : text;
        }
        catch (IOException e) {
            return "";
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        try (InputStream input = in) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[16384];
            int n;
            while ((n = input.read(buffer)) != -1) out.write(buffer, 0, n);
            return out.toByteArray();
        }
    }
}
