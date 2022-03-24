/*
 * Author: Jonatan Schroeder
 * Updated: March 2022
 *
 * This code may not be used without written consent of the authors.
 */

package ca.yorku.rtsp.client.exception;

public class RTSPException extends Exception {

    public RTSPException(String message) {
        super(message);
    }

    public RTSPException(Throwable cause) {
        super(cause);
    }

    public RTSPException(String message, Throwable cause) {
        super(message, cause);
    }
}
