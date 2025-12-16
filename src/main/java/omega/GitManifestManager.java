package omega;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;

import com.google.common.io.Closer;

import bx.util.BxException;
import bx.util.Slogger;

public class GitManifestManager extends ManifestManager {

	Logger logger = Slogger.forEnclosingClass();

	String gitUrl;

	String headRef;
	
	public GitManifestManager gitUrl(String url) {
		this.gitUrl = url;
		return this;
	}

	public void update() {
		if (baseDir == null || (!baseDir.exists())) {
			clone();
			return;
		}
		pull();
	}

	Closeable toCloseable(Git git) {
		return new Closeable() {
			
			@Override
			public void close() {
				git.close();
				
			}
		};
	}
	public void pull() {
		logger.atInfo().log("git pull");
		try (Closer closer= Closer.create()){
			
			
			Git git = Git.open(baseDir);
			closer.register(toCloseable(git));
		
			var repository = git.getRepository();
			
			git.pull().call();
			var refs  = repository.getRefDatabase().getRefsByPrefix("HEAD");
			this.headRef = refs.get(0).getObjectId().name();
			
			logger.atInfo().log("ref={}",headRef);
		
		
		}
		catch (IOException | GitAPIException e) {
			throw new BxException(e);
		}
	}
	
	
	public String getRef() {
		return headRef;
	}
	

	
	public File clone() {
		try {
		//	Path p = java.nio.file.Files.createTempDirectory("repo");

			
			Path p = new File("./repo").toPath();
			
			logger.atInfo().log("cloning {} into {}",gitUrl,p.toFile());
			

			Git git = Git.cloneRepository().setURI(gitUrl)

					.setDirectory(baseDir).call();
			this.baseDir = p.toFile();
			var refs  = git.getRepository().getRefDatabase().getRefsByPrefix("HEAD");
			this.headRef = refs.get(0).getObjectId().name();
			return p.toFile();
		} catch (IOException | GitAPIException e) {
			throw new BxException(e);
		}
	}
}
