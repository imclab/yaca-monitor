/*
 * Copyright (C) 2012-2014, Markus Sprunck <sprunck.markus@gmail.com>
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * - The name of its contributor may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.sw_engineering_candies.yaca;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The class is a minimal web server to provide data to a external client
 */
public class WebServer extends Thread {

    /**
     * Constants
     */
    private static final Log LOGGER = LogFactory.getLog(WebServer.class);
    private static final String NL = System.getProperty("line.separator");

    /**
     * Port for this HTTP server
     */
    private final int port;

    public WebServer(final int port) {
	this.port = port;
    }

    @Override
    public void run() {
	Socket connection = null;
	while (true) {
	    try {
		final ServerSocket server = new ServerSocket(port);
		connection = server.accept();
		final OutputStream out = new BufferedOutputStream(connection.getOutputStream());
		final InputStream in = new BufferedInputStream(connection.getInputStream());
		final String request = readFirstLineOfRequest(in).toString();
		LOGGER.debug("get request " + request.toString());

		if (request.startsWith("GET /process")) {

		    String newPID = request.substring(15, 19);
		    try {
			Integer.valueOf(newPID);
			CallStackAnalyser.setProcessNewID(newPID);
		    } catch (NumberFormatException ex) {
			// this can be ignored
		    }

		    // Create content of response
		    final StringBuffer jsonpModel = Agent.MODEL.getJSONPModel();
		    ByteArrayOutputStream compressedString = compress(jsonpModel.toString());

		    // For HTTP/1.0 or later send a MIME header
		    if (request.indexOf("HTTP/1.1") != -1) {
			final String headerString = "HTTP/1.1 200 OK" + NL + "Server: YacaAgent 2.0" + NL
				+ "Content-Type: application/json" + NL + "Content-Encoding: gzip" + NL
				+ "Content-Length: " + compressedString.size() + NL + NL;
			final byte[] header = headerString.getBytes("UTF-8");
			out.write(header);
			out.write(compressedString.toByteArray());
			out.flush();
		    }
		} else if (request.startsWith("GET /terminate")) {
		    throw new RuntimeException("terminate");
		}
		// Close the socket
		connection.close();
		in.close();
		out.close();
		server.close();
	    } catch (final IOException e) {
		LOGGER.error(e.getMessage());
	    }
	}
    }

    public static ByteArrayOutputStream compress(String str) throws IOException {
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	GZIPOutputStream gzip = new GZIPOutputStream(out);
	gzip.write(str.getBytes("UTF-8"));
	gzip.close();
	return out;
    }

    private StringBuffer readFirstLineOfRequest(final InputStream in) throws IOException {
	final StringBuffer request;
	request = new StringBuffer(100);
	while (true) {
	    final int character = in.read();
	    if ((character == '\n') || (character == '\r') || (character == -1)) {
		break;
	    }
	    request.append((char) character);
	}
	return request;
    }

}
