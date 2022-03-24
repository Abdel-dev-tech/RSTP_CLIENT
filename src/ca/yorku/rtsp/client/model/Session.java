/*
 * Author: Jonatan Schroeder
 * Updated: March 2022
 *
 * This code may not be used without written consent of the authors.
 */

package ca.yorku.rtsp.client.model;

import ca.yorku.rtsp.client.exception.RTSPException;
import ca.yorku.rtsp.client.net.RTSPConnection;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * This class manages an open session with an RTSP server. It provides the main interaction between the network
 * interface and the user interface.
 */
public class Session {

    private Set<SessionListener> sessionListeners = new HashSet<SessionListener>();
    private RTSPConnection rtspConnection;
    private String videoName = null;

    /**
     * Creates a new RTSP session. This constructor will also create a new network connection with the server. No stream
     * setup is established at this point.
     *
     * @param server The IP address or host name of the RTSP server.
     * @param port   The port where the RTSP server is listening to.
     * @throws RTSPException If it was not possible to establish a connection with the server.
     */
    public Session(String server, int port) throws RTSPException {

        rtspConnection = new RTSPConnection(this, server, port);
    }

    /**
     * Adds a new listener interface to be called every time a session event (such as a change in video name or a new
     * frame) happens. Any interaction with user interfaces is done through these listeners.
     *
     * @param listener A SessionListener to be called when a session event happens.
     */
    public synchronized void addSessionListener(SessionListener listener) {
        sessionListeners.add(listener);
        listener.videoNameChanged(this.videoName);
    }

    /**
     * Removes an existing listener from the list of listeners to be called for session events.
     *
     * @param listener A SessionListener that should no longer be called when a session event happens.
     */
    public void removeSessionListener(SessionListener listener) {
        sessionListeners.remove(listener);
    }

    /**
     * Opens a new video file in the interface.
     *
     * @param videoName The name (URL) of the video to be opened. It should correspond to a local file in the server.
     */
    public void open(String videoName) {
        try {
            rtspConnection.setup(videoName);
            this.videoName = videoName;
            for (SessionListener listener : sessionListeners)
                listener.videoNameChanged(this.videoName);
        } catch (RTSPException e) {
            listenerException(e);
        }
    }

    /**
     * Starts to play the existing file. It should only be called once a file has been opened. This function will return
     * immediately after the request was responded. Frames will be received in the background and will be handled by
     * the
     * <code>processReceivedFrame</code> method. If the video has been paused previously, playback will resume where it
     * stopped.
     */
    public void play() {
        try {
            rtspConnection.play();
        } catch (RTSPException e) {
            listenerException(e);
        }
    }

    /**
     * Pauses the playback the existing file. It should only be called once a file has started playing. This function
     * will return immediately after the request was responded. The server might still send a few frames before stopping
     * the playback completely.
     */
    public void pause() {
        try {
            rtspConnection.pause();
        } catch (RTSPException e) {
            listenerException(e);
        }
    }

    /**
     * Closes the currently open file. It should only be called once a file has been open.
     */
    public synchronized void close() {
        try {
            rtspConnection.teardown();
            processReceivedFrame(null);
            videoName = null;
            for (SessionListener listener : sessionListeners)
                listener.videoNameChanged(this.videoName);
        } catch (RTSPException e) {
            listenerException(e);
        }
    }

    private void listenerException(RTSPException e) {
        for (SessionListener listener : sessionListeners)
            listener.exceptionThrown(e);
    }

    /**
     * Closes the connection with the current server. This session element should not be used anymore after this point.
     */
    public void closeConnection() {
        rtspConnection.closeConnection();
    }

    /**
     * Processes a frame received from the RTSP server. This method will direct the frame to the user interface to be
     * processed and presented to the user.
     *
     * @param frame The recently received frame.
     */
    public synchronized void processReceivedFrame(Frame frame) {
        if (videoName == null) return;
        for (SessionListener listener : sessionListeners)
            listener.frameReceived(frame);
    }

    /**
     * Returns the name of the currently opened video.
     *
     * @return The name of the video currently open, or null if no video is open.
     */
    public String getVideoName() {
        return videoName;
    }
}
