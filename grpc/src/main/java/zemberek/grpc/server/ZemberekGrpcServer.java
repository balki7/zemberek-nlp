package zemberek.grpc.server;

import io.grpc.Server;
import io.netty.handler.ssl.SslContext;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyServerBuilder;
import nl.altindag.ssl.SSLFactory;
import nl.altindag.ssl.util.NettySslUtils;
import zemberek.core.logging.Log;

import java.nio.file.Path;
import java.nio.file.Paths;

public class ZemberekGrpcServer {

  public static final int DEFAULT_PORT = 9005;

  private final int port;
  private ZemberekContext context;

  public ZemberekGrpcServer(int port, ZemberekGrpcConfiguration configuration) {
    this.port = port;
    context = new ZemberekContext(configuration);
  }

  public void start() throws Exception {
    SSLFactory sslFactory = SSLFactory.builder().withIdentityMaterial("server/keystore.jks", "dragon".toCharArray())
            .withTrustMaterial("server/truststore.jks", "dragon".toCharArray())
            .withNeedClientAuthentication().build();
    SslContext sslContext = GrpcSslContexts.configure(NettySslUtils.forServer(sslFactory)).build();
    Server server = NettyServerBuilder.forPort(port)
        .addService(new LanguageIdServiceImpl())
        .addService(new PreprocessingServiceImpl())
        .addService(new NormalizationServiceImpl(context))
        .addService(new MorphologyServiceImpl(context))
            .sslContext(sslContext)
        .build()
        .start();
    Log.info("Zemberek grpc server started at port: " + port);
    server.awaitTermination();
  }

  public static void main(String[] args) throws Exception {
    Path data = Paths.get("data");
    Path lmPath = Paths.get(data.toString(), "lm", "lm.2gram.slm");
    Path normalizationPath = Paths.get(data.toString(), "normalization");
    new ZemberekGrpcServer(DEFAULT_PORT, new ZemberekGrpcConfiguration(lmPath, normalizationPath)).start();
  }

}
