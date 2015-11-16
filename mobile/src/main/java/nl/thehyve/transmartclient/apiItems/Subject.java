package nl.thehyve.transmartclient.apiItems;

/**
 * Created by Ward Weistra on 27-10-15.
 * Copyright (c) 2013 The Hyve B.V.
 * This code is licensed under the GNU Lesser General Public License,
 * version 3, or (at your option) any later version.
 */
public class Subject {
    public String religion;
    public String maritalStatus;
    public String race;
    public int id;
    public String birthDate;
    public String age;
    public String deathDate;
    public String trial;
    public String inTrialId;
    public String sex;

    public String getInTrialId() {
        return inTrialId;
    }
}
