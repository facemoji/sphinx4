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

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a named value. A StatisticsVariable may be used to track data in a fashion that will allow the data to be
 * viewed or dumped at any time.  Statistics are kept in a pool and are grouped in contexts. Statistics can be dumped
 * as a whole or by context.
 */
public class StatisticsVariable {

    private static final Map<String, StatisticsVariable> pool = new HashMap<String, StatisticsVariable>();

    /** the value of this StatisticsVariable. It can be manipulated directly by the application. */
    public double value;

    private final String name;        // the name of this value
    private boolean enabled;        // if true this var is enabled


    /**
     * Gets the StatisticsVariable with the given name from the given context. If the statistic does not currently
     * exist, it is created. If the context does not currently exist, it is created.
     *
     * @param statName the name of the StatisticsVariable
     * @return the StatisticsVariable with the given name and context
     */
    static public StatisticsVariable getStatisticsVariable(String statName) {

        StatisticsVariable stat = pool.get(statName);
        if (stat == null) {
            stat = new StatisticsVariable(statName);
            pool.put(statName, stat);
        }
        return stat;
    }


    /**
     * Contructs a StatisticsVariable with the given name and context
     *
     * @param statName the name of this StatisticsVariable
     */
    private StatisticsVariable(String statName) {
        this.name = statName;
        this.value = 0.0;
    }


    /**
     * Retrieves the name of this StatisticsVariable
     *
     * @return the name of this StatisticsVariable
     */
    public String getName() {
        return name;
    }


    /**
     * Retrieves the value for this StatisticsVariable
     *
     * @return the current value for this StatisticsVariable
     */
    public double getValue() {
        return value;
    }


    /**
     * Sets the value for this StatisticsVariable
     *
     * @param value the new value
     */
    public void setValue(double value) {
        this.value = value;
    }


    /**
     * Sets the enabled state of this StatisticsVariable
     *
     * @param enabled the new enabled state
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }


}
