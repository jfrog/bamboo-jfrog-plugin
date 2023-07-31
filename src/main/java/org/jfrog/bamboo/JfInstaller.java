package org.jfrog.bamboo;

import org.apache.commons.lang.StringUtils;
import org.jfrog.bamboo.config.ServerConfig;
import org.jfrog.bamboo.utils.BuildLog;
import org.jfrog.bamboo.utils.OsUtils;
import org.jfrog.build.client.DownloadResponse;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The JFrog CLI installer.
 */
public class JfInstaller {
    private static final String RELEASE = "[RELEASE]";
    private static final String DEFAULT_CLI_VERSION = "2.44.0";
    public static final String RELEASES_URL = "https://releases.jfrog.io";
    public static final String BINARY_NAME = "jf";
    private static final String SHA256_FILE_NAME = "sha256";
    public static final String JFROG_CLI_DIRECTORY = "jfrog-cli";

    /**
     * Returns the JFrog CLI binary name based on the operating system.
     *
     * @return The JFrog CLI binary name.
     */
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
    private static Path getJfrogExecutableDirectory(String jfrogTmpDir, String version) throws IOException {
        Path jfExecDir = version.equals(RELEASE) ?
                Paths.get(jfrogTmpDir, JFROG_CLI_DIRECTORY) :
                Paths.get(jfrogTmpDir, JFROG_CLI_DIRECTORY, version);
        return Files.createDirectories(jfExecDir).toAbsolutePath();
    }

    /**
     * Retrieves the JFrog CLI executable path.
     *
     * @param serverConfig The provided Server Config.
     * @param buildLog     The build log.
     * @return The absolute path of the JFrog CLI executable.
     * @throws IOException If an I/O error occurs while downloading or creating the executable.
     */
    public static String getJfExecutable(final ServerConfig serverConfig, String jfrogTmpDir, BuildLog buildLog) throws IOException {
        buildLog.info("Getting JFrog CLI executable...");
        boolean downloadFromReleases = StringUtils.isBlank(serverConfig.getCliRepository());

        // An empty string indicates the latest version.
        String version = StringUtils.defaultIfBlank(serverConfig.getCliVersion(), downloadFromReleases ? RELEASE : DEFAULT_CLI_VERSION);

        Path executableLocation = getJfrogExecutableDirectory(jfrogTmpDir, version);
        String binaryName = getJfrogCliBinaryName();
        String executableFullPath = Paths.get(executableLocation.toString(), binaryName).toString();

        // Decide whether to download CLI from releases.jfrog.io or from Artifactory
        String downloadSourceUrl = downloadFromReleases ? RELEASES_URL : serverConfig.getUrl();

        // Downloading executable from Artifactory
        try (ArtifactoryManager manager = new ArtifactoryManager(
                downloadSourceUrl + "/artifactory",
                downloadFromReleases ? "" : serverConfig.getUsername(),
                downloadFromReleases ? "" : serverConfig.getPassword(),
                downloadFromReleases ? "" : serverConfig.getAccessToken(),
                buildLog)) {
            String cliUrlSuffix = String.format("/jfrog-cli/v2-jf/%s/jfrog-cli-%s/%s", version, OsUtils.getOsDetails(), binaryName);
            if (!downloadFromReleases) {
                // Should download from configured server. (example: myRepo/artifactory/jfrog-cli/v2-jf/2.37.0/jfrog-cli-mac-386/jf)
                cliUrlSuffix = String.format("/%s/artifactory%s", serverConfig.getCliRepository(), cliUrlSuffix);
            }
            // Getting updated CLI binary's sha256 from Artifactory.
            String artifactorySha256 = manager.downloadHeader(cliUrlSuffix, DownloadResponse.SHA256_HEADER_NAME);
            // Check whether it's needed to download a new executable or it already exists on the agent.
            if (shouldDownloadTool(executableLocation, artifactorySha256)) {
                if (version.equals(RELEASE)) {
                    buildLog.info(String.format("Download '%s' latest version from: %s%n", binaryName, manager.getUrl() + cliUrlSuffix));
                } else {
                    buildLog.info(String.format("Download '%s' version %s from: %s%n", binaryName, version, manager.getUrl() + cliUrlSuffix));
                }
                File downloadResponse = manager.downloadToFile(cliUrlSuffix, executableFullPath);
                if (!downloadResponse.setExecutable(true)) {
                    throw new IOException("No permission to add execution permission to binary");
                }
                buildLog.info("Successfully downloaded JFrog CLI executable: " + downloadResponse.getPath());
                createSha256File(executableLocation, artifactorySha256);
            } else {
                buildLog.info("Found existing JFrog CLI executable");
            }
            return executableFullPath;
        } catch (IOException e) {
            throw new IOException("Failed while running download CLI command with error: " + e);
        }
    }

    /**
     * Determines whether the tool needs to be downloaded based on the tool's directory and its sha256.
     *
     * @param toolLocation      The expected location of the tool on the file system.
     * @param artifactorySha256 The sha256 of the expected file in Artifactory.
     * @return True if the tool should be downloaded, false otherwise.
     * @throws IOException If an I/O error occurs while reading the sha256 file.
     */
    private static boolean shouldDownloadTool(Path toolLocation, String artifactorySha256) throws IOException {
        // In case no sha256 was provided (for example when the customer blocks headers), download the tool.
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

    /**
     * Creates a sha256 file containing the specified sha256 value.
     *
     * @param toolLocation      The tool location.
     * @param artifactorySha256 The sha256 value.
     * @throws IOException If an I/O error occurs while writing the file.
     */
    private static void createSha256File(Path toolLocation, String artifactorySha256) throws IOException {
        File file = new File(toolLocation.toFile(), SHA256_FILE_NAME);
        Files.write(file.toPath(), artifactorySha256.getBytes(StandardCharsets.UTF_8));
    }
}
