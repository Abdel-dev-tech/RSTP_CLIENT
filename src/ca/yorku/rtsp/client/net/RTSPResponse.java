/*
 * Author: Jonatan Schroeder
 * Updated: March 2022
 *
 * This code may not be used without written consent of the authors.
 */

package ca.yorku.rtsp.client.net;

import java.util.HashMap;
import java.util.Map;

/**
 * This class represents an RTSP response. The method
 * <code>readRTSPResponse</code> is used to read a response from a
 * BufferedReader (usually associated to a socket).
 */
public class RTSPResponse {

    private String rtspVersion;
    private int responseCode;
    private String responseMessage;
    private Map<String, String> headers;

    /**
     * Creates an RTSP response.
     *
     * @param rtspVersion     The String representation of the RTSP version (e.g., "RTSP/1.0").
     * @param responseCode    The response code corresponding the result of the requested operation.
     * @param responseMessage The response message associated to the response code.
     */
    public RTSPResponse(String rtspVersion, int responseCode, String responseMessage) {
        this.rtspVersion = rtspVersion;
        this.responseCode = responseCode;
        this.responseMessage = responseMessage;
        this.headers = new HashMap<String, String>();
    }

    /**
     * Returns the RTSP version included in the response. It is expected to be "RTSP/1.0".
     *
     * @return A String representing the RTSP version read from the response.
     */
    public String getRtspVersion() {
        return rtspVersion;
    }

    /**
     * Returns the numeric response code included in the response. The code 200 represent a successful response, while a
     * code between 400 and 599 represents an error.
     *
     * @return The response code of the RTSP response.
     */
    public int getResponseCode() {
        return responseCode;
    }

    /**
     * Returns the response message associated to the response code. It should not be used for any automated
     * verification, and is usually only intended for human users.
     *
     * @return A String representing the message associated to the response code.
     */
    public String getResponseMessage() {
        return responseMessage;
    }

    /**
     * Returns the value of the named header field.
     *
     * @param headerName The name of the header field to be retrieved. Header names are case-insensitive.
     * @return The value of the header field named by headerName, or null if that header wasn't included in the
     * response.
     */
    public String getHeaderValue(String headerName) {
        return headers.get(headerName.toUpperCase());
    }

    /**
     * Sets the value of a named header field.
     *
     * @param headerName  The name of the header field to be updated. Header names are case-insensitive.
     * @param headerValue The new value of the header field.
     */
    public void addHeaderValue(String headerName, String headerValue) {
        headers.put(headerName.toUpperCase(), headerValue);
    }

    @Override
    public String toString() {
        return responseCode + " '" + responseMessage + '\'';
    }
}