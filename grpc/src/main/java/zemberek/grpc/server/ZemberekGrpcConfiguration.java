package zemberek.grpc.server;

import java.nio.file.Path;

//TODO: This class will change in next release.
public class ZemberekGrpcConfiguration {

  Path normalizationLmPath;
  Path normalizationDataRoot;

  ZemberekGrpcConfiguration(Path normalizationLmPath, Path normalizationDataRoot) {
    this.normalizationLmPath = normalizationLmPath;
    this.normalizationDataRoot = normalizationDataRoot;
  }

  public boolean normalizationPathsAvailable() {
    return normalizationLmPath!=null && normalizationDataRoot!=null;
  }
}
