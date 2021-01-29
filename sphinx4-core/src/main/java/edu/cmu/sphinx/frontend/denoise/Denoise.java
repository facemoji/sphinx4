/*
 * Copyright 2013 Carnegie Mellon University.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 */
package edu.cmu.sphinx.frontend.denoise;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DataStartSignal;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.util.props.S4Double;
import edu.cmu.sphinx.util.props.S4Integer;
import java.util.Arrays;

/**
 * The noise filter, same as implemented in sphinxbase/sphinxtrain/pocketsphinx.
 *
 * Noise removal algorithm is inspired by the following papers Computationally
 * Efficient Speech Enchancement by Spectral Minina Tracking by G. Doblinger
 *
 * Power-Normalized Cepstral Coefficients (PNCC) for Robust Speech Recognition
 * by C. Kim.
 *
 * For the recent research and state of art see papers about IMRCA and A
 * Minimum-Mean-Square-Error Noise Reduction Algorithm On Mel-Frequency Cepstra
 * For Robust Speech Recognition by Dong Yu and others
 */
public class Denoise extends BaseDataProcessor {

    double[] power;
    double[] noise;
    double[] floor;
    double[] peak;

    public static final double LAMBDA_POWER_DEFAULT = 0.7;
    public static final double LAMBDA_A_DEFAULT = 0.995;
    public static final double LAMBDA_B_DEFAULT = 0.5;
    public static final double LAMBDA_T_DEFAULT = 0.85;
    public static final double MU_T_DEFAULT = 0.2;
    public static final double MAX_GAIN_DEFAULT = 20.0;
    public static final int SMOOTH_WINDOW_DEFAULT = 4;

    @S4Double(defaultValue = LAMBDA_POWER_DEFAULT)
    public final static String LAMBDA_POWER = "lambdaPower";
    double lambdaPower;
    @S4Double(defaultValue = LAMBDA_A_DEFAULT)
    public final static String LAMBDA_A = "lambdaA";
    double lambdaA;
    @S4Double(defaultValue = LAMBDA_B_DEFAULT)
    public final static String LAMBDA_B = "lambdaB";
    double lambdaB;
    @S4Double(defaultValue = LAMBDA_T_DEFAULT)
    public final static String LAMBDA_T = "lambdaT";
    double lambdaT;
    @S4Double(defaultValue = MU_T_DEFAULT)
    public final static String MU_T = "muT";
    double muT;
    @S4Double(defaultValue = MAX_GAIN_DEFAULT)
    public final static String MAX_GAIN = "maxGain";
    double maxGain;
    @S4Integer(defaultValue = SMOOTH_WINDOW_DEFAULT)
    public final static String SMOOTH_WINDOW = "smoothWindow";
    int smoothWindow;

    final static double EPS = 1e-10;

    public Denoise(double lambdaPower, double lambdaA, double lambdaB,
        double lambdaT, double muT,
        double maxGain, int smoothWindow) {
        this.lambdaPower = lambdaPower;
        this.lambdaA = lambdaA;
        this.lambdaB = lambdaB;
        this.lambdaT = lambdaT;
        this.muT = muT;
        this.maxGain = maxGain;
        this.smoothWindow = smoothWindow;
    }

    private Denoise() {
        this(LAMBDA_POWER_DEFAULT, LAMBDA_A_DEFAULT, LAMBDA_B_DEFAULT, LAMBDA_T_DEFAULT, MU_T_DEFAULT, MAX_GAIN_DEFAULT, SMOOTH_WINDOW_DEFAULT);
    }

    public static Denoise withDefaults() {
        return new Denoise();
    }

    @Override
    public Data getData() throws DataProcessingException {
        Data inputData = getPredecessor().getData();
        int i;

        if (inputData instanceof DataStartSignal) {
            power = null;
            noise = null;
            floor = null;
            peak = null;
            return inputData;
        }
        if (!(inputData instanceof DoubleData)) {
            return inputData;
        }

        DoubleData inputDoubleData = (DoubleData) inputData;
        double[] input = inputDoubleData.getValues();
        int length = input.length;

        if (power == null)
            initStatistics(input, length);

        updatePower(input);

        estimateEnvelope(power, noise);

        double[] signal = new double[length];
        for (i = 0; i < length; i++) {
            signal[i] = Math.max(power[i] - noise[i], 0.0);
        }

        estimateEnvelope(signal, floor);

        tempMasking(signal);

        powerBoosting(signal);

        double[] gain = new double[length];
        for (i = 0; i < length; i++) {
            gain[i] = signal[i] / (power[i] + EPS);
            gain[i] = Math.min(Math.max(gain[i], 1.0 / maxGain), maxGain);
        }
        double[] smoothGain = smooth(gain);

        for (i = 0; i < length; i++) {
            input[i] *= smoothGain[i];
        }

        return inputData;
    }

    private double[] smooth(double[] gain) {
        double[] result = new double[gain.length];
        for (int i = 0; i < gain.length; i++) {
            int start = Math.max(i - smoothWindow, 0);
            int end = Math.min(i + smoothWindow + 1, gain.length);
            double sum = 0.0;
            for (int j = start; j < end; j++) {
                sum += gain[j];
            }
            result[i] = sum / (end - start);
        }
        return result;
    }

    private void powerBoosting(double[] signal) {
        for (int i = 0; i < signal.length; i++) {
            if (signal[i] < floor[i])
                signal[i] = floor[i];
        }
    }

    private void tempMasking(double[] signal) {
        for (int i = 0; i < signal.length; i++) {
            double in = signal[i];

            peak[i] *= lambdaT;
            if (signal[i] < lambdaT * peak[i])
                signal[i] = peak[i] * muT;

            if (in > peak[i])
                peak[i] = in;
        }
    }

    private void updatePower(double[] input) {
        for (int i = 0; i < input.length; i++) {
            power[i] = lambdaPower * power[i] + (1 - lambdaPower) * input[i];
        }
    }

    private void estimateEnvelope(double[] signal, double[] envelope) {
        for (int i = 0; i < signal.length; i++) {
            if (signal[i] > envelope[i])
                envelope[i] = lambdaA * envelope[i] + (1 - lambdaA) * signal[i];
            else
                envelope[i] = lambdaB * envelope[i] + (1 - lambdaB) * signal[i];
        }
    }

    private void initStatistics(double[] input, int length) {
        /* no previous data, initialize the statistics */
        power = Arrays.copyOf(input, length);
        noise = Arrays.copyOf(input, length);
        floor = new double[length];
        peak = new double[length];
        for (int i = 0; i < length; i++) {
            floor[i] = input[i] / maxGain;
        }
    }
}
