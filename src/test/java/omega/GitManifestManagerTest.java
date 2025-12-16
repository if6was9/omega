package omega;

import org.junit.jupiter.api.Test;

public class GitManifestManagerTest {

	
	@Test
	public void testIt() {
		
		
		GitManifestManager mm = new GitManifestManager();
		mm.gitUrl("git@github.com:if6was9/omega.git");
		
		mm.update();
		
		
	}
}
