/*
 * Author: Jonatan Schroeder
 * Updated: March 2022
 *
 * This code may not be used without written consent of the authors.
 */

package ca.yorku.rtsp.client.net;

import ca.yorku.rtsp.client.exception.RTSPException;
import ca.yorku.rtsp.client.model.Frame;
import ca.yorku.rtsp.client.model.Session;
import java.awt.Toolkit;
import java.util.*;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.StringTokenizer;
import javax.swing.Timer;
/**
 * This class represents a connection with an RTSP server.
 */
public class RTSPConnection {

    private static final int BUFFER_LENGTH = 0x10000;

    private Session session;
    private Socket RTSPsocket;
    public DatagramSocket RTPsocket;
    public DatagramPacket RTPPacket;
    private InetAddress ServerIPAddr;
    private int RTSPSeqNb;
    String rtspSessionId;
    static BufferedReader RTSPBufferedReader;
    static BufferedWriter RTSPBufferedWriter;
    static int state;
    static int RTP_PORT = 8000;
    private String videoName;
    double statStartTime;
    private Timer timer;
    RTPReceivingThread rtpRceivedataThread;
    byte[] rtpBuffer;
    int payload_size;
    public byte[] payload;

    // TODO Add additional fields, if necessary

    /**
     * Establishes a new connection with an RTSP server. No message is sent at this point, and no stream is set up.
     *
     * @param session The Session object to be used for connectivity with the UI.
     * @param server  The hostname or IP address of the server.
     * @param port    The TCP port number where the server is listening to.
     * @throws RTSPException If the connection couldn't be accepted, such as if the host name or port number are invalid
     *                       or there is no connectivity.
     */
    public RTSPConnection(Session session, String server, int port) throws RTSPException {
        this.rtpRceivedataThread = new RTPReceivingThread(this);
        this.session = session;
        this.state = 0;
        this.rtpBuffer = new byte[15000];
        System.out.println("RTSP state: INIT");
        try {

            this.RTPsocket = new DatagramSocket(this.RTP_PORT);
            this.RTPsocket.setSoTimeout(5);

            ServerIPAddr = InetAddress.getByName(server);
            RTSPsocket = new Socket(ServerIPAddr, port);
            RTSPBufferedReader = new BufferedReader(new InputStreamReader(this.RTSPsocket.getInputStream()));
            RTSPBufferedWriter = new BufferedWriter(new OutputStreamWriter(this.RTSPsocket.getOutputStream()));
        } catch (UnknownHostException e) {

        } catch (IOException b) {

        }
      /*  VideoFileName = var0[2];
        DatagramSocket RTPsocket;*/
        //  Socket RTPsocket = new Socket(ServerIPAddr, port);
       /* RTSPBufferedReader = new BufferedReader(new InputStreamReader(var1.RTSPsocket.getInputStream()));
        RTSPBufferedWriter = new BufferedWriter(new OutputStreamWriter(var1.RTSPsocket.getOutputStream()));
        state = 0;*/
        // TODO
    }

    /**
     * Sends a SETUP request to the server. This method is responsible for sending the SETUP request, receiving the
     * response and retrieving the session identification to be used in future messages. It is also responsible for
     * establishing an RTP datagram socket to be used for data transmission by the server. The datagram socket should be
     * created with a random UDP port number, and the port number used in that connection has to be sent to the RTSP
     * server for setup. This datagram socket should also be defined to timeout after 1 second if no packet is
     * received.
     *
     * @param videoName The name of the video to be setup.
     * @throws RTSPException If there was an error sending or receiving the RTSP data, or if the RTP socket could not be
     *                       created, or if the server did not return a successful response.
     */
    public synchronized void setup(String videoName) throws RTSPException {

        // TODO
        this.videoName = videoName;
        System.out.println("Setup Button pressed !");
        int rtspResponseCode;
        this.RTSPSeqNb = 1;
        try {
            RTSPBufferedWriter.write("SETUP " + videoName + " RTSP/1.0\r\n");
            RTSPBufferedWriter.write("CSeq: " + this.RTSPSeqNb + "\r\n");
            RTSPBufferedWriter.write("Transport: RTP/UDP;client_port=" + this.RTP_PORT + "\r\n\r\n");
            RTSPBufferedWriter.flush();

            /*Get the server response*/
            String serverResponse = RTSPBufferedReader.readLine();
            /*Print teh server response into the terminal*/
            System.out.println("RTSP Client - Received from Server:");
            System.out.println(serverResponse);
            /*break the server response into tokens, to be able to check the response code*/
            StringTokenizer serverResponseTokens = new StringTokenizer(serverResponse);
            serverResponseTokens.nextToken();
            rtspResponseCode = Integer.parseInt(serverResponseTokens.nextToken());
            if (rtspResponseCode == 200) {
                String SCeqNumber = RTSPBufferedReader.readLine();
                System.out.println(SCeqNumber);

                String SessionID = RTSPBufferedReader.readLine();
                System.out.println(SessionID);

                /*break the server response into tokens, to be able to retrieve session ID*/
                serverResponseTokens = new StringTokenizer(SessionID);
                String secondLineResponse = serverResponseTokens.nextToken();

                if (this.state == 0 && secondLineResponse.compareTo("Session:") == 0) {
                    this.rtspSessionId = serverResponseTokens.nextToken();
                    System.out.println("The session ID is :" + this.rtspSessionId);
                    this.state = 1;
                    System.out.println("RTSP state: READY");
                } else if (secondLineResponse.compareTo("Content-Base:") == 0) {
                    System.out.println("Response contains Content-base field");

                }

            }

        } catch (Exception exp) {
            System.out.println("Exception caught: " + exp);
            System.exit(0);
        }

    }

    /**
     * Sends a PLAY request to the server. This method is responsible for sending the request, receiving the response
     * and, in case of a successful response, starting a separate thread responsible for receiving RTP packets with
     * frames.
     *
     * @throws RTSPException If there was an error sending or receiving the RTSP data, or if the server did not return a
     *                       successful response.
     */
    public synchronized void play() throws RTSPException {

        System.out.println("Play Button pressed!");
        this.statStartTime = (double) System.currentTimeMillis();
        if (this.state == 1) {
            ++this.RTSPSeqNb;
            int rtspResponseCode = this.sendPlayRequest();

            if (rtspResponseCode != 200) {
                System.out.println("Invalid Server Response");
            } else {
                this.state = 2;
                System.out.println("RTSP state: PLAYING");
                if(rtpRceivedataThread.threadSuspended)
                {
                    synchronized(rtpRceivedataThread) {
                        rtpRceivedataThread.notify();
                    }
                rtpRceivedataThread.threadSuspended=false;
                }
                else
                    rtpRceivedataThread.start();
            }
        }
        // TODO
    }

    private int sendPlayRequest() {

        int rtspResponseCode = 0;
        try {
            /* Create a socket that will be used for RTP receive traffic*/


            RTSPBufferedWriter.write("PLAY " + this.videoName + " RTSP/1.0\r\n");
            RTSPBufferedWriter.write("CSeq: " + this.RTSPSeqNb + "\r\n");
            RTSPBufferedWriter.write("Session: " + this.rtspSessionId + "\r\n\r\n");
            RTSPBufferedWriter.flush();



            /*Get the server response*/
            String playResponse;
            do {
                playResponse = RTSPBufferedReader.readLine();
            } while (playResponse.compareTo("") == 0);
            /*Print teh server response into the terminal*/
            System.out.println("RTSP Client - Received from Server:");
            System.out.println(playResponse);
            /*break the server response into tokens, to be able to check the response code*/
            StringTokenizer serverResponseTokens = new StringTokenizer(playResponse);
            serverResponseTokens.nextToken();
            rtspResponseCode = Integer.parseInt(serverResponseTokens.nextToken());

            playResponse = RTSPBufferedReader.readLine();
            playResponse = RTSPBufferedReader.readLine();

        } catch (Exception exp) {
            System.out.println("Exception caught: " + exp);
            System.exit(0);
        }
        return rtspResponseCode;
    }


    private class RTPReceivingThread extends Thread {
        /**
         * Continuously receives RTP packets until the thread is cancelled. Each packet received from the datagram
         * socket is assumed to be no larger than BUFFER_LENGTH bytes. This data is then parsed into a Frame object
         * (using the parseRTPPacket method) and the method session.processReceivedFrame is called with the resulting
         * packet. The receiving process should be configured to timeout if no RTP packet is received after two seconds.
         */

        public RTSPConnection rtspConnection;
        boolean threadSuspended;
        RTPReceivingThread(RTSPConnection conection) {
            this.rtspConnection = conection;
            threadSuspended=false;
        }

        public void run() {
            double elapsedTime=0.0, start=(double)System.currentTimeMillis();
            while (true) {
                rtspConnection.RTPPacket = new DatagramPacket(rtspConnection.rtpBuffer, rtspConnection.rtpBuffer.length);

                try {
                    /* parsed into a Frame object */
                    elapsedTime=(double)System.currentTimeMillis()-start;
                    if(elapsedTime>2000)
                        break;
                    if(rtpRceivedataThread.threadSuspended)
                        synchronized(rtpRceivedataThread) {
                            rtpRceivedataThread.wait();
                        }
                    rtspConnection.RTPsocket.receive(rtspConnection.RTPPacket);
                    Frame frame = rtspConnection.parseRTPPacket(rtspConnection.RTPPacket);

                    System.out.println("Packet Received");
                    /* called with the resulting packet */
                    session.processReceivedFrame(frame);
                    this.sleep(20);
                    start=(double)System.currentTimeMillis();

                    } catch (InterruptedIOException exception) {
                        System.out.println("Nothing to read");

                    } catch (IOException e) {
                        System.out.println("Exception caught: " + e);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    // TODO
                }
            System.out.println("************ End of THREAD ****************");
            }
        }

        /**
         * Sends a PAUSE request to the server. This method is responsible for sending the request, receiving the response
         * and, in case of a successful response, stopping the thread responsible for receiving RTP packets with frames.
         *
         * @throws RTSPException If there was an error sending or receiving the RTSP data, or if the server did not return a
         *                       successful response.
         */
        public synchronized void pause() throws RTSPException {

            System.out.println("Pause Button pressed!");
            if(this.state==2) {
                this.RTSPSeqNb++;
                /*Send Pause request*/
                sendPauseRequest();
                try {
                    /*Get Pause response*/
                    RTSPResponse response = readRTSPResponse();

                    if(response.getResponseCode()==200){
                        this.state = 1;
                        System.out.println("RTSP state: READY");
                        rtpRceivedataThread.threadSuspended=true;
                    }
                    else{
                        System.out.println("Invalid Server pause Response");
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }/*catch (InterruptedException e) {
                    e.printStackTrace();
                }*/
            }
        }
    private int sendPauseRequest() {

        int rtspResponseCode = 0;
        try {
            /* Create a socket that will be used for RTP receive traffic*/

            RTSPBufferedWriter.write("PAUSE " + this.videoName + " RTSP/1.0\r\n");
            RTSPBufferedWriter.write("CSeq: " + this.RTSPSeqNb + "\r\n");
            RTSPBufferedWriter.write("Session: " + this.rtspSessionId + "\r\n\r\n");
            RTSPBufferedWriter.flush();

        } catch (Exception exp) {
            System.out.println("Exception caught: " + exp);
            System.exit(0);
        }
        return rtspResponseCode;
    }
        /**
         * Sends a TEARDOWN request to the server. This method is responsible for sending the request, receiving the
         * response and, in case of a successful response, closing the RTP socket. This method does not close the RTSP
         * connection, and a further SETUP in the same connection should be accepted. Also this method can be called both
         * for a paused and for a playing stream, so the thread responsible for receiving RTP packets will also be
         * cancelled.
         *
         * @throws RTSPException If there was an error sending or receiving the RTSP data, or if the server did not return a
         *                       successful response.
         */
        public synchronized void teardown() throws RTSPException {

            // TODO
        }

        /**
         * Closes the connection with the RTSP server. This method should also close any open resource associated to this
         * connection, such as the RTP connection, if it is still open.
         */
        public synchronized void closeConnection() {
            // TODO
        }

        /**
         * Parses an RTP packet into a Frame object.
         *
         * @param packet the byte representation of a frame, corresponding to the RTP packet.
         * @return A Frame object.
         */
        public static Frame parseRTPPacket(DatagramPacket packet) {

            // TODO
            // Frame(byte payloadType, boolean marker, short sequenceNumber, int timestamp, byte[] payload, int offset, int length)
            Frame frame;
            int offset = 12, HeaderSize = 12, payloadSize;
            int payloadLength = packet.getLength();
            byte[] rtpBuffer = packet.getData();
            byte[] header = new byte[12];
            byte[] payload;
            int version, TimeStamp;
            byte payloadType;
            short SequenceNumber;
            if (payloadLength >= 12) {
                /* get the rtp header*/
                System.arraycopy(rtpBuffer, 0, header, 0, 12);
                payloadSize = payloadLength - HeaderSize;
                payload = new byte[payloadSize];
                // System.arraycopy(rtpBuffer,12,payload,0,payloadSize );
                version = (header[0] & 255) >>> 6;
                payloadType = (byte) (header[1] & 127);
                SequenceNumber = (byte) ((header[3] & 255) + ((header[2] & 255) << 8));
                TimeStamp = (header[7] & 255) + ((header[6] & 255) << 8) + ((header[5] & 255) << 16) + ((header[4] & 255) << 24);
                frame = new Frame(payloadType, false, SequenceNumber, TimeStamp, rtpBuffer, offset, payloadSize);
            } else {
                frame = null;
            }

            return frame; // Replace with a proper Frame
        }

        /**
         * Reads and parses an RTSP response from the socket's input.
         *
         * @return An RTSPResponse object if the response was read completely, or null if the end of the stream was reached.
         * @throws IOException   In case of an I/O error, such as loss of connectivity.
         * @throws RTSPException If the response doesn't match the expected format.
         */
        public RTSPResponse readRTSPResponse() throws IOException, RTSPException {

            RTSPResponse  rtspResponse;
            /*Get the server response*/
            String serverResponse;
            do {
                serverResponse = RTSPBufferedReader.readLine();
            } while (serverResponse.compareTo("") == 0);
            /*Print teh server response into the terminal*/
            System.out.println("RTSP Client - Received from Server:");
            System.out.println(serverResponse);
            /*break the server response into tokens, to be able to check the response code*/
            StringTokenizer serverResponseTokens = new StringTokenizer(serverResponse);
            String rtspVersion = serverResponseTokens.nextToken();
            int rtspResponseCode = Integer.parseInt(serverResponseTokens.nextToken());
            String rtspMessageCode=new String("");
            if(serverResponseTokens.hasMoreElements())
                rtspMessageCode= serverResponseTokens.nextToken();

            rtspResponse= new RTSPResponse(rtspVersion,rtspResponseCode,rtspMessageCode);
            String headerName="",Value="";
            do {
                serverResponse = RTSPBufferedReader.readLine();
                serverResponseTokens = new StringTokenizer(serverResponse);
                if(serverResponseTokens.hasMoreElements())
                     headerName = serverResponseTokens.nextToken();
                if(serverResponseTokens.hasMoreElements())
                     Value      = serverResponseTokens.nextToken();
                rtspResponse.addHeaderValue(headerName,Value);
            } while (serverResponse.compareTo("") != 0);

            return rtspResponse; // Replace with a proper RTSPResponse
        }

    }

