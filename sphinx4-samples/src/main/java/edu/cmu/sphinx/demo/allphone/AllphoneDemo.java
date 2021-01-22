/*
 * Copyright 2014 Carnegie Mellon University.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 */

package edu.cmu.sphinx.demo.allphone;

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.Context;
import edu.cmu.sphinx.api.SpeechResult;
import edu.cmu.sphinx.decoder.ResultListener;
import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.result.WordResult;
import edu.cmu.sphinx.util.TimeFrame;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * A simple example that shows how to transcribe a continuous audio file that
 * has multiple utterances in it.
 */
public class AllphoneDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("Loading models...");

        Configuration configuration = new Configuration();

        // Load model from the jar
        configuration
            .setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");

        // You can also load model from folder
        // configuration.setAcousticModelPath("file:en-us");

        configuration.setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");
        Context context = new Context(configuration);
        context.setLocalProperty("decoder->searchManager", "allphoneSearchManager");
        Recognizer recognizer = context.getInstance(Recognizer.class);
        InputStream stream =
            // AllphoneDemo.class.getResourceAsStream("/edu/cmu/sphinx/demo/aligner/10001-90210-01803.wav");
            Files.newInputStream(Paths.get("c:/repo/facemoji/audio2bs/sphinx4-5prealpha-src/sphinx4-samples/src/main/resources/edu/cmu/sphinx/demo/speakerid/test.wav"));
        stream.skip(44);
// todo Decoder.featureBlockSize - set it to 1 or use FrameDecoder somehow
        // Simple recognition with generic model
        recognizer.allocate();
        context.setSpeechSource(stream, TimeFrame.INFINITE);
        Result result;
        recognizer.addResultListener(new ResultListener() {
            @Override
            public void newResult(Result result) {
                boolean aFinal = result.isFinal();
                String bestResultNoFiller = result.getBestResultNoFiller();
                Token bestToken = result.getBestToken();
                if (bestToken.getWord() != null) {
                    System.out.print(bestToken.getWord().getSpelling());
                    System.out.printf(" %g2 ", bestToken.getAcousticScore());
                } else {
                    System.out.printf(" nw:%d ", bestToken.getCollectTime());
                }
            }

            @Override
            public void newProperties(PropertySheet ps) throws PropertyException {

            }
        });
        while ((result = recognizer.recognize()) != null) {
            SpeechResult speechResult = new SpeechResult(result);
            System.out.format("%nHypothesis: %s%n", speechResult.getHypothesis());

            System.out.println("List of recognized words and their times:");
            for (WordResult r : speechResult.getWords()) {
                System.out.println(r);
            }

            // System.out.println("Lattice contains " + speechResult.getLattice().getNodes().size() + " nodes");
        }
        recognizer.deallocate();

    }
}
