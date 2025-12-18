package omega;

import bx.util.BxException;
import bx.util.Config;
import bx.util.S;
import bx.util.Slogger;
import com.google.common.base.Preconditions;
import com.google.common.io.Closer;
import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.GitCommand;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;

public class GitRepoManager extends RepoManager {

  static Logger logger = Slogger.forEnclosingClass();

  String gitUrl;

  String targetRef = null;

  boolean forceRecovery = false;

  public GitRepoManager gitUrl(String url) {
    this.gitUrl = url;
    return this;
  }

  private void defer(Closer closer, Git git) {
    if (git != null) {
      closer.register(toCloseable(git));
    }
  }

  public void clean() {
    checkout(targetRef);
  }

  public void checkout(String refName) {

    logger.atInfo().log("checkout({})", refName);
    try (Closer closer = Closer.create()) {
      Git git = Git.open(getBaseDir());
      defer(closer, git);
      logger.atInfo().log("clean checkout of {}", refName);
      org.eclipse.jgit.lib.Ref ref = git.checkout().setForced(true).setName(refName).call();

      git.reset().setMode(ResetType.HARD).setRef(refName).call();

    } catch (GitAPIException | IOException e) {
      throw new BxException(e);
    }
  }

  public void fetch() {

    try (Closer closer = Closer.create()) {
      Git git = Git.open(getBaseDir());
      defer(closer, git);
      logger.atInfo().log("git fetch");

      FetchCommand fc = git.fetch().setRefSpecs("+refs/heads/*:refs/remotes/origin/*");
      FetchResult fr = fc.call();

    } catch (GitAPIException | IOException e) {
      throw new BxException(e);
    }
  }

  public void forceReclone() {
    forceRecovery = true;
    this.deleteWorkspace();
    clone();
    forceRecovery = false;
  }

  public GitRepoManager targetRef(String ref) {
    this.targetRef = ref;
    return this;
  }

  boolean isMissingOrEmpty(File f) {
    if (!f.exists()) {
      return true;
    }
    if (f.isDirectory() && f.listFiles() == null || f.listFiles().length == 0) {
      return true;
    }
    return false;
  }

  public void update() {

    logger.atInfo().log("*** GitRepoManager.update() *** ");

    createTempWorkingDirIfNecessary();

    if (isMissingOrEmpty(getBaseDir())) {
      clone();
    } else {
      try {
        fetch();

        // If we started with a cloned repo but didn't have targetRef set,
        // we need to find it from the checked-out repo.  If we don't set targetRef here,
        // the clean() operation will fail.
        if (S.isBlank(targetRef)) {
          targetRef = getBranchOrRef();
        }

        clean();
      } catch (RuntimeException e) {
        logger.atWarn().setCause(e).log("problem with fetch/clean");
        forceReclone();
      }
    }
    logger.atInfo().log("current branch={} rev={}", getBranchOrRef(), getCurrentRevision());
  }

  Closeable toCloseable(Git git) {
    return new Closeable() {

      @Override
      public void close() {
        git.close();
      }
    };
  }

  private void createTempWorkingDirIfNecessary() {
    try {
      if (getBaseDir() == null) {

        File tmpDir = Files.createTempDirectory("repo").toFile();

        baseDir(new File(tmpDir, "repo"));
      }
    } catch (IOException e) {
      throw new BxException(e);
    }
  }

  Optional<CredentialsProvider> getCredentialsProvider() {

    Config cfg = Config.get();

    if (cfg.get("GIT_PASSWORD").isPresent()) {

      UsernamePasswordCredentialsProvider cp =
          new UsernamePasswordCredentialsProvider(
              cfg.get("GIT_USERNAME").orElse(""), cfg.get("GIT_PASSWORD").get().toCharArray());
      return Optional.of(cp);
    }
    return Optional.empty();
  }

  public <C extends GitCommand, T extends Object> TransportCommand<C, T> applyTransportConfig(
      TransportCommand<C, T> command) {

    getCredentialsProvider()
        .ifPresent(
            cp -> {
              command.setCredentialsProvider(cp);
            });

    return command;
  }

  public String getCurrentRevision() {

    try (Closer closer = Closer.create()) {
      Git git = Git.open(getBaseDir());
      defer(closer, git);

      ObjectId id = git.getRepository().resolve(Constants.HEAD);

      return id.getName();
    } catch (IOException e) {
      throw new BxException(e);
    }
  }

  public void withGit(Consumer<Git> func) {
    if (getBaseDir() == null) {
      return;
    }
    if (!getBaseDir().exists()) {
      return;
    }
    try (Closer closer = Closer.create()) {
      Git g = Git.open(getBaseDir());
      defer(closer, g);
      func.accept(g);
    } catch (IOException e) {
      throw new BxException(e);
    }
  }

  public File clone() {
    try (Closer closer = Closer.create()) {
      // Path p = java.nio.file.Files.createTempDirectory("repo");

      Preconditions.checkState(S.isNotBlank(gitUrl), "GIT_URL not set");

      createTempWorkingDirIfNecessary();
      Path p = getBaseDir().toPath();
      logger.atInfo().log("cloning {} into {}", gitUrl, p.toFile());

      CloneCommand cc = Git.cloneRepository();
      applyTransportConfig(cc);

      Git git = cc.setURI(gitUrl).setDirectory(getBaseDir()).call();
      defer(closer, git);
      baseDir(p.toFile());

      var refs = git.getRepository().getRefDatabase().getRefsByPrefix("HEAD");
      var headRef = refs.get(0).getObjectId().name();

      if (targetRef != null) {
        checkout(targetRef);
        var branchOrRef = git.getRepository().getFullBranch();

      } else {
        var branchName = git.getRepository().getFullBranch();
        logger.atInfo().log("cloned to branch={} ref={}", branchName, headRef);
        targetRef = branchName;
      }

      return p.toFile();
    } catch (IOException | GitAPIException e) {
      throw new BxException(e);
    }
  }

  boolean isSame(File f1, File f2) {

    try {
      return f1.getCanonicalPath().equals(f2.getCanonicalPath());
    } catch (IOException e) {
      throw new BxException(e);
    }
  }

  public String getBranchOrRef() {
    try (Closer closer = Closer.create()) {
      Git git = Git.open(getBaseDir());
      defer(closer, git);
      return git.getRepository().getFullBranch();
    } catch (IOException e) {
      throw new BxException(e);
    }
  }

  public void deleteWorkspace() {
    File dir = this.getBaseDir();
    if (dir == null || !dir.exists()) {
      return;
    } else if (dir.isFile()) {
      dir.delete();
      return;
    } else if (dir.isDirectory()) {
      // ok
    } else {
      throw new BxException("unknown fs type: " + dir);
    }

    if (isSame(new File("/"), dir) || isSame(dir, new File(System.getProperty("user.home")))) {

      throw new BxException("cannot delete: " + getBaseDir());
    }
    logger.atInfo().log("deleting " + dir.getAbsolutePath());
    try {
      MoreFiles.deleteRecursively(dir.toPath(), RecursiveDeleteOption.ALLOW_INSECURE);
    } catch (IOException e) {
      throw new BxException(e);
    }
  }
}
