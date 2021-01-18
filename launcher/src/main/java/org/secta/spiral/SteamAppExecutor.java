package org.secta.spiral;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static java.awt.Desktop.getDesktop;
import static java.util.Arrays.asList;

public class SteamAppExecutor {
  private final String steamInstallationPath;
  private final boolean useSteamProtocol;

  public SteamAppExecutor(final String steamPath, boolean useSteamProtocol) {
    this.steamInstallationPath = steamPath;
    this.useSteamProtocol = useSteamProtocol;
  }

  public SteamAppExecutor() {
    this(null, true);
  }

  public void startGameById(final String id) throws Exception {
    if (useSteamProtocol) {
      Desktop desktop = getDesktop();
      final URI steamProtocol = new URI("steam://run/" + id);
      desktop.browse(steamProtocol);
    } else {
      startProcess("-applaunch", id);
    }
  }

  private void startProcess(String... arguments) throws IOException {
    final List<String> allArguments = new ArrayList<>();
    allArguments.add(steamInstallationPath);
    final List<String> argumentsList = asList(arguments);
    allArguments.addAll(argumentsList);
    final ProcessBuilder process = new ProcessBuilder(allArguments);
    process.start();
  }
}
