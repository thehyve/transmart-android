package nl.thehyve.transmartclient.rest;

/**
 * Created by Ward Weistra on 18-10-15.
 * Copyright (c) 2013 The Hyve B.V.
 * This code is licensed under the GNU Lesser General Public License,
 * version 3, or (at your option) any later version.
 */
public interface RestInteractionListener {
    void authorizationLost();
    void connectionLost();
}
