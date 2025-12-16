package omega;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

import bx.util.Json;
import bx.util.Slogger;
import omega.DockerContainerManager;
import omega.GitManifestManager;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.ObjectNode;

public class GitTest {

	
	org.slf4j.Logger logger = Slogger.forEnclosingClass();
	

	
	@Test
	public void tedstIt() {
		  String remoteUri = "git@github.com:if6was9/infra.git"; 
		  
		  GitManifestManager gmm = new GitManifestManager().gitUrl(remoteUri);
		
		  gmm.baseDir(new File("./repo"));
		  
		  DockerContainerManager dcm = new DockerContainerManager();
		  
		  
		  
		  dcm.manifestManager(gmm);
		  
		  dcm.process();
		  
		 
		  
		
		  
		  
		  
	}
	
	
		
		
		
	
}
