package omega;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;

import com.fasterxml.jackson.databind.node.MissingNode;

import bx.util.BxException;
import bx.util.Json;
import bx.util.S;
import bx.util.Slogger;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.dataformat.yaml.YAMLMapper;

public class ManifestManager {

	static Logger logger = Slogger.forEnclosingClass();
	File baseDir;

	YAMLMapper yamlMapper = new YAMLMapper();

	ManifestManager() {

	}

	
	
	public static ManifestManager create(File rootDir) {

		ManifestManager mm = new ManifestManager().baseDir(rootDir);
	
		return mm;
	}

	
	public File getBaseDir() {
		return this.baseDir;
	}
	public ManifestManager baseDir(File baseDir) {
		this.baseDir = baseDir;
		return this;
	}
	

	public String getGitRepo() {
		return "";
	}
	public String getRef() {
		return "";
	}



	public String computeDigest(JsonNode manifest) {

		JsonNode copy = manifest.deepCopy();

		// remove all injected properties
		Json.asObjectNode(copy).ifPresent(c -> {
			c.propertyNames().stream().filter(p -> p.startsWith("x-omega")).toList().forEach(p -> {
				c.remove(p);
			});

		});

		return Json.hash(copy);
	}
}
