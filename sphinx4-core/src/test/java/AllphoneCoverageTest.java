import edu.cmu.sphinx.api.ByteInputStream;
import edu.cmu.sphinx.api.InputStreams;
import edu.cmu.sphinx.api.PhonemeRecognizerFactory;
import edu.cmu.sphinx.decoder.ResultListener;
import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.frontend.util.StreamDataSource;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.result.WordResult;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.testng.annotations.Test;

/**
 * Run this test with coverage to see what is actually used for phoneme recognition with usable configuration.
 * Target state is: No automagic, no reflection, no XML config, no (exposed) mutable state.
 * TODO get rid of Recognizer and edu.cmu.sphinx.decoder.Decoder altogether. Decoder uses SearchManager directly.
 */
public class AllphoneCoverageTest {

  @Test
  public void minimalisticPhonemeDetectionWithoutAutomagic() throws IOException {
    try (ByteInputStream dataStream = InputStreams.fromInputStream(getAudioInputStream())) {
      StreamDataSource dataSource = new StreamDataSource(dataStream, 16000, 3200, 16, false, true);
      Recognizer recognizer = PhonemeRecognizerFactory.createPhonemeRecognizer(dataSource, "resource:/edu/cmu/sphinx/models/en-us/en-us");
      recognizer.allocate();
      recognizer.addResultListener(createResultListener());
      runRecognition(recognizer);
      recognizer.deallocate();
    }
  }

  private void runRecognition(Recognizer recognizer) {
    Result result;
    while ((result = recognizer.recognize()) != null) {
      System.out.format("%nHypothesis: %s%n", result.getBestResultNoFiller());
      System.out.println("List of recognized words and their times:");
      for (WordResult r : result.getTimedBestResult(false)) {
        System.out.println(r);
      }
    }
  }

  private InputStream getAudioInputStream() throws IOException {
    // InputStream stream = Files.newInputStream(Paths.get(path));
    InputStream stream = Files.newInputStream(Paths.get("C:/repo/facemoji/sphinx4-phoneme/sphinx4-samples/src/main/resources/edu/cmu/sphinx/demo/aligner/10001-90210-01803.wav"));
    // InputStream stream = getClass().getResourceAsStream("/edu/cmu/sphinx/demo/aligner/10001-90210-01803.wav");
    stream.skip(44);
    return stream;
  }

  private ResultListener createResultListener() {
    return new ResultListener() {
      @Override
      public void newResult(Result result) {
        Token bestToken = result.getBestToken();
        if (bestToken.getWord() != null) {
          System.out.print(bestToken.getWord().getSpelling());
          System.out.print(' ');
        } else {
          System.out.printf(" nw:%d ", bestToken.getCollectTime());
        }
      }
    };
  }

}
