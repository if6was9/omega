package omega;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.eclipse.jgit.api.Git;
import org.slf4j.Logger;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Info;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Maps;
import com.google.common.io.Files;

import bx.util.Config;
import bx.util.Slogger;
import tools.jackson.databind.ObjectMapper;

public class App {

  static Logger logger = Slogger.forEnclosingClass();

  public static ObjectMapper mapper = new ObjectMapper();

  public static void main(String[] args) throws IOException {
	  
	  
	
	  /*
	  String gitUrl = cfg.get("GIT_URL").orElse("https://github.com/if6was9/bx.git");
	  
	
	  Path p = java.nio.file.Files.createTempDirectory("repo");
	  
	  logger.atInfo().log("REPO={}",p);
	  try {
	  Git git = Git.cloneRepository()
			  .setURI(gitUrl)
			  
			  .setDirectory(p.toFile())
			  .call();
	  
	  }
	  catch (Exception e) {
		  e.printStackTrace();
	  }
	  
	  */
	  
	  var dcm = new DockerContainerManager();
	  dcm.getOmegaContainers();
	  
	//  ManifestManager sm = new ManifestManager();
	  //sm.findAll();
	  
  }

}
