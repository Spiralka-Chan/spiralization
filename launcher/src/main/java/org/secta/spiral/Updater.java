package org.secta.spiral;

import org.secta.spiral.common.Constants;
import org.secta.spiral.common.RemoteFile;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

public class Updater {

    private static File update(final String path) {
        final GithubClient client = new GithubClient();

        try {
            final Collection<RemoteFile> files = client.getFiles(Constants.GITHUB_USER, Constants.GITHUB_REPO, Constants.DISTRO_PATH);
            final Optional<RemoteFile> latest = files.parallelStream()
                    .filter(f -> f.name.startsWith("launcher-") && f.name.endsWith(".jar"))
                    .max(Comparator.comparing((RemoteFile f) -> f.name));

            if (latest.isPresent()) {
                try (BufferedInputStream in = new BufferedInputStream(latest.get().downloadUrl.openStream());
                     FileOutputStream fileOutputStream = new FileOutputStream(path)) {
                    byte dataBuffer[] = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                        fileOutputStream.write(dataBuffer, 0, bytesRead);
                    }
                }
            }
        } catch (final IOException e) {
            // TODO: log - bida
        }

        return new File(path);
    }

    public static void main(String[] args) {
        final String jarPath = args.length > 0 ? args[0] : "./launcher.jar";
        final File jar = new File(jarPath);//update("./launcher.jar");

        if (jar.exists()) {
            try {
                final URLClassLoader child = new URLClassLoader(
                        new URL[]{jar.toURI().toURL()}, Updater.class.getClassLoader()
                );
                Class classToLoad = Class.forName("org.secta.spiral.Launcher", true, child);
                Method method = classToLoad.getDeclaredMethod("main", String[].class);
                final Object[] argz = new Object[1];
                argz[0] = new String[]{};
                method.invoke(null, argz);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            // TODO: report failure
            System.exit(1);
        }
    }
}