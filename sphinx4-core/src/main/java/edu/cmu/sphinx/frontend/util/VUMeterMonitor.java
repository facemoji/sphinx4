package edu.cmu.sphinx.frontend.util;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DoubleData;

/**
 * A VU meter to be plugged into a front-end. Preferably this component should be plugged directly behind the
 * <code>DataBlocker</code> in order to ensure that only equally sized blocks of meaningful length are used for RMS
 * computation.
 * <p>
 * Because vu-monitoring makes sense only for online speech processing the vu-meter will be visible only if data source
 * which precedes it is a <code>Microphone</code>.
 *
 * @author Holger Brandl
 */

public class VUMeterMonitor extends BaseDataProcessor {

    final VUMeter vumeter;


    public VUMeterMonitor() {
        vumeter = new VUMeter();
    }


    @Override
    public Data getData() throws DataProcessingException {
        Data d = getPredecessor().getData();

        if (d instanceof DoubleData)
            vumeter.calculateVULevels(d);

        return d;
    }


}
