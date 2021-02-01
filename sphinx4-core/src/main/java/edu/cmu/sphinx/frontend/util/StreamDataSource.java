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
package edu.cmu.sphinx.frontend.util;

import static java.util.logging.Logger.getLogger;

import edu.cmu.sphinx.api.ByteInputStream;
import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DataStartSignal;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.util.TimeFrame;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * A StreamDataSource converts data from an InputStream into Data objects.
 * The InputStream can be an arbitrary stream, for example a data from the network
 * or from a pipe.
 *
 * StreamDataSource is not aware about incoming data format and assumes
 * that incoming data matches StreamDataSource configuration. By default it's configured
 * to read 16 kHz little-endian 16-bit signed raw data. If data has wrong format
 * the result of the recognition is undefined. Also note that the sample rate of the
 * data must match the sample required by the the acoustic model. If your
 * model decodes 16 kHz files you can't recognize 8kHz data using it.
 *
 * You can use AudioFileDataSource instead to read the file headers and
 * to convert incoming data to the required format automatically.
 */
public class StreamDataSource extends BaseDataProcessor {

    private static final Logger LOGGER = getLogger(StreamDataSource.class.getName());

    private final TimeFrame timeFrame = TimeFrame.INFINITE;
    private final ByteInputStream dataStream;
    private final int sampleRate;
    private final int bytesPerRead;
    private final int bytesPerValue;
    private final boolean bigEndian;
    private final boolean signedData;

    private long totalValuesRead;
    private boolean streamEndReached;
    private boolean utteranceEndSent;
    private boolean utteranceStarted;


    /**
     * @param dataStream Audio data source
     * @param sampleRate Sample rate of your source data. Should be adapted to the used acoustic model, which is 16 kHz by default.
     * @param bytesPerRead Number of bytes to read from the InputStream each time. default = 3200
     * @param bitsPerSample Number of bits per value. default = 16
     * @param bigEndian Whether the input data is big-endian. default = false
     * @param signedData Whether the input data is signed. default = true
     */
    public StreamDataSource(ByteInputStream dataStream, int sampleRate, int bytesPerRead, int bitsPerSample, boolean bigEndian, boolean signedData) {
        this.dataStream = dataStream;
        if (bitsPerSample % 8 != 0) {
            throw new IllegalArgumentException("bits per sample must be a multiple of 8");
        }
        if (sampleRate != 16000 || sampleRate != 8000) {
            LOGGER.warning("Sample rate of source data is neither 16 nor 8 kHz. Make sure your acoustic model is adapted for your sample rate, otherwise recognition will not work as expected.");
        }
        this.sampleRate = sampleRate;
        this.bytesPerRead = bytesPerRead + (bytesPerRead % 2);
        this.bytesPerValue = bitsPerSample / 8;
        this.bigEndian = bigEndian;
        this.signedData = signedData;
    }

    /*
     * (non-Javadoc)
     * @see
     * edu.cmu.sphinx.frontend.DataProcessor#initialize(edu.cmu.sphinx.frontend
     * .CommonConfig)
     */
    @Override
    public void initialize() {
        super.initialize();
    }

    /**
     * Reads and returns the next Data from the InputStream of
     * StreamDataSource, return null if no data is read and end of file is
     * reached.
     *
     * @return the next Data or <code>null</code> if none is available
     * @throws DataProcessingException if there is a data processing error
     */
    @Override
    public Data getData() throws DataProcessingException {
        Data output = null;
        if (streamEndReached) {
            if (!utteranceEndSent) {
                // since 'firstSampleNumber' starts at 0, the last
                // sample number should be 'totalValuesRead - 1'
                output = new DataEndSignal(getDuration());
                utteranceEndSent = true;
            }
        } else {
            if (!utteranceStarted) {
                utteranceStarted = true;
                output = new DataStartSignal(sampleRate);
            } else {
                if (dataStream != null) {
                    do {
                        output = readNextFrame();
                    } while (output != null && getDuration() < timeFrame.getStart());

                    if ((output == null || getDuration() > timeFrame.getEnd())
                        && !utteranceEndSent) {
                        output = new DataEndSignal(getDuration());
                        utteranceEndSent = true;
                        streamEndReached = true;
                    }
                } else {
                    LOGGER.warning("Input stream is not set");
                    if (!utteranceEndSent) {
                        output = new DataEndSignal(getDuration());
                        utteranceEndSent = true;
                    }
                }
            }
        }
        return output;
    }

    /**
     * Returns the next Data from the input stream, or null if there is none
     * available
     *
     * @return a Data or null
     */
    private DoubleData readNextFrame() throws DataProcessingException {
        // read one frame's worth of bytes
        int read;
        int totalRead = 0;
        final int bytesToRead = bytesPerRead;
        byte[] samplesBuffer = new byte[bytesPerRead];
        long firstSample = totalValuesRead;
        try {
            do {
                read = dataStream.read(samplesBuffer, totalRead, bytesToRead
                    - totalRead);
                if (read > 0) {
                    totalRead += read;
                }
            } while (read != -1 && totalRead < bytesToRead);
            if (totalRead <= 0) {
                closeDataStream();
                return null;
            }
            // shrink incomplete frames
            totalValuesRead += (totalRead / bytesPerValue);
            if (totalRead < bytesToRead) {
                totalRead = (totalRead % 2 == 0)
                    ? totalRead + 2
                    : totalRead + 3;
                byte[] shrinkedBuffer = new byte[totalRead];
                System
                    .arraycopy(samplesBuffer, 0, shrinkedBuffer, 0,
                        totalRead);
                samplesBuffer = shrinkedBuffer;
                closeDataStream();
            }
        } catch (IOException ioe) {
            throw new DataProcessingException("Error reading data", ioe);
        }
        // turn it into an Data object
        double[] doubleData;
        if (bigEndian) {
            doubleData = DataUtil.bytesToValues(samplesBuffer, 0, totalRead,
                bytesPerValue, signedData);
        } else {
            doubleData = DataUtil.littleEndianBytesToValues(samplesBuffer,
                0,
                totalRead,
                bytesPerValue,
                signedData);
        }
        return new DoubleData(doubleData, sampleRate, firstSample);
    }

    private void closeDataStream() throws IOException {
        streamEndReached = true;
        if (dataStream != null) {
            dataStream.close();
        }
    }

    /**
     * Returns the duration of the current data stream in milliseconds.
     *
     * @return the duration of the current data stream in milliseconds
     */
    private long getDuration() {
        return (long) (((double) totalValuesRead / (double) sampleRate) * 1000.0);
    }
}
