import edu.cmu.sphinx.api.SpeechResult;
import edu.cmu.sphinx.decoder.Decoder;
import edu.cmu.sphinx.decoder.ResultListener;
import edu.cmu.sphinx.decoder.pruner.SimplePruner;
import edu.cmu.sphinx.decoder.scorer.SimpleAcousticScorer;
import edu.cmu.sphinx.decoder.search.PartitionActiveListFactory;
import edu.cmu.sphinx.decoder.search.SimpleBreadthFirstSearchManager;
import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.frontend.AutoCepstrum;
import edu.cmu.sphinx.frontend.DataBlocker;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.endpoint.SpeechClassifier;
import edu.cmu.sphinx.frontend.endpoint.SpeechMarker;
import edu.cmu.sphinx.frontend.feature.DeltasFeatureExtractor;
import edu.cmu.sphinx.frontend.feature.LiveCMN;
import edu.cmu.sphinx.frontend.filter.Preemphasizer;
import edu.cmu.sphinx.frontend.transform.DiscreteFourierTransform;
import edu.cmu.sphinx.frontend.util.StreamDataSource;
import edu.cmu.sphinx.frontend.window.RaisedCosineWindower;
import edu.cmu.sphinx.linguist.acoustic.AcousticModel;
import edu.cmu.sphinx.linguist.acoustic.UnitManager;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Loader;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Sphinx3Loader;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.TiedStateAcousticModel;
import edu.cmu.sphinx.linguist.allphone.AllphoneLinguist;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.result.WordResult;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Run this test with coverage to see what is actually used for phoneme recognition with usable configuration.
 * Target state is: No automagic, no reflection, no XML config, no (exposed) mutable state.
 * TODO get rid of util.props
 * TODO get rid of Recognizer and edu.cmu.sphinx.decoder.Decoder altogether. Decoder uses SearchManager directly.
 */
public class AllphoneCoverageTest {

  @Test
  public void minimalisticPhonemeDetectionWithoutAutomagic() throws IOException {
    StreamDataSource dataSource = new StreamDataSource(16000, 3200, 16, false, true);
    dataSource.setInputStream(getInputStream("c:/repo/facemoji/audio2bs/sphinx4-5prealpha-src/sphinx4-samples/src/main/resources/edu/cmu/sphinx/demo/speakerid/test.wav"));
    Recognizer recognizer = initRecognizer(dataSource);
    recognizer.allocate();
    recognizer.addResultListener(createResultListener());
    runRecognition(recognizer);
    recognizer.deallocate();
  }

  private void runRecognition(Recognizer recognizer) {
    Result result;
    while ((result = recognizer.recognize()) != null) {
      SpeechResult speechResult = new SpeechResult(result);
      System.out.format("%nHypothesis: %s%n", speechResult.getHypothesis());
      System.out.println("List of recognized words and their times:");
      for (WordResult r : speechResult.getWords()) {
        System.out.println(r);
      }
    }
  }

  private InputStream getInputStream(String path) throws IOException {
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

      @Override
      public void newProperties(PropertySheet ps) throws PropertyException {
      }
    };
  }

  private Recognizer initRecognizer(DataProcessor dataSource) {
    UnitManager unitManager = new UnitManager();
    Sphinx3Loader loader = new Sphinx3Loader("resource:/edu/cmu/sphinx/models/en-us/en-us", unitManager, 0f, 1e-7f, 0.0001f, 4, true);
    AcousticModel acousticModel = new TiedStateAcousticModel(loader, unitManager, true);
    FrontEnd frontEnd = getFrontEnd(dataSource, loader);
    SimpleBreadthFirstSearchManager searchManager = new SimpleBreadthFirstSearchManager(
        new AllphoneLinguist(acousticModel, 0.05f, false),
        new SimplePruner(),
        new SimpleAcousticScorer(frontEnd),
        new PartitionActiveListFactory(20000, 1e-60),
        1e-40,
        0,
        false);

    Decoder decoder = new Decoder(searchManager, true, false, new ArrayList<>(), 1);
    return new Recognizer(decoder, new ArrayList<>());
  }

  // todo
  private FrontEnd getFrontEnd(DataProcessor dataSource, Loader loader) {
    AutoCepstrum autoCepstrum = null;
    try {
      autoCepstrum = new AutoCepstrum(loader);
    } catch (IOException e) {
      e.printStackTrace();
      Assert.fail(e.getMessage(), e);
    }

    List<DataProcessor> frontEndList = Arrays.asList(
        dataSource,
        new DataBlocker(10),
        new SpeechClassifier(10, 0.003, 13, 0),
        new SpeechMarker(200, 200, 50),
        new Preemphasizer(0.97),
        new RaisedCosineWindower(0.46, 25.625f, 10),
        new DiscreteFourierTransform(-1, false),
        autoCepstrum,
        new LiveCMN(300, 400, 200),
        new DeltasFeatureExtractor(3)
        // new FeatureTransform(loader),
    );
    return new FrontEnd(frontEndList);
  }
}
