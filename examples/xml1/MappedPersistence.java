package com.dg.demo.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A simple interface to return a map of
 * values for the demonstration.
 */
public interface MappedPersistence {

    // returns a formatted string of the cities
    // mapped to a particular state.
    String getCommonValues(String state);

    // returns a set of cities, which are
    // the map keys.
    Set<String> keys();

    // returns a map of cities <K> with states<V>
    // as the value.
    Map<String, String> proxy();

    // returns a set of states available.
    Set<String> valueSet();

    //returns a List of cities.
    List<String> mappedKeys(String s);

}