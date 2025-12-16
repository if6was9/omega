package omega;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.slf4j.Logger;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import com.google.common.base.Suppliers;
import com.google.common.collect.Maps;

import bx.util.BxException;
import bx.util.Json;
import bx.util.S;
import bx.util.Slogger;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.dataformat.yaml.YAMLMapper;

public class DockerContainerManager {

	
	DockerClient client;
	
	GitManifestManager manifestManager;
	
	static Logger logger = Slogger.forEnclosingClass();
	
	public DockerClient getClient() {
		  
		if (client==null) {
			
		
		  DockerClientConfig dockerConfig = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
				  
		  DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
				    .dockerHost(dockerConfig.getDockerHost())
				    .sslConfig(dockerConfig.getSSLConfig())
				    .maxConnections(100)
				    .connectionTimeout(Duration.ofSeconds(30))
				    .responseTimeout(Duration.ofSeconds(45))
				    .build();
		
		  
		  
		  DockerClient dockerClient = DockerClientImpl.getInstance(dockerConfig,httpClient);
		  
		  this.client = dockerClient;
		}
		
		return this.client;
	}
	
	

	public void getOmegaContainers() {
		var client = getClient();
		
		
		
		client.listContainersCmd().withShowAll(false).exec().forEach(c->{
			String name = List.of(c.getNames()).stream().map(n->n.startsWith("/")? n.substring(1) : n).findFirst().orElse(null);
			
			System.out.println(c.getLabels());
		});
		
	}
	
	Supplier<String> engineIdSupplier = Suppliers.memoize(this::fetchEngineId);
	
	private String fetchEngineId() {
		return getClient().infoCmd().exec().getId();
	}
	
	public String getEngineId() {
		return engineIdSupplier.get();
	}
	
	public DockerContainerManager manifestManager(GitManifestManager manager) {
		this.manifestManager = manager;
		return this;
	}
	
	
	
	
	YAMLMapper yamlMapper = new YAMLMapper();
	public Stream<JsonNode> findComposeFiles() {

		Set<String> composeFileNames = Set.of("compose.yml", "compose.yaml", "docker-compose.yaml",
				"docker-compose.yml");

		logger.atInfo().log("finding compose files: {}",manifestManager.baseDir);
		try {
		return Files.walk(manifestManager.baseDir.toPath())
				
				.filter(p -> composeFileNames.contains(p.toFile().getName())).map(p -> {
			try {
				
				JsonNode n = yamlMapper.readTree(p.toFile());

				if (n.path("x-omega").isObject()) {
				
					String filePath = p.toFile().toString();
					if (filePath.startsWith(manifestManager.baseDir.getPath())) {
						filePath = filePath.substring(manifestManager.getBaseDir().getPath().length());
						if (filePath.startsWith("/")) {
							filePath = filePath.substring(1);
						}
					}
					
					try {
					ObjectNode omega = (ObjectNode) n.path("x-omega");
					omega.put("composePath", filePath);
					omega.put("gitRef", S.notBlank(manifestManager.getRef()).orElse(""));
					omega.put("baseDir", manifestManager.baseDir.toPath().toFile().getCanonicalPath());
					return n;
					}
					catch (IOException e) {
						throw new BxException(e);
					}
				}
				else {
					logger.atInfo().log("no x-omega: {}",n);
				}
				
				return null;
			} catch (RuntimeException e) {
				return null;
			}

		})
		.filter(it->{
			
			if (it==null) {
				return false;
			}
			if (it.has("x-omega")) {
				return true;
			}
			
	
			return false;
		}).map(n->{
			
			String digest = manifestManager.computeDigest(n);
			
		
			Json.asObjectNode(n.path("x-omega")).ifPresent(it->{
				it.put("digest", digest);
				
			});
			return n;
		});
		
	
		}
		catch (IOException e) {
			throw new BxException(e);
		}
	
	}
	
	public void process() {
		manifestManager.update();
		
		findComposeFiles().forEach(it->{
			System.out.println(it.toPrettyString());
		});
	
		
	
	}
}
