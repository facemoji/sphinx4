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

import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.frontend.FloatData;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;


/** Defines utility methods for manipulating data values. */
public class DataUtil {

    private static final int HEXADECIMAL = 1;
    private static final int SCIENTIFIC = 2;
    private static final int DECIMAL = 3;


    /** The number format to be used by *ArrayToString() methods. The default is scientific. */
    private static int dumpFormat = SCIENTIFIC;

    /**
     * Static initialization of dumpFormat
     */
    static {
        String formatProperty = System.getProperty("frontend.util.dumpformat",
                "SCIENTIFIC");
        if (formatProperty.compareToIgnoreCase("DECIMAL") == 0) {
            dumpFormat = DECIMAL;
        } else if (formatProperty.compareToIgnoreCase("HEXADECIMAL") == 0) {
            dumpFormat = HEXADECIMAL;
        } else if (formatProperty.compareToIgnoreCase("SCIENTIFIC") == 0) {
            dumpFormat = SCIENTIFIC;
        }
    }


    /** Uninstantiable class. */
    private DataUtil() {
    }


    /**
     * Converts a big-endian byte array into an array of doubles. Each consecutive bytes in the byte array are converted
     * into a double, and becomes the next element in the double array. The size of the returned array is
     * (length/bytesPerValue). Currently, only 1 byte (8-bit) or 2 bytes (16-bit) samples are supported.
     *
     * @param byteArray     a byte array
     * @param offset        which byte to start from
     * @param length        how many bytes to convert
     * @param bytesPerValue the number of bytes per value
     * @param signedData    whether the data is signed
     * @return a double array, or <code>null</code> if byteArray is of zero length
     * @throws java.lang.ArrayIndexOutOfBoundsException if index goes out of bounds
     *
     */
    public static double[] bytesToValues(byte[] byteArray,
                                               int offset,
                                               int length,
                                               int bytesPerValue,
                                               boolean signedData)
            throws ArrayIndexOutOfBoundsException {

        if (0 < length && (offset + length) <= byteArray.length) {
            assert (length % bytesPerValue == 0);
            double[] doubleArray = new double[length / bytesPerValue];

            int i = offset;

            for (int j = 0; j < doubleArray.length; j++) {
                int val = byteArray[i++];
                if (!signedData) {
                    val &= 0xff; // remove the sign extension
                }
                for (int c = 1; c < bytesPerValue; c++) {
                    int temp = byteArray[i++] & 0xff;
                    val = (val << 8) + temp;
                }

                doubleArray[j] = val;
            }

            return doubleArray;
        } else {
            throw new ArrayIndexOutOfBoundsException
                    ("offset: " + offset + ", length: " + length
                            + ", array length: " + byteArray.length);
        }
    }


    /**
     * Converts a little-endian byte array into an array of doubles. Each consecutive bytes of a float are converted
     * into a double, and becomes the next element in the double array. The number of bytes in the double is specified
     * as an argument. The size of the returned array is (data.length/bytesPerValue).
     *
     * @param data          a byte array
     * @param offset        which byte to start from
     * @param length        how many bytes to convert
     * @param bytesPerValue the number of bytes per value
     * @param signedData    whether the data is signed
     * @return a double array, or <code>null</code> if byteArray is of zero length
     * @throws java.lang.ArrayIndexOutOfBoundsException if index goes out of bounds
     *
     */
    public static double[] littleEndianBytesToValues(byte[] data,
                                                           int offset,
                                                           int length,
                                                           int bytesPerValue,
                                                           boolean signedData)
            throws ArrayIndexOutOfBoundsException {

        if (0 < length && (offset + length) <= data.length) {
            assert (length % bytesPerValue == 0);
            double[] doubleArray = new double[length / bytesPerValue];

            int i = offset + bytesPerValue - 1;

            for (int j = 0; j < doubleArray.length; j++) {
                int val = data[i--];
                if (!signedData) {
                    val &= 0xff; // remove the sign extension
                }
                for (int c = 1; c < bytesPerValue; c++) {
                    int temp = data[i--] & 0xff;
                    val = (val << 8) + temp;
                }

                // advance 'i' to the last byte of the next value
                i += (bytesPerValue * 2);

                doubleArray[j] = val;
            }

            return doubleArray;

        } else {
            throw new ArrayIndexOutOfBoundsException
                    ("offset: " + offset + ", length: " + length
                            + ", array length: " + data.length);
        }
    }


    /**
     * Returns the number of samples per window given the sample rate (in Hertz) and window size (in milliseconds).
     *
     * @param sampleRate     the sample rate in Hertz (i.e., frequency per seconds)
     * @param windowSizeInMs the window size in milliseconds
     * @return the number of samples per window
     */
    public static int getSamplesPerWindow(int sampleRate,
                                          float windowSizeInMs) {
        return (int) (sampleRate * windowSizeInMs / 1000);
    }


    /**
     * Returns the number of samples in a window shift given the sample rate (in Hertz) and the window shift (in
     * milliseconds).
     *
     * @param sampleRate      the sample rate in Hertz (i.e., frequency per seconds)
     * @param windowShiftInMs the window shift in milliseconds
     * @return the number of samples in a window shift
     */
    public static int getSamplesPerShift(int sampleRate,
                                         float windowShiftInMs) {
        return (int) (sampleRate * windowShiftInMs / 1000);
    }


    /**
     * Returns a native audio format that has the same encoding, endianness and sample size as the given format, and a
     * sample rate that is greater than or equal to the given sample rate.
     *
     * @param format the desired format
     * @param mixer  if non-null, use this Mixer; otherwise use AudioSystem
     * @return a suitable native audio format
     */
    public static AudioFormat getNativeAudioFormat(AudioFormat format,
                                                   Mixer mixer) {
        Line.Info[] lineInfos;

        if (mixer != null) {
            lineInfos = mixer.getTargetLineInfo
                    (new Line.Info(TargetDataLine.class));
        } else {
            lineInfos = AudioSystem.getTargetLineInfo
                    (new Line.Info(TargetDataLine.class));
        }

        AudioFormat nativeFormat = null;

        // find a usable target line
        for (Line.Info info : lineInfos) {

            AudioFormat[] formats = ((TargetDataLine.Info)info).getFormats();

            for (AudioFormat thisFormat : formats) {

                // for now, just accept downsampling, not checking frame
                // size/rate (encoding assumed to be PCM)

                if (thisFormat.getEncoding() == format.getEncoding()
                    && thisFormat.isBigEndian() == format.isBigEndian()
                    && thisFormat.getSampleSizeInBits() ==
                    format.getSampleSizeInBits()
                    && thisFormat.getSampleRate() >= format.getSampleRate()) {
                    nativeFormat = thisFormat;
                    break;
                }
            }
            if (nativeFormat != null) {
                //no need to look through remaining lineinfos
                break;
            }
        }
        return nativeFormat;
    }


    /**
     * Converts DoubleData object to FloatDatas.
     * @param data data to convert
     * @return converted data
     */
    public static DoubleData FloatData2DoubleData(FloatData data) {
        int numSamples = data.getValues().length;

        double[] doubleData = new double[numSamples];
        float[] values = data.getValues();
        for (int i = 0; i < values.length; i++) {
            doubleData[i] = values[i];
        }

        return new DoubleData(doubleData, data.getSampleRate(), data.getFirstSampleNumber());
    }


    /**
     * Converts FloatData object to DoubleData.
     * @param data data to convert
     * @return converted data
     */
    public static FloatData DoubleData2FloatData(DoubleData data) {
        int numSamples = data.getValues().length;

        float[] floatData = new float[numSamples];
        double[] values = data.getValues();
        for (int i = 0; i < values.length; i++) {
            floatData[i] = (float) values[i];
        }

        return new FloatData(floatData, data.getSampleRate(), data.getFirstSampleNumber());
    }
}
