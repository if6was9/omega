package omega;

import bx.util.Json;
import java.io.File;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ObjectNode;

public class RepoManagerTest {

  @Test
  public void testIt() {

    RepoManager mm = new RepoManager();

    Assertions.assertThat(mm.getBaseDir()).isNull();

    mm.baseDir(new File("."));

    Assertions.assertThat(mm.getBaseDir().getAbsolutePath())
        .isEqualTo(new File(".").getAbsolutePath());
  }

  @Test
  public void testCompute() {

    RepoManager mm = new RepoManager();

    ObjectNode doc = Json.createObjectNode();
    Assertions.assertThat(mm.computeDigest(doc))
        .isEqualTo("44136fa355b3678a1146ad16f7e8649e94fb4fc21fe77e8310c060f61caaff8a");

    doc.set("services", Json.createObjectNode());
    Assertions.assertThat(mm.computeDigest(doc))
        .isEqualTo("d8bb576872842519ea3e5e49d7e21510a8ce619e70bcc49ec4c59d2202898366");

    var omega = Json.createObjectNode();
    doc.set("x-omega", omega);

    Assertions.assertThat(mm.computeDigest(doc))
        .isEqualTo("d8bb576872842519ea3e5e49d7e21510a8ce619e70bcc49ec4c59d2202898366");
  }
}
