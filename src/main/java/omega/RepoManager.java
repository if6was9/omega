package omega;

import bx.util.Json;
import bx.util.Slogger;
import java.io.File;
import org.slf4j.Logger;
import tools.jackson.databind.JsonNode;
import tools.jackson.dataformat.yaml.YAMLMapper;

public class RepoManager {

  static Logger logger = Slogger.forEnclosingClass();
  private File baseDir = null;

  YAMLMapper yamlMapper = new YAMLMapper();

  RepoManager() {
    super();
  }

  RepoManager(File baseDir) {
    super();
    this.baseDir = baseDir;
  }

  RepoManager baseDir(File baseDir) {
    this.baseDir = baseDir;
    return this;
  }

  public File getBaseDir() {
    return this.baseDir;
  }

  public String computeDigest(JsonNode manifest) {

    JsonNode copy = manifest.deepCopy();

    // remove all injected properties
    Json.asObjectNode(copy)
        .ifPresent(
            c -> {
              c.propertyNames().stream()
                  .filter(p -> p.startsWith("x-omega"))
                  .toList()
                  .forEach(
                      p -> {
                        c.remove(p);
                      });
            });

    return Json.hash(copy);
  }
}
