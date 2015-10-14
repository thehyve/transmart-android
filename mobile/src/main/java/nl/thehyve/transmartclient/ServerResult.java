package nl.thehyve.transmartclient;

/**
 * Created by Ward Weistra on 22-12-14.
 * Copyright (c) 2015 The Hyve B.V.
 * This code is licensed under the GNU Lesser General Public License,
 * version 3, or (at your option) any later version.
 */

public class ServerResult {
    private int responseCode;
    private String responseDescription;
    private String result;

    public ServerResult() {
        this.responseCode = 0;
        this.responseDescription = "";
        this.result = "";
    }

    public ServerResult(int responseCode, String responseDescription, String result) {
        this.responseCode = responseCode;
        this.responseDescription = responseDescription;
        this.result = result;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public String getResponseDescription() {
        return responseDescription;
    }

    public void setResponseDescription(String responseDescription) {
        this.responseDescription = responseDescription;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
