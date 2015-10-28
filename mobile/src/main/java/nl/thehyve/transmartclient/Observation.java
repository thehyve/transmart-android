package nl.thehyve.transmartclient;

/**
 * Created by Ward Weistra on 27-10-15.
 * Copyright (c) 2013 The Hyve B.V.
 * This code is licensed under the GNU Lesser General Public License,
 * version 3, or (at your option) any later version.
 */
public class Observation {
    public Subject subject;
    public String label;
    public String value;

    public Subject getSubject() {
        return subject;
    }

    public String getLabel() {
        return label;
    }

    public String getValue() {
        return value;
    }
}
