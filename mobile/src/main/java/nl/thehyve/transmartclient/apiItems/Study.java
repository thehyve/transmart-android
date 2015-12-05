package nl.thehyve.transmartclient.apiItems;

/**
 * Created by Ward Weistra on 03-12-15.
 * Copyright (c) 2013 The Hyve B.V.
 * This code is licensed under the GNU Lesser General Public License,
 * version 3, or (at your option) any later version.
 */
public class Study {
    private String id;
    private OntologyTerm ontologyTerm;

    public String getId() {
        return id;
    }

    public OntologyTerm getOntologyTerm() {
        return ontologyTerm;
    }

    public class OntologyTerm {
        private String name;
        private String key;
        private String fullName;
        private String type;

        public String getName() {
            return name;
        }

        public String getKey() {
            return key;
        }

        public String getFullName() {
            return fullName;
        }

        public String getType() {
            return type;
        }
    }
}
