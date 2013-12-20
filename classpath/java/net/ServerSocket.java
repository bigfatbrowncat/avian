/* Copyright (c) 2008-2013, Avian Contributors

   Permission to use, copy, modify, and/or distribute this software
   for any purpose with or without fee is hereby granted, provided
   that the above copyright notice and this permission notice appear
   in all copies.

   There is NO WARRANTY for this software.  See license.txt for
   details. */

package java.net;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ServerSocket implements Closeable, AutoCloseable  {
	
	private static final int SOMAXCONN = 0x7fffffff;
	
	private SocketImpl socketImpl;
	private int backlog = SOMAXCONN;

	public ServerSocket() throws IOException {
		socketImpl = new DefaultSocketImpl();
	}
	
	public ServerSocket(int port) throws IOException {
		this();
		socketImpl.bind(InetAddress.getByName("127.0.0.1"), port);
	}
	public ServerSocket(int port, int backlog) throws IOException {
		this(port);
		this.backlog = backlog;
	}
	
	public ServerSocket(int port, int backlog, InetAddress bindAddr) throws IOException {
		this();
		this.backlog = backlog;
		socketImpl.bind(bindAddr, port);
	}
	
	public Socket accept() throws IOException {
		Socket newSock = new Socket();
		socketImpl.listen(backlog);
		socketImpl.accept(newSock.getSocketImpl());
		return newSock;
	}
	
	/**
	 * Binds the socket to a local address. If <code>null</code> address is specified, the OS
	 * decides which address to bind
	 * @param endpoint the server address
	 * @throws IOException in the case of a connection problem
	 * @throws IllegalArgumentException if endpoint is null or it's a {@link SocketAddress} 
	 * subclass that's not supported by this socket
	 */
	public void bind(SocketAddress bindpoint) throws IOException, SocketException {
		socketImpl.bind(((InetSocketAddress)bindpoint).getAddress(), ((InetSocketAddress)bindpoint).getPort());
	}
	
	public void bind(SocketAddress bindpoint, int backlog) throws IOException, SocketException {
		socketImpl.bind(((InetSocketAddress)bindpoint).getAddress(), ((InetSocketAddress)bindpoint).getPort());
		this.backlog = backlog;
	}
	
	@Override
	public void close() throws IOException {
		socketImpl.close();
	}
	
	/**
	 * Gets the input stream of the socket. Closing this stream means closing the socket input. 
	 * @return the input stream of the socket.
	 */
	public InputStream getInputStream() {
		return socketImpl.getInputStream();
	}
	
	/**
	 * Gets the output stream of the socket. Closing this stream means closing the socket output. 
	 * @return the output stream of the socket.
	 */
	public OutputStream getOutputStream() {
		return socketImpl.getOutputStream();
	}
	
	public int available() throws IOException {
		return getInputStream().available();
	}
}
