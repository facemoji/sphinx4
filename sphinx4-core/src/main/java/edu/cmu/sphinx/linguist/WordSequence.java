/*
 * Copyright 1999-2002 Carnegie Mellon University.
 * Portions Copyright 2002 Sun Microsystems, Inc.
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 */

package edu.cmu.sphinx.linguist;

import static java.lang.Math.min;

import edu.cmu.sphinx.linguist.dictionary.Word;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class can be used to keep track of a word sequence. This class is an
 * immutable class. It can never be modified once it is created (except,
 * perhaps for transient, cached things such as a precalculated hashcode).
 */

public final class WordSequence implements Comparable<WordSequence> {

    private final Word[] words;
    private transient int hashCode = -1;

    /**
     * Constructs a word sequence from the list of words
     *
     * @param list the list of words
     */
    public WordSequence(List<Word> list) {
        this.words = list.toArray(new Word[0]);
        check();
    }

    private void check() {
        for (Word word : words)
            if (word == null)
                throw new Error("WordSequence should not have null Words.");
    }

    /**
     * Returns the n-th word in this sequence
     *
     * @param n which word to return
     * @return the n-th word in this sequence
     */
    public Word getWord(int n) {
        assert n < words.length;
        return words[n];
    }

    /**
     * Returns the number of words in this sequence
     *
     * @return the number of words
     */
    public int size() {
        return words.length;
    }

    /**
     * Returns a string representation of this word sequence. The format is:
     * [ID_0][ID_1][ID_2].
     *
     * @return the string
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Word word : words)
            sb.append('[').append(word).append(']');
        return sb.toString();
    }

    /**
     * Calculates the hashcode for this object
     *
     * @return a hashcode for this object
     */
    @Override
    public int hashCode() {
        if (hashCode == -1) {
            int code = 123;
            for (int i = 0; i < words.length; i++) {
                code += words[i].hashCode() * (2 * i + 1);
            }
            hashCode = code;
        }
        return hashCode;
    }

    /**
     * compares the given object to see if it is identical to this WordSequence
     *
     * @param object the object to compare this to
     * @return true if the given object is equal to this object
     */
    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (!(object instanceof WordSequence))
            return false;

        return Arrays.equals(words, ((WordSequence) object).words);
    }

    /**
     * @param startIndex start index
     * @param stopIndex stop index
     * @return a subsequence with both <code>startIndex</code> and
     *         <code>stopIndex</code> exclusive.
     */
    public WordSequence getSubSequence(int startIndex, int stopIndex) {
        List<Word> subseqWords = new ArrayList<>();

        for (int i = startIndex; i < stopIndex; i++) {
            subseqWords.add(getWord(i));
        }

        return new WordSequence(subseqWords);
    }

    /**
     * @return the words of the <code>WordSequence</code>.
     */
    public Word[] getWords() {
        return getSubSequence(0, size()).words; // create a copy to keep the
                                                // class immutable
    }

    public int compareTo(WordSequence other) {
        int n = min(words.length, other.words.length);
        for (int i = 0; i < n; ++i) {
            if (!words[i].equals(other.words[i])) {
                return words[i].compareTo(other.words[i]);
            }
        }

        return words.length - other.words.length;
    }
}
