/*
 * Copyright 2013 Carnegie Mellon University.
 * Portions Copyright 2004 Sun Microsystems, Inc.
 * Portions Copyright 2004 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 */

package edu.cmu.sphinx.api;

import edu.cmu.sphinx.decoder.Decoder;
import edu.cmu.sphinx.decoder.pruner.SimplePruner;
import edu.cmu.sphinx.decoder.scorer.SimpleAcousticScorer;
import edu.cmu.sphinx.decoder.search.PartitionActiveListFactory;
import edu.cmu.sphinx.decoder.search.SimpleBreadthFirstSearchManager;
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
import edu.cmu.sphinx.frontend.window.RaisedCosineWindower;
import edu.cmu.sphinx.linguist.acoustic.AcousticModel;
import edu.cmu.sphinx.linguist.acoustic.UnitManager;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Loader;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Sphinx3Loader;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.TiedStateAcousticModel;
import edu.cmu.sphinx.linguist.allphone.AllphoneLinguist;
import edu.cmu.sphinx.recognizer.Recognizer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Helps to tweak configuration without touching XML-file directly.
 */
public class PhonemeRecognizerFactory {

    public static Recognizer createPhonemeRecognizer(DataProcessor dataSource) {
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

    // TODO Improve this mess
    private static FrontEnd getFrontEnd(DataProcessor dataSource, Loader loader) {
        AutoCepstrum autoCepstrum = null;
        try {
            autoCepstrum = new AutoCepstrum(loader);
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize Cepstrum Data processor", e);
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
