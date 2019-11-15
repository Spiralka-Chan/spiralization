package org.secta.spiral.patcher;

import org.jetbrains.annotations.Nullable;
import org.secta.spiral.common.LogUtils;
import org.secta.spiral.common.RemoteFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLConnection;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Patch {
  private static final Logger LOG = LogUtils.getLog();

  private static final String RELEASE_FILE = "release.txt";
  private static final String VERSION_PREFIX = "# version=";

  private final Map<String, RemoteFile> propertyFiles;
  private final Map<String, RemoteFile> commonFiles;

  private final Map<String, Map<String, String>> vars;

  private String patchNotes;
  private String patchVersion;

  public Patch(final Collection<RemoteFile> files) {
    this.propertyFiles = new HashMap<>();
    this.commonFiles = new HashMap<>();

    this.vars = new HashMap<>();
    for (final RemoteFile f : files) {
      if (f.name.equals(RELEASE_FILE)) {
        try {
          parseReleaseInfo(fetchAsString(f));
        } catch (IOException e) {
          LOG.severe("Failed to fetch remote file " + f.name);
          e.printStackTrace();
        }
      } else if (f.name.endsWith(".properties")) {
        this.propertyFiles.put(f.name, f);
      } else {
        this.commonFiles.put(f.path, f);
      }
    }
  }

  public Set<String> getPropertyFiles() {
    return propertyFiles.keySet();
  }

  public Set<String> getCommonFiles() {
    return commonFiles.keySet();
  }

  @Nullable
  public String fetchFile(final String path) {
    if (!commonFiles.containsKey(path)) {
      return null;
    }
    try {
      return fetchAsString(commonFiles.get(path));
    } catch (IOException e) {
      e.printStackTrace();
      LOG.severe("Failed to fetch remote file " + path);
    }
    return null;
  }

  @Nullable
  public String getVersion() {
    return patchVersion;
  }

  @Nullable
  public String getPatchNotes() {
    return patchNotes;
  }

  @Nullable
  public String getRevision(final String fileName) {
    return propertyFiles.containsKey(fileName) ? propertyFiles.get(fileName).sha : null;
  }

  @Nullable
  public String getVar(final String fileName, final String var) {
    final RemoteFile f = propertyFiles.get(fileName);
    if (f == null) {
      return null;
    }
    if (!vars.containsKey(fileName)) {
      vars.put(fileName, new HashMap<>());
      try {
        fetch(f);
      } catch (IOException e) {
        LOG.severe("Failed to fetch file " + f.name);
      }
    }
    return vars.get(fileName).get(var);
  }

  private void parseReleaseInfo(final String releaseTxt) {
    if (releaseTxt.startsWith(VERSION_PREFIX)) {
      final String[] result = releaseTxt.split("\n", 2);
      this.patchVersion = result[0].replaceAll(VERSION_PREFIX, "");
      this.patchNotes = result[1];
    } else {
      this.patchNotes = releaseTxt;
    }
  }

  private void fetch(final RemoteFile file) throws IOException {
    final URLConnection conn = file.downloadUrl.openConnection();
    try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
      String line;
      while ((line = in.readLine()) != null) {
        if (!line.startsWith("#")) {
          String[] tokens = line.split("=", 2);
          if (tokens.length == 2) {
            vars.get(file.name).put(tokens[0], tokens[1]);
          } else {
            LOG.warning("Bad line: '" + line + "' in file " + file.sha);
          }
        }
      }
      LOG.info("Fetched patch for " + file.name + " @ " + file.sha);
    }
  }

  private String fetchAsString(final RemoteFile file) throws IOException {
    final URLConnection conn = file.downloadUrl.openConnection();
    final String r = new BufferedReader(new InputStreamReader(conn.getInputStream())).lines()
        .parallel().collect(Collectors.joining("\n"));
    return r;
  }
}