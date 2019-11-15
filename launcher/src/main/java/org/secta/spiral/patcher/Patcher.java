package org.secta.spiral.patcher;

import org.secta.spiral.common.LogUtils;
import org.secta.spiral.common.ProgressListener;

import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Patcher {
  private static final Logger LOG = LogUtils.getLog();

  private static final String PATCHED_MARKER = "#spiralized ";
  private static final String CONFIG_TAR_PATH = "code/projectx-config.jar";
  private static final Map<String, String> ENV = new HashMap<>();

  static {
    ENV.put("create", "true");
  }

  private final String gameRootPath;

  public Patcher(final String gameRootPath) {
    this.gameRootPath = gameRootPath;
  }

  public boolean isConfigured() {
    return new File(gameRootPath, CONFIG_TAR_PATH).exists();
  }

  public boolean patchAll(final Patch patch, final ProgressListener progress) throws IOException {
    final URI uri = URI.create("jar:" + new File(gameRootPath, CONFIG_TAR_PATH).toURI());

    int val = 0;
    final float total = patch.getPropertyFiles().size() + patch.getCommonFiles().size();

    try (final FileSystem zipfs = FileSystems.newFileSystem(uri, ENV)) {
      final Stream<Path> walk = Files.walk(zipfs.getPath("rsrc", "i18n"));
      final List<Path> files = walk
          .filter(f -> patch.getPropertyFiles().contains(f.getFileName().toString()))
          .collect(Collectors.toList());

      for (final Path file : files) {
        patchFile(file, patch);
        progress.update(val++ / (total - 1));
      }
    }

    for (final String filePath : patch.getCommonFiles()) {
      final String file = patch.fetchFile(filePath);
      if (file != null) {
        final File dest = new File(gameRootPath, filePath);
        if (!dest.exists()) {
          Files.createDirectories(dest.getParentFile().toPath());
        }
        try (FileWriter writer = new FileWriter(dest)) {
          LOG.info("writing file " + filePath);
          writer.write(file);
          progress.update(val++ / (total - 1));
        } catch (final FileNotFoundException e) {
          throw e;
        }
      }
    }

    return true;
  }

  private void patchFile(final Path file, final Patch patch) throws IOException {
    final String fileName = file.getFileName().toString();
    final String fileVersion = patch.getRevision(fileName);

    if (fileVersion == null) {
      return;
    }

    final File in = File.createTempFile(this.getClass().getCanonicalName(), ".in");
    final File patched = File.createTempFile(this.getClass().getCanonicalName(), ".out");
    Files.copy(file, in.toPath(), StandardCopyOption.REPLACE_EXISTING);
    in.deleteOnExit();
    patched.deleteOnExit();

    BufferedReader b = new BufferedReader(new FileReader(in));
    try (FileWriter fw = new FileWriter(patched)) {
      fw.write(PATCHED_MARKER);
      fw.write(fileVersion);
      fw.write(System.lineSeparator());

      String line;
      while ((line = b.readLine()) != null) {
        if (line.startsWith(PATCHED_MARKER)) {
          String readVersion = line.replaceAll(PATCHED_MARKER, "");
          if (readVersion.equals(fileVersion)) {
            LOG.info("Skip up-to-date file " + file.getFileName());
            return;
          }
        }

        if (!line.startsWith("#")) {
          String[] tokens = line.split("=", 2);
          String var = patch.getVar(fileName, tokens[0]);
          if (tokens.length == 2 && var != null) {
            fw.write(tokens[0]);
            fw.write('=');
            fw.write(var);
          } else {
            fw.write(line);
          }
        } else {
          fw.write(line);
        }
        fw.write(System.lineSeparator());
      }
    }
    Files.copy(patched.toPath(), file, StandardCopyOption.REPLACE_EXISTING);
    LOG.info("Patched file " + file.getFileName());
  }
}