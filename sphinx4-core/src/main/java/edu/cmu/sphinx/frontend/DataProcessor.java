/*
 * Copyright 2004 Carnegie Mellon University.
 * Portions Copyright 2004 Sun Microsystems, Inc.
 * Portions Copyright 2004 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 * @see FrontEnd
 */
package edu.cmu.sphinx.frontend;

import java.util.List;

/**
 * A processor that performs a signal processing function.
 *
 * Since a DataProcessor usually belongs to a particular front end pipeline,
 * you can name the pipeline it belongs to in the {@link #initialize()
 * initialize} method. (Note, however, that it is not always the case that a
 * DataProcessor belongs to a particular pipeline. For example,
 * the Microphone class is a DataProcessor,
 * but it usually does not belong to any particular pipeline.
 * <p>
 * Each
 * DataProcessor usually have a predecessor as well. This is the previous
 * DataProcessor in the pipeline. Again, not all DataProcessors have
 * predecessors.
 * <p>
 * Calling {@link #getData() getData}will return the
 * processed Data object.
 */
public interface DataProcessor {

    static List<DataProcessor> chainProcessors(List<DataProcessor> dataProcessors) {
        for (int i = 1; i < dataProcessors.size(); i++) {
            dataProcessors.get(i).setPredecessor(dataProcessors.get(i - 1));
        }
        return dataProcessors;
    }

    /**
     * Initializes this DataProcessor.
     *
     * This is typically called after the DataProcessor has been configured.
     */
    void initialize();


    /**
     * Returns the processed Data output.
     *
     * @return an Data object that has been processed by this DataProcessor
     * @throws DataProcessingException if a data processor error occurs
     */
    Data getData() throws DataProcessingException;


    /**
     * Returns the predecessor DataProcessor.
     *
     * @return the predecessor
     */
    DataProcessor getPredecessor();


    /**
     * Sets the predecessor DataProcessor. This method allows dynamic reconfiguration of the front end.
     *
     * @param predecessor the new predecessor of this DataProcessor
     */
    void setPredecessor(DataProcessor predecessor);
}
