package org.secta.spiral;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.secta.spiral.common.RemoteFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;

public class GithubClient {
    private static URL GITHUB_API;

    static {
        try {
            GITHUB_API = new URL("https://api.github.com");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    private final URL apiUrl;

    public GithubClient() {
        this(GITHUB_API);
    }

    public GithubClient(final URL apiUrl) {
        this.apiUrl = apiUrl;
    }

    public Collection<RemoteFile> getFiles(final String user, final String repo) throws IOException {
        return fetchFiles(user, repo, "");
    }

    private Collection<RemoteFile> fetchFiles(final String user, final String repo, final String path) throws IOException {
        final Collection<RemoteFile> result = new ArrayList<>();
        final JsonNode files = request("/repos/" + user + "/" + repo + "/contents/" + path);
        for (final JsonNode file : files) {
            try {
                final String type = file.get("type").asText();
                if (type.equals("file")) {
                    result.add(
                            new RemoteFile(
                                    file.get("name").asText(),
                                    file.get("sha").asText(),
                                    file.get("path").asText(),
                                    new URL(file.get("download_url").asText()),
                                    file.get("size").asInt()
                            )
                    );
                } else if (type.equals("dir")) {
                    result.addAll(fetchFiles(user, repo, file.get("path").asText()));
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    private JsonNode request(final String path) throws IOException {
        final URL url = new URL(this.apiUrl, path);
        final URLConnection conn = url.openConnection();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readTree(in);
        }
    }
}
