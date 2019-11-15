package org.secta.spiral.common;

import java.net.URL;

public class RemoteFile {
    public final String name;
    public final String sha;
    public final String path;
    public final URL downloadUrl;
    public final int size;

    public RemoteFile(final String name, final String sha, final String path, final URL downloadUrl, int size) {
        this.name = name;
        this.sha = sha;
        this.path = path;
        this.downloadUrl = downloadUrl;
        this.size = size;
    }
}
