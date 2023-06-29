package org.jfrog.bamboo;

import org.apache.commons.lang.StringUtils;
import org.jfrog.bamboo.utils.BuildLog;
import org.jfrog.bamboo.utils.OsUtils;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.lang.String.format;
import static org.jfrog.build.client.DownloadResponse.SHA256_HEADER_NAME;

public class JfInstaller {
    private static final String RELEASE = "[RELEASE]";
    public static final String RELEASES_ARTIFACTORY_URL = "https://releases.jfrog.io/artifactory";
    public static final String REPOSITORY = "jfrog-cli";
    public static final String BINARY_NAME = "jf";
    private static final String SHA256_FILE_NAME = "sha256";
    public static final String BIN_JFROG_DIRECTORY = "jfrog";

    private static String getJfrogCliBinaryName() {
        if (OsUtils.isWindows()) {
            return BINARY_NAME + ".exe";
        }
        return BINARY_NAME;
    }


    /**
     * Returns the directory path where the JFrog executable is located based on the specified version.
     * If the version is "RELEASE", the path points to the base JFrog directory.
     * If the version is not "RELEASE", the path includes the version subdirectory within the JFrog directory.
     * If the directory doesn't exist, it will be created.
     *
     * @param version The version of JFrog. Use "RELEASE" for the base directory.
     * @return The absolute path of the JFrog executable directory.
     * @throws IOException If an I/O error occurs while creating the directory.
     */
    private static Path getJfrogExecutableDirectory(String version) throws IOException {
        Path jfExecDir = version.equals(RELEASE) ?
                Paths.get(BIN_JFROG_DIRECTORY) :
                Paths.get(BIN_JFROG_DIRECTORY, version);
        return Files.createDirectories(jfExecDir).toAbsolutePath();
    }

    public static String getJfExecutable(final String providedVersion, BuildLog myBuildLog) throws IOException {
        myBuildLog.info("Getting JFrog CLI executable...");

        // An empty string indicates the latest version.
        String version = StringUtils.defaultIfBlank(providedVersion, RELEASE);

        Path executableLocation = getJfrogExecutableDirectory(version);
        String binaryName = getJfrogCliBinaryName();
        String executableFullPath = Paths.get(executableLocation.toString(), binaryName).toString();

        // Downloading executable from Artifactory
        try (ArtifactoryManager manager = new ArtifactoryManager(RELEASES_ARTIFACTORY_URL, "", "", myBuildLog)) {
            String cliUrlSuffix = String.format("/%s/v2-jf/%s/jfrog-cli-%s/%s", REPOSITORY, version, OsUtils.getOsDetails(), binaryName);
            // Getting updated cli binary's sha256 form Artifactory.
            String artifactorySha256 = manager.downloadHeader(cliUrlSuffix, SHA256_HEADER_NAME);
            // Check whether it's needed to download a new executable, or it already exists on agent
            if (shouldDownloadTool(executableLocation, artifactorySha256)) {
                if (version.equals(RELEASE)) {
                    myBuildLog.info(format("Download '%s' latest version from: %s%n", binaryName, RELEASES_ARTIFACTORY_URL + cliUrlSuffix));
                } else {
                    myBuildLog.info(format("Download '%s' version %s from: %s%n", binaryName, version, RELEASES_ARTIFACTORY_URL + cliUrlSuffix));
                }
                File downloadResponse = manager.downloadToFile(cliUrlSuffix, executableFullPath);
                if (!downloadResponse.setExecutable(true)) {
                    throw new IOException("No permission to add execution permission to binary");
                }
                myBuildLog.info("Successfully downloaded JFrog cli executable: " + downloadResponse.getPath());
                createSha256File(executableLocation, artifactorySha256);
            } else {
                myBuildLog.info("Found existing JFrog CLI executable");
            }
            return executableFullPath;
        } catch (IOException e) {
            throw new IOException("Failed while running download CLI command with error: " + e);
        }
    }

    /**
     * We should skip the download if the tool's directory already contains the specific version, otherwise we should download it.
     * A file named 'sha256' contains the specific binary sha256.
     * If the file sha256 has not changed, we will skip the download, otherwise we will download and overwrite the existing files.
     *
     * @param toolLocation      - expected location of the tool on the fileSystem.
     * @param artifactorySha256 - sha256 of the expected file in artifactory.
     */
    private static boolean shouldDownloadTool(Path toolLocation, String artifactorySha256) throws IOException {
        // In case no sha256 was provided (for example when the customer blocks headers) download the tool.
        if (artifactorySha256.isEmpty()) {
            return true;
        }
        // Looking for the sha256 file in the tool directory.
        Path path = toolLocation.resolve(SHA256_FILE_NAME);
        if (!Files.exists(path)) {
            return true;
        }
        String fileContent = Files.readString(path);
        return !StringUtils.equals(fileContent, artifactorySha256);
    }

    private static void createSha256File(Path toolLocation, String artifactorySha256) throws IOException {
        File file = new File(toolLocation.toFile(), SHA256_FILE_NAME);
        Files.write(file.toPath(), artifactorySha256.getBytes(StandardCharsets.UTF_8));
    }

}
