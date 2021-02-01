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

package edu.cmu.sphinx.util;

import java.io.PrintWriter;


/** Provides a set of generic utilities */
public class Utilities {

    private final static boolean TRACKING_OBJECTS = false;


    // Unconstructable.
    private Utilities() {
    }


    /**
     * Returns a string with the given number of spaces.
     *
     * @param padding the number of spaces in the string
     * @return a string of length 'padding' containing only the SPACE char.
     */
    public static String pad(int padding) {
        if (padding > 0) {
            StringBuilder sb = new StringBuilder(padding);
            for (int i = 0; i < padding; i++) {
                sb.append(' ');
            }
            return sb.toString();
        } else {
            return "";
        }
    }


    /**
     * Pads with spaces or truncates the given string to guarantee that it is exactly the desired length.
     *
     * @param string    the string to be padded
     * @param minLength the desired length of the string
     * @return a string of length containing string padded with whitespace or truncated
     */
    public static String pad(String string, int minLength) {
        String result = string;
        int pad = minLength - string.length();
        if (pad > 0) {
            result = string + pad(minLength - string.length());
        } else if (pad < 0) {
            result = string.substring(0, minLength);
        }
        return result;
    }


    /**
     * Pads with spaces or truncates the given int to guarantee that it is exactly the desired length.
     *
     * @param val       the value to be padded
     * @param minLength the desired length of the string
     * @return a string of length containing string padded with whitespace or truncated
     */
    public static String pad(int val, int minLength) {
        return pad(String.valueOf(val), minLength);
    }


    /**
     * Pads with spaces or truncates the given double to guarantee that it is exactly the desired length.
     *
     * @param val       the value to be padded
     * @param minLength the desired length of the string
     * @return a string of length containing string padded with whitespace or truncated
     */
    public static String pad(double val, int minLength) {
        return pad(String.valueOf(val), minLength);
    }

    /**
     * Dumps padded text. This is a simple tool for helping dump text with padding to a Writer.
     *
     * @param pw      the stream to send the output
     * @param padding the number of spaces in the string
     * @param string  the string to output
     */
    public static void dump(PrintWriter pw, int padding, String string) {
        pw.print(pad(padding));
        pw.println(string);
    }


    /**
     * utility method for tracking object counts
     *
     * @param name  the name of the object
     * @param count the count of objects
     */
    public static void objectTracker(String name, int count) {
        if (TRACKING_OBJECTS) {
            if (count % 1000 == 0) {
                System.out.println("OT: " + name + ' ' + count);
            }
        }
    }


    /**
     * Byte-swaps the given integer to the other endian. That is, if this integer is big-endian, it becomes
     * little-endian, and vice-versa.
     *
     * @param integer the integer to swap
     * @return swapped integer
     */
    public static int swapInteger(int integer) {
        return (((0x000000ff & integer) << 24) |
                ((0x0000ff00 & integer) << 8) |
                ((0x00ff0000 & integer) >> 8) |
                ((0xff000000 & integer) >> 24));
    }


    /**
     * Byte-swaps the given float to the other endian. That is, if this float is big-endian, it becomes little-endian,
     * and vice-versa.
     *
     * @param floatValue the float to swap
     * @return swapped float
     */
    public static float swapFloat(float floatValue) {
        return Float.intBitsToFloat
                (swapInteger(Float.floatToRawIntBits(floatValue)));
    }


    /**
     * If a data point is below 'floor' make it equal to floor.
     *
     * @param data  the data to floor
     * @param floor the floored value
     */
    public static void floorData(float[] data, float floor) {
        for (int i = 0; i < data.length; i++) {
            if (data[i] < floor) {
                data[i] = floor;
            }
        }
    }
    /**
     * If a data point is non-zero and below 'floor' make it equal to floor
     * (don't floor zero values though).
     *
     * @param data the data to floor
     * @param floor the floored value
     */
    public static void nonZeroFloor(float[] data, float floor) {
        for (int i = 0; i < data.length; i++) {
            if (data[i] != 0.0 && data[i] < floor) {
                data[i] = floor;
            }
        }
    }


    /**
     * Normalize the given data.
     *
     * @param data the data to normalize
     */
    public static void normalize(float[] data) {
        float sum = 0;
        for (float val : data) {
            sum += val;
        }
        if (sum != 0.0f) {
            for (int i = 0; i < data.length; i++) {
                data[i] = data[i] / sum;
            }
        }
    }


    /**
     * Combines two paths without too much logic only cares about avoiding
     * double backslashes since they hurt some resource searches.
     *
     * @param path1 First path to join
     * @param path2 Second path to join
     * @return combined path
     */
    public static String pathJoin(String path1, String path2) {
        if (path1.length() > 0 && path1.charAt(path1.length() - 1) == '/') {
            path1 = path1.substring(0, path1.length() - 1);
        }
        if (path2.length() > 0 && path2.charAt(0) == '/') {
            path2 = path2.substring(1);
        }
        return path1 + "/" + path2;
    }

}


