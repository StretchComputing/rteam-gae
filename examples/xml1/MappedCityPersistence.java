package com.dg.demo.model.impl;

import com.dg.demo.model.MappedPersistence;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of MappedPersistence Interface.
 * Extends Abstract Map, but obfuscates the
 * mappings via the MappedPersistence Interface.
 */
public class MappedCityPersistence
        extends AbstractMap<String, String>
        implements MappedPersistence {

    private Map<String, String> proxyMap;

    public MappedCityPersistence() {
        proxyMap = new HashMap<String, String>();
        loadProxy();
    }
    // this class must be implemented...
    public Set<Entry<String, String>> entrySet() {
        return proxyMap.entrySet();
    }
    //the values for persistence.
    private void loadProxy() {
        proxyMap.put("Los Angeles", "CA");
        proxyMap.put("Santa Barbara", "CA");
        proxyMap.put("San Francisco", "CA");
        proxyMap.put("San Diego", "CA");
        proxyMap.put("Dallas", "TX");
        proxyMap.put("Houston", "TX");
        proxyMap.put("Austin", "TX");
        proxyMap.put("El Paso", "TX");
        proxyMap.put("Chicago", "IL");
        proxyMap.put("Joliet", "IL");
        proxyMap.put("Naperville", "IL");
        proxyMap.put("Oak Park", "IL");
    }

    @Override
    public String toString() {
        StringBuilder builder =
                new StringBuilder("City/State values:\n");
        for (String s : proxyMap.keySet()) {
            builder.append(s).append(" => ")
                    .append(proxyMap.get(s)).append("\n");
        }
        return builder.toString();
    }

    public String getCommonValues(String value) {
        StringBuilder builder =
                new StringBuilder("Cities located in "
                        + value + ":\n");
        for (String s : proxyMap.keySet()) {
            if (proxyMap.get(s).equals(value)) {
                builder.append(s).append("\n");
            }
        }
        return builder.toString();
    }

    public Set<String> keys() {
        return proxyMap.keySet();
    }

    public Map<String, String> proxy() {
        return this.proxyMap;
    }

    public Set<String> valueSet() {
        Set<String> quickAndDirtySet = new HashSet<String>();
        for (String s : proxyMap.values()) {
            quickAndDirtySet.add(s);
        }
        return quickAndDirtySet;
    }

    public List<String> mappedKeys(String s) {
        List<String> quickAndDirtyList = new ArrayList<String>();
        for (String key : proxyMap.keySet()) {
            if (proxyMap.get(key).equals(s)) {
                quickAndDirtyList.add(key);
            }
        }
        return quickAndDirtyList;
    }

    public static void main(String[] args) {
        MappedCityPersistence p = new MappedCityPersistence();
        System.out.println(p.getCommonValues("TX"));
    }
}