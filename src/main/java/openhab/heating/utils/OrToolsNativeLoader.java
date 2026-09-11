package openhab.heating.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import com.sun.jna.Platform;

@NonNullByDefault
public final class OrToolsNativeLoader {
    private static final String JNI_LIBRARY = "jniortools";
    private static boolean loaded;

    private OrToolsNativeLoader() {
    }

    public static synchronized void load() {
        if (loaded) {
            return;
        }

        Bundle bundle = FrameworkUtil.getBundle(OrToolsNativeLoader.class);
        if (bundle == null) {
            try {
                Path nativeDirectory = Files.createTempDirectory("ortools-");
                nativeDirectory.toFile().deleteOnExit();
                List<Path> libraries = copyClasspathLibraries(resourceRoot(), nativeDirectory);
                Path jniLibrary = nativeDirectory.resolve(System.mapLibraryName(JNI_LIBRARY));
                loadDependencies(libraries, jniLibrary);
                System.load(jniLibrary.toAbsolutePath().toString());
                loaded = true;
            } catch (IOException | RuntimeException e) {
                throw new IllegalStateException("Unable to load OR-Tools native libraries", e);
            }
            return;
        }

        String resourceRoot = resourceRoot();
        try {
            Path nativeDirectory = Files.createTempDirectory("ortools-");
            nativeDirectory.toFile().deleteOnExit();
            List<Path> libraries = copyLibraries(bundle, resourceRoot, nativeDirectory);
            loadDependencies(libraries, nativeDirectory.resolve(System.mapLibraryName(JNI_LIBRARY)));
            System.load(nativeDirectory.resolve(System.mapLibraryName(JNI_LIBRARY)).toAbsolutePath().toString());
            loaded = true;
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("Unable to load OR-Tools native libraries", e);
        }
    }

    private static String resourceRoot() {
        return "ortools-" + Platform.RESOURCE_PREFIX;
    }

    private static List<Path> copyClasspathLibraries(String resourceRoot, Path nativeDirectory) throws IOException {
        ClassLoader classLoader = OrToolsNativeLoader.class.getClassLoader();
        if (classLoader == null) {
            throw new IllegalStateException("Failed to get class loader for OrToolsNativeLoader class");
        }
        Enumeration<URL> resources = classLoader.getResources(resourceRoot);
        if (!resources.hasMoreElements()) {
            throw new IOException("Classpath resource directory not found: " + resourceRoot);
        }

        List<Path> libraries = new ArrayList<>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            if ("file".equals(resource.getProtocol())) {
                try (var entries = Files.list(Path.of(resource.toURI()))) {
                    entries.filter(Files::isRegularFile).forEach(source -> {
                        try {
                            libraries.add(copyLibrary(source, nativeDirectory));
                        } catch (IOException e) {
                            throw new IllegalStateException(e);
                        }
                    });
                } catch (java.net.URISyntaxException e) {
                    throw new IOException(e);
                }
            } else if ("jar".equals(resource.getProtocol())) {
                JarURLConnection connection = (JarURLConnection) resource.openConnection();
                try (JarFile jarFile = connection.getJarFile()) {
                    String prefix = resourceRoot + "/";
                    Enumeration<JarEntry> entries = jarFile.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        if (!entry.isDirectory() && entry.getName().startsWith(prefix)) {
                            String name = entry.getName().substring(prefix.length());
                            try (InputStream input = jarFile.getInputStream(entry)) {
                                libraries.add(copyLibrary(input, name, nativeDirectory));
                            }
                        }
                    }
                }
            } else {
                throw new IOException("Unsupported classpath resource URL: " + resource);
            }
        }
        return libraries.stream().distinct().toList();
    }

    private static Path copyLibrary(Path source, Path nativeDirectory) throws IOException {
        Path destination = nativeDirectory.resolve(source.getFileName().toString());
        if (Files.exists(destination)) {
            return destination;
        }
        Files.copy(source, destination);
        destination.toFile().deleteOnExit();
        return destination;
    }

    private static Path copyLibrary(InputStream source, String name, Path nativeDirectory) throws IOException {
        Path destination = nativeDirectory.resolve(name).normalize();
        if (!destination.getParent().equals(nativeDirectory)) {
            throw new IOException("Invalid native library resource: " + name);
        }
        if (Files.exists(destination)) {
            return destination;
        }
        Files.copy(source, destination);
        destination.toFile().deleteOnExit();
        return destination;
    }

    private static List<Path> copyLibraries(Bundle bundle, String resourceRoot, Path nativeDirectory)
            throws IOException {
        Enumeration<URL> entries = bundle.findEntries(resourceRoot, "*", false);
        if (entries == null) {
            throw new IOException("Bundle resource directory not found: " + resourceRoot);
        }

        List<Path> libraries = new ArrayList<>();
        while (entries.hasMoreElements()) {
            URL entry = entries.nextElement();
            String name = entry.getPath().substring(entry.getPath().lastIndexOf('/') + 1);
            if (name.isEmpty()) {
                continue;
            }
            Path destination = nativeDirectory.resolve(name).normalize();
            if (!destination.getParent().equals(nativeDirectory)) {
                throw new IOException("Invalid native library resource: " + entry);
            }
            try (InputStream input = entry.openStream()) {
                Files.copy(input, destination);
            }
            destination.toFile().deleteOnExit();
            libraries.add(destination);
        }
        return libraries;
    }

    private static void loadDependencies(List<Path> libraries, Path jniLibrary) {
        List<Path> remaining = new ArrayList<>(libraries);
        remaining.remove(jniLibrary);

        boolean progress;
        do {
            progress = false;
            for (Path library : List.copyOf(remaining)) {
                try {
                    System.load(library.toAbsolutePath().toString());
                    remaining.remove(library);
                    progress = true;
                } catch (UnsatisfiedLinkError e) {
                    // Its dependencies may be loaded during a later pass.
                }
            }
        } while (progress && !remaining.isEmpty());
    }
}
