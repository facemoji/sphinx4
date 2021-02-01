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

package edu.cmu.sphinx.linguist.acoustic.tiedstate;

import edu.cmu.sphinx.linguist.acoustic.Unit;
import java.util.Map;
import java.util.Properties;

/** Generic interface for a loader of acoustic models */
public interface Model {

    /**
     * Gets the senone pool for this loader.
     *
     * @return the pool
     */
    Pool<Senone> getSenonePool();

    /**
     * Returns the HMM Manager for this loader.
     *
     * @return the HMM Manager
     */
    HMMManager getHMMManager();

    /**
     * Returns the map of context independent units. The map can be accessed by unit name.
     *
     * @return the map of context independent units
     */
    Map<String, Unit> getContextIndependentUnits();

    /**
     * Returns the size of the left context for context dependent units.
     *
     * @return the left context size
     */
    int getLeftContextSize();

    /**
     * Returns the size of the right context for context dependent units.
     *
     * @return the left context size
     */
    int getRightContextSize();

    /**
     * @return the model properties
     */
    Properties getProperties();

}
