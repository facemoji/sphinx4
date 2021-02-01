package edu.cmu.sphinx.util;

/** Some simple matrix and vector manipulation methods. */
public class MatrixUtils {

    public static float[] double2float(double[] values) { // what a mess !!! -> fixme: how to convert number arrays ?
        float[] newVals = new float[values.length];
        for (int i = 0; i < newVals.length; i++) {
            newVals[i] = (float) values[i];
        }

        return newVals;
    }

}
