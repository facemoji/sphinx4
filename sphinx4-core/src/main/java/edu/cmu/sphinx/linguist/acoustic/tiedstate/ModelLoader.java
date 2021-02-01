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

import java.io.IOException;

/**
 * Generic interface for a loader of acoustic models
 */
public interface ModelLoader {

    /**
     * Loads the acoustic model.
     *
     * @return Loaded acoustic model
     * @throws IOException if an error occurs while loading the model
     */
    Model load(ModelData modelData) throws IOException;
}
