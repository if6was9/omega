package omega;

import bx.util.Slogger;
import java.io.File;
import java.net.InetSocketAddress;
import java.security.PublicKey;
import java.util.List;
import java.util.function.BiFunction;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.sshd.ServerKeyDatabase;
import org.eclipse.jgit.transport.sshd.ServerKeyDatabase.Configuration;
import org.eclipse.jgit.transport.sshd.SshdSessionFactoryBuilder;
import org.slf4j.Logger;

public class InsecureServerKeyDatabase implements ServerKeyDatabase {

  static Logger logger = Slogger.forEnclosingClass();

  @Override
  public List<PublicKey> lookup(
      String connectAddress, InetSocketAddress remoteAddress, Configuration config) {

    logger.atInfo().log("lookup({},{},{})", connectAddress, remoteAddress, config);
    return List.of();
  }

  @Override
  public boolean accept(
      String connectAddress,
      InetSocketAddress remoteAddress,
      PublicKey serverKey,
      Configuration config,
      CredentialsProvider provider) {

    logger.atInfo().log(
        "accept({},{},{},{},{})",
        connectAddress,
        remoteAddress,
        config,
        serverKey,
        config,
        provider);
    return true;
  }

  public static SshdSessionFactoryBuilder ignoreKnownHosts(SshdSessionFactoryBuilder builder) {

    builder.setServerKeyDatabase(configFunction());

    return builder;
  }

  public static BiFunction<File, File, ServerKeyDatabase> configFunction() {

    var fn =
        new BiFunction<File, File, ServerKeyDatabase>() {

          @Override
          public ServerKeyDatabase apply(File t, File u) {

            return new InsecureServerKeyDatabase();
          }
        };
    return fn;
  }
}
