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

import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.result.WordResult;
import java.util.List;


/**
 * High-level wrapper for {@link Result} instance.
 */
public final class SpeechResult {

    private final Result result;

    /**
     * Constructs recognition result based on {@link Result} object.
     *
     * @param result recognition result returned by {@link Recognizer}
     */
    public SpeechResult(Result result) {
        this.result = result;
    }

    /**
     * Returns {@link List} of words of the recognition result.
     * Within the list words are ordered by time frame.
     *
     * @return words that form the result
     */
    public List<WordResult> getWords() {
        return result.getTimedBestResult(false);
    }

    /**
     * @return string representation of the result.
     */
    public String getHypothesis() {
	return result.getBestResultNoFiller();
    }

    /**
     * Return Result object of current SpeechResult
     *
     * @return Result object stored in this.result
     */
    public Result getResult() {
    	return result;
    }
}
