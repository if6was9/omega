package omega;

import bx.util.Config;
import bx.util.RuntimeEnvironment;
import bx.util.Sleep;
import bx.util.Slogger;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.sshd.JGitKeyCache;
import org.eclipse.jgit.transport.sshd.SshdSessionFactoryBuilder;
import org.eclipse.jgit.util.FS;
import org.slf4j.Logger;
import tools.jackson.databind.ObjectMapper;

public class App {

  static Logger logger = Slogger.forEnclosingClass();

  public static ObjectMapper mapper = new ObjectMapper();

  static Config cfg = Config.get();

  public static void configureSsh() {

    var builder =
        new SshdSessionFactoryBuilder()
            .setPreferredAuthentications("publickey,password")
            .setHomeDirectory(FS.DETECTED.userHome())
            .setSshDirectory(new File(FS.DETECTED.userHome(), ".ssh"));

    boolean checkHostKeys = true;

    try {
      checkHostKeys = Boolean.parseBoolean(cfg.get("VERIFY_HOST_KEY").orElse("true"));
    } catch (RuntimeException e) {
      logger.atWarn().setCause(e).log("could not parse VERIFY_HOST_KEY");
    }

    if (checkHostKeys == false) {

      logger.atWarn().log("host key verification is disabled");
      builder.setServerKeyDatabase(
          (homeDir, dotSshDir) -> {
            return new InsecureServerKeyDatabase();
          });
    }

    var sshdSessionFactory = builder.build(new JGitKeyCache());

    SshSessionFactory.setInstance(sshdSessionFactory);
  }

  public static void main(String[] args) throws IOException {

    logger.atInfo().log("container: {}", RuntimeEnvironment.get().isRunningInContainer());

    configureSsh();

    String gitUrl = cfg.get("GIT_URL").orElse(null);

    Path p = new File("./repo").toPath();

    GitRepoManager gmm = new GitRepoManager().gitUrl(gitUrl);

    gmm.baseDir(p.toFile());

    cfg.get("GIT_REF")
        .ifPresent(
            ref -> {
              gmm.targetRef(ref);
            });

    DockerComposeManager dcm = new DockerComposeManager();

    dcm.dockerVersion();

    dcm.manifestManager(gmm);

    int pollSecs = Integer.parseInt(cfg.get("POLL_SECS").orElse("60"));

    do {
      dcm.process();
      if (pollSecs > 0) {
        logger.atInfo().log("sleeping for {} secs (env: POLL_SECS)...", pollSecs);
        Sleep.sleepSecs(pollSecs);
      }
    } while (pollSecs > 0);
  }
}
