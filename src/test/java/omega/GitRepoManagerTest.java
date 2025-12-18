package omega;

import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;
import java.io.File;
import java.io.IOException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class GitRepoManagerTest {

  @Test
  public void testIt() throws IOException {

    GitRepoManager mm = new GitRepoManager();
    mm.baseDir(new File("./temp"));
    mm.gitUrl("https://github.com/if6was9/omega.git");
    mm.deleteWorkspace();

    mm.update();

    Assertions.assertThat(mm.getBranchOrRef()).isEqualTo("refs/heads/main");
    mm.update();

    MoreFiles.deleteRecursively(
        new File(mm.getBaseDir(), ".git/objects").toPath(), RecursiveDeleteOption.ALLOW_INSECURE);

    mm.update();

    var pom = new File("./temp/pom.xml");
    Assertions.assertThat(pom).exists();
    pom.delete();
    Assertions.assertThat(pom).doesNotExist();
    mm.update();
    Assertions.assertThat(pom).exists();
  }

  @Test
  public void testTargetRef() throws IOException {

    GitRepoManager mm = new GitRepoManager();
    mm.baseDir(new File("./temp"));
    mm.gitUrl("https://github.com/if6was9/omega.git");
    mm.deleteWorkspace();
    mm.targetRef("3fd1a74");
    mm.update();
    Assertions.assertThat(mm.getCurrentRevision())
        .isEqualTo("3fd1a742428cae0ad8e18f4cfd5a7720e04c4661");
    Assertions.assertThat(mm.getBranchOrRef())
        .isEqualTo("3fd1a742428cae0ad8e18f4cfd5a7720e04c4661");
    Assertions.assertThat(new File("./temp/pom.xml")).doesNotExist();

    mm.targetRef("main");
    mm.update();

    Assertions.assertThat(mm.getBranchOrRef()).isEqualTo("refs/heads/main");
    Assertions.assertThat(mm.getCurrentRevision())
        .isNotEqualTo("3fd1a742428cae0ad8e18f4cfd5a7720e04c4661");
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
}
