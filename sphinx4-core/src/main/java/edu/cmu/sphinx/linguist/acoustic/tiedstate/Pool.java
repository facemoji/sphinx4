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


import java.util.ArrayList;
import java.util.List;

/** Used to pool shared objects in the acoustic model */
public class Pool<T> {

    private final String name;
    private final List<T> pool;

    /**
     * Creates a new pool.
     *
     * @param name the name of the pool
     */
    public Pool(String name) {
        this.name = name;
        pool = new ArrayList<>();
    }

    /**
     * Returns the pool's name.
     *
     * @return the pool name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the object with the given ID from the pool.
     *
     * @param id the id of the object
     * @return the object
     * @throws IndexOutOfBoundsException if the ID is out of range
     */
    public T get(int id) {
        return pool.get(id);
    }

    /**
     * Places the given object in the pool.
     *
     * @param id a unique ID for this object
     * @param o  the object to add to the pool
     */
    public void put(int id, T o) {
        if (id == pool.size()) {
            pool.add(o);
        } else {
            pool.set(id, o);
        }
    }

    /**
     * Retrieves the size of the pool.
     *
     * @return the size of the pool
     */
    public int size() {
        return pool.size();
    }

    @Override
    public String toString() {
        return "Pool " + name + " Entries: " + size();
    }

}
