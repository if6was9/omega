package omega;

import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;
import java.io.File;
import java.io.IOException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class GitRepoManagerTest {

  @Test
  public void testRecover() throws IOException {

    GitRepoManager mm = new GitRepoManager();
    mm.gitUrl("https://github.com/if6was9/omega.git");
    mm.baseDir(new File("./temp"));
    mm.deleteWorkspace();
    mm.update();

    Assertions.assertThat(mm.getBranchOrRef()).isEqualTo("refs/heads/main");
    mm.update();

    Assertions.assertThat(mm.getCloneCount()).isEqualTo(1);

    MoreFiles.deleteRecursively(
        new File(mm.getBaseDir(), ".git/objects").toPath(), RecursiveDeleteOption.ALLOW_INSECURE);

    mm.update();

    Assertions.assertThat(mm.getCloneCount()).isEqualTo(2);

    var pom = new File("./temp/pom.xml");
    Assertions.assertThat(pom).exists();
    pom.delete();
    Assertions.assertThat(pom).doesNotExist();
    mm.update();
    Assertions.assertThat(pom).exists();

    Assertions.assertThat(mm.getCloneCount()).isEqualTo(2);
  }

  @Test
  public void testTargetRef() throws IOException {

    GitRepoManager mm = new GitRepoManager();
    mm.baseDir(new File("./temp"));
    mm.gitUrl("https://github.com/if6was9/omega.git");
    mm.deleteWorkspace();
    mm.targetRef("6cb5d03");
    mm.update();
    Assertions.assertThat(mm.getCurrentRevision())
        .isEqualTo("6cb5d03d0fe5e593367fe1b2a70fcd659aef2d19");
    Assertions.assertThat(mm.getBranchOrRef())
        .isEqualTo("6cb5d03d0fe5e593367fe1b2a70fcd659aef2d19");
    Assertions.assertThat(new File("./temp/pom.xml")).doesNotExist();

    mm.targetRef("main");
    mm.update();

    Assertions.assertThat(mm.getBranchOrRef()).isEqualTo("refs/heads/main");
    Assertions.assertThat(mm.getCurrentRevision())
        .isNotEqualTo("6cb5d03d0fe5e593367fe1b2a70fcd659aef2d19");
    Assertions.assertThat(new File("./temp/pom.xml")).exists();
  }

  @Test
  public void testExplicitMain() {

    GitRepoManager mm = new GitRepoManager();
    mm.baseDir(new File("./temp"));
    mm.gitUrl("https://github.com/if6was9/omega.git");
    mm.deleteWorkspace();
    mm.targetRef("main");
    mm.update();

    Assertions.assertThat(mm.getBranchOrRef()).isEqualTo("refs/heads/main");

    mm.update();
  }

  @Test
  public void testStartWithDetachedHead() {
    GitRepoManager mm = new GitRepoManager();
    mm.baseDir(new File("./temp"));
    mm.gitUrl("https://github.com/if6was9/omega.git");
    mm.deleteWorkspace();

    String detachedHeadRef = "5a4a609";
    mm.targetRef(detachedHeadRef);
    mm.update();

    Assertions.assertThat(mm.getCurrentRevision()).startsWith(detachedHeadRef);

    // This will execute a pull, which will fail in a detached head state.
    // We want to see that the revision does not change
    mm.update();
    Assertions.assertThat(mm.getCurrentRevision()).startsWith(detachedHeadRef);

    mm = new GitRepoManager();
    mm.baseDir(new File("./temp"));
    mm.gitUrl("https://github.com/if6was9/omega.git");
    mm.targetRef("main");
    mm.update();

    Assertions.assertThat(mm.getCurrentRevision()).doesNotStartWith(detachedHeadRef);
    mm.update();

    Assertions.assertThat(mm.getBranchOrRef()).isEqualTo("refs/heads/main");
  }
}
