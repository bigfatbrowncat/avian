/* Copyright (c) 2008-2013, Avian Contributors

   Permission to use, copy, modify, and/or distribute this software
   for any purpose with or without fee is hereby granted, provided
   that the above copyright notice and this permission notice appear
   in all copies.

   There is NO WARRANTY for this software.  See license.txt for
   details. */

package java.net;

import java.io.IOException;

public abstract class ServerSocket {
	/**
	 * This method is called from all routines that depend on winsock in windows,
	 * so it has public visibility
	 * @throws IOException
	 */
	public static native void init() throws IOException;
	
	/**
	 * Creates the native socket object
	 * @return Handle to the native object
	 * @throws IOException
	 */
	private static native /* SOCKET */long create() throws IOException;
	
	private static native void bind(/* SOCKET */long sock, long addr, short port) throws IOException;
	private static native void bindAny(/* SOCKET */long sock) throws IOException;

	private static native int getLocalAddress(/* SOCKET */long sock) throws IOException;
	private static native int getLocalPort(/* SOCKET */long sock) throws IOException;

	private static native void close(/* SOCKET */long sock);

	private long sock;
	private InetSocketAddress localAddress;
	
	public ServerSocket() throws IOException {
		init();
		sock = create();
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
		if (localAddress != null) {
			throw new SocketException("Socket already bound to " + localAddress.toString());
		}

		if (bindpoint == null) {
			bindAny(sock);
		}
		if (bindpoint instanceof InetSocketAddress) {
			InetSocketAddress inetBindpoint = (InetSocketAddress)bindpoint;
			bind(sock, inetBindpoint.getAddress().getRawAddress(), (short) inetBindpoint.getPort());
			localAddress = inetBindpoint;
		} else {
			throw new IllegalArgumentException("only InetSocketAddress supported so far");
		}
	}
}
