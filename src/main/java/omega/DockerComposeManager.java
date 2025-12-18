package omega;

import bx.util.BxException;
import bx.util.Json;
import bx.util.S;
import bx.util.Slogger;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.transport.DockerHttpClient;
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;
import com.google.common.base.Joiner;
import com.google.common.base.Suppliers;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.dataformat.yaml.YAMLMapper;

public class DockerComposeManager {

  DockerClient client;

  GitRepoManager manifestManager;

  static Logger logger = Slogger.forEnclosingClass();

  public static final String DOCKER = findDockerExe();

  public DockerClient getClient() {

    if (client == null) {

      DockerClientConfig dockerConfig =
          DefaultDockerClientConfig.createDefaultConfigBuilder().build();

      DockerHttpClient httpClient =
          new ZerodepDockerHttpClient.Builder()
              .dockerHost(dockerConfig.getDockerHost())
              .sslConfig(dockerConfig.getSSLConfig())
              .maxConnections(100)
              .connectionTimeout(Duration.ofSeconds(30))
              .responseTimeout(Duration.ofSeconds(45))
              .build();

      DockerClient dockerClient = DockerClientImpl.getInstance(dockerConfig, httpClient);

      this.client = dockerClient;
    }

    return this.client;
  }

  private static String findDockerExe() {
    List<String> DOCKER_EXE_CANDIDATES = List.of("/usr/local/bin/docker", "/usr/bin/docker");
    return DOCKER_EXE_CANDIDATES.stream()
        .filter(it -> new File(it).exists())
        .findFirst()
        .orElse("docker");
  }

  public void getOmegaContainers() {
    var client = getClient();

    client
        .listContainersCmd()
        .withShowAll(false)
        .exec()
        .forEach(
            c -> {
              String name =
                  List.of(c.getNames()).stream()
                      .map(n -> n.startsWith("/") ? n.substring(1) : n)
                      .findFirst()
                      .orElse(null);
            });
  }

  Supplier<String> engineIdSupplier = Suppliers.memoize(this::fetchEngineId);

  private String fetchEngineId() {
    return getClient().infoCmd().exec().getId();
  }

  public String getEngineId() {
    return engineIdSupplier.get();
  }

  public DockerComposeManager manifestManager(GitRepoManager manager) {
    this.manifestManager = manager;
    return this;
  }

  YAMLMapper yamlMapper = new YAMLMapper();

  public Stream<JsonNode> findComposeFiles() {

    Set<String> composeFileNames =
        Set.of("compose.yml", "compose.yaml", "docker-compose.yaml", "docker-compose.yml");

    logger.atInfo().log("finding compose files: {}", manifestManager.getBaseDir());
    try {
      return Files.walk(manifestManager.getBaseDir().toPath())
          .filter(p -> composeFileNames.contains(p.toFile().getName()))
          .map(
              p -> {
                try {

                  JsonNode n = yamlMapper.readTree(p.toFile());

                  if (n.path("x-omega").isObject()) {

                    logger.atInfo().log("x-omega: {}", p);
                    String filePath = p.toFile().toString();
                    if (filePath.startsWith(manifestManager.getBaseDir().getPath())) {
                      filePath =
                          filePath.substring(manifestManager.getBaseDir().getPath().length());
                      if (filePath.startsWith("/")) {
                        filePath = filePath.substring(1);
                      }
                    }

                    try {
                      ObjectNode omega = (ObjectNode) n.path("x-omega");
                      omega.put("composePath", filePath);
                      omega.put(
                          "gitRef", S.notBlank(manifestManager.getCurrentRevision()).orElse(""));
                      omega.put(
                          "baseDir",
                          manifestManager.getBaseDir().toPath().toFile().getCanonicalPath());

                      File composeAbsolutePath =
                          new File(omega.path("baseDir").asString(), filePath);
                      omega.put("composeAbsolutePath", composeAbsolutePath.getAbsolutePath());

                      return n;
                    } catch (IOException e) {
                      throw new BxException(e);
                    }
                  } else {
                    logger.atDebug().log("no x-omega: {}", p);
                  }

                  return null;
                } catch (RuntimeException e) {
                  return null;
                }
              })
          .filter(
              it -> {
                if (it == null) {
                  return false;
                }
                if (it.has("x-omega")) {
                  return true;
                }

                return false;
              })
          .map(
              n -> {
                String digest = manifestManager.computeDigest(n);

                Json.asObjectNode(n.path("x-omega"))
                    .ifPresent(
                        it -> {
                          it.put("digest", digest);
                        });
                return n;
              });

    } catch (IOException e) {
      throw new BxException(e);
    }
  }

  public void dockerVersion() {
    try {
      new ProcessExecutor()
          .command(List.of(DOCKER, "version"))
          .redirectOutput(Slf4jStream.ofCaller().asInfo())
          .redirectErrorStream(true)
          .execute();
    } catch (IOException | InterruptedException | TimeoutException e) {
      throw new BxException(e);
    }
  }

  public void ensureUp(JsonNode n) {

    String composeAbsolutePath = n.path("x-omega").path("composeAbsolutePath").asString();

    List<String> command = List.of(DOCKER, "compose", "-f", composeAbsolutePath, "up", "-d");
    logger.atInfo().log("EXEC: {}", Joiner.on(" ").join(command));
    try {
      new ProcessExecutor()
          .command(command)
          .directory(new File(composeAbsolutePath).getParentFile())
          .redirectOutput(Slf4jStream.ofCaller().asInfo())
          .redirectErrorStream(true)
          .execute();
    } catch (IOException | InterruptedException | TimeoutException e) {
      throw new BxException(e);
    }
  }

  public void ensureDown(JsonNode n) {

    String composeAbsolutePath = n.path("x-omega").path("composeAbsolutePath").asString();

    List<String> command = List.of(DOCKER, "compose", "-f", composeAbsolutePath, "down");
    logger.atInfo().log("EXEC: {}", Joiner.on(" ").join(command));

    try {
      new ProcessExecutor()
          .command(command)
          .directory(new File(composeAbsolutePath).getParentFile())
          .redirectOutput(Slf4jStream.ofCaller().asInfo())
          .redirectErrorStream(true)
          .execute();

    } catch (IOException | InterruptedException | TimeoutException e) {
      throw new BxException(e);
    }
  }

  public boolean isEnabled(JsonNode n) {
    if (n == null) {
      return false;
    }
    return n.path("enabled").asBoolean(true);
  }

  private Optional<JsonNode> ours(JsonNode compose) {

    String id = getEngineId();
    for (JsonNode run : compose.path("x-omega").path("run")) {
      if (run.path("id").asString("INVALID").equals(id)) {
        return Optional.of(run);
      }
    }

    return Optional.empty();
  }

  boolean shouldBeUp(JsonNode compose) {

    Optional<JsonNode> ours = ours(compose);
    if (ours.isEmpty()) {
      // if there is no matching run, the compose is not for us
      return false;
    }

    return isEnabled(ours.get());
  }

  boolean shouldBeDown(JsonNode compose) {

    Optional<JsonNode> ours = ours(compose);
    if (ours.isEmpty()) {
      // if there is no matching run, the compose is not for us
      return false;
    }

    return isEnabled(ours.get()) == false;
  }

  public void process() {

    manifestManager.update();

    List<JsonNode> allComposeFiles = findComposeFiles().toList();

    List<JsonNode> ourDown = allComposeFiles.stream().filter(this::shouldBeDown).toList();

    List<JsonNode> ourUp = allComposeFiles.stream().filter(this::shouldBeUp).toList();

    ourDown.forEach(
        n -> {
          ensureDown(n);
        });

    ourUp.forEach(
        n -> {
          ensureUp(n);
        });

    cleanupOrphans(Stream.concat(ourUp.stream(), ourDown.stream()).toList());
  }

  void terminateContainer(Container c) {
    logger.atInfo().log("terminating orphanned container: {}", List.of(c.getNames()));
    getClient().killContainerCmd(c.getId()).exec();
    getClient().removeContainerCmd(c.getId()).exec();
  }

  void cleanupOrphans(List<JsonNode> ours) {
    logger.atInfo().log("cleaning up orphans...");

    // Compile a list of all the compose files that are relevant to our node
    List<String> ourComposeFiles =
        ours.stream()
            .map(n -> n.path("x-omega").path("composeAbsolutePath").asString(null))
            .toList();

    // Look at the running containers
    getClient()
        .listContainersCmd()
        .exec()
        .forEach(
            container -> {
              Map<String, String> labels = container.getLabels();

              String runningComposeFile = labels.get("com.docker.compose.project.config_files");

              if (isCurrentProcess(container)) {
                logger.atInfo().log(
                    "ignoring {}{} because it is this process",
                    container.getId(),
                    List.of(container.getNames()));
              } else {
                // We only care about omega-managed compose files
                if (runningComposeFile != null && runningComposeFile.contains("/omega/")) {
                  if (!ourComposeFiles.contains(runningComposeFile)) {

                    terminateContainer(container);
                  }
                }
              }
            });
    logger.atInfo().log("orphan cleanup complete");
  }

  public boolean isCurrentProcess(Container container) {

    if (container.getId().startsWith(S.notBlank(System.getenv("HOSTNAME")).orElse("NOT FOUND"))) {
      return true;
    }
    return false;
  }
}
