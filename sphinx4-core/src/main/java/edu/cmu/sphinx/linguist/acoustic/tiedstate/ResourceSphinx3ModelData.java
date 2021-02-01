package edu.cmu.sphinx.linguist.acoustic.tiedstate;

import static edu.cmu.sphinx.linguist.acoustic.tiedstate.ModelFile.FEATURE_TRANSFORM;
import static edu.cmu.sphinx.linguist.acoustic.tiedstate.ModelFile.FEAT_PARAMS;
import static edu.cmu.sphinx.linguist.acoustic.tiedstate.ModelFile.MDEF;
import static edu.cmu.sphinx.linguist.acoustic.tiedstate.ModelFile.MEANS;
import static edu.cmu.sphinx.linguist.acoustic.tiedstate.ModelFile.MIXTURE_WEIGHTS;
import static edu.cmu.sphinx.linguist.acoustic.tiedstate.ModelFile.TRANSITION_MATRICES;
import static edu.cmu.sphinx.linguist.acoustic.tiedstate.ModelFile.VARIANCES;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResourceSphinx3ModelData implements ModelData {

  private final Map<ModelFile, byte[]> buffers;

  final static Pattern jarPattern = Pattern.compile("resource:(.*)", Pattern.CASE_INSENSITIVE);

  public static URL resourceToURL(String location) throws MalformedURLException {
    Matcher jarMatcher = jarPattern.matcher(location);
    if (jarMatcher.matches()) {
      String resourceName = jarMatcher.group(1);
      return ResourceSphinx3ModelData.class.getResource(resourceName);
    } else {
      if (location.indexOf(':') == -1) {
        location = "file:" + location;
      }
      return new URL(location);
    }
  }

  public static ResourceSphinx3ModelData readFrom(String resourceRootDir) {
    final EnumMap<ModelFile, byte[]> files = new EnumMap<>(ModelFile.class);
    for (ModelFile modelFile : EnumSet.of(MEANS, VARIANCES, MIXTURE_WEIGHTS, TRANSITION_MATRICES, FEATURE_TRANSFORM, FEAT_PARAMS, MDEF)) {
      try {
        readFile(resourceToURL(resourcePath(resourceRootDir, modelFile.filename))).ifPresent(byteBuffer -> files.put(modelFile, byteBuffer));
      } catch (MalformedURLException e) {
        e.printStackTrace();
      }
    }
    return new ResourceSphinx3ModelData(files);
  }

  private ResourceSphinx3ModelData(Map<ModelFile, byte[]> buffers) {
    this.buffers = buffers;
  }

  private static Optional<byte[]> readFile(URL path) {
    if (path == null) {
      return Optional.empty();
    }
    try {
      byte[] data = toByteArray(path.openStream());
      return Optional.of(data);
    } catch (IOException e) {
      return Optional.empty();
    }
  }

  private static String resourcePath(String resourceRootDir, String file) {
    String slash = resourceRootDir.endsWith("/") || resourceRootDir.endsWith("\\") ? "" : "/";
    return resourceRootDir + slash + file;
  }

  public static byte[] toByteArray(InputStream in) throws IOException {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    byte[] buffer = new byte[1024];
    int len;
    while ((len = in.read(buffer)) != -1) {
      os.write(buffer, 0, len);
    }
    return os.toByteArray();
  }

  @Override
  public byte[] get(ModelFile modelFile) {
    return buffers.get(modelFile);
  }
}
