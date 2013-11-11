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

public class Socket implements Closeable, AutoCloseable {
	
	private static final int BUFFER_SIZE = 65535;
	
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
	
	/**
	 * Connects the native socket object to an address:port
	 * @param sock Native socket handler
	 * @param addr Address to connect to
	 * @param port Port to connect to
	 * @throws IOException
	 */
	private static native void connect(/* SOCKET */long sock, long addr, short port) throws IOException;
	private static native void connectTimeout(/* SOCKET */long sock, long addr, short port, int timeout) throws IOException, SocketTimeoutException;
	private static native void bind(/* SOCKET */long sock, long addr, short port) throws IOException;
	private static native void bindAny(/* SOCKET */long sock) throws IOException;

	private static native int getLocalAddress(/* SOCKET */long sock) throws IOException;
	private static native int getLocalPort(/* SOCKET */long sock) throws IOException;
	private static native int getRemoteAddress(/* SOCKET */long sock) throws IOException;
	private static native int getRemotePort(/* SOCKET */long sock) throws IOException;

	
	private static native void send(/* SOCKET */long sock, byte[] buffer, int start_pos, int count) throws IOException;
	private static native int recv(/* SOCKET */long sock, byte[] buffer, int start_pos, int count) throws IOException;
	
	private static native void abort(/* SOCKET */long sock);
	private static native void close(/* SOCKET */long sock);
	private static native void closeOutput(/* SOCKET */long sock);
	private static native void closeInput(/* SOCKET */long sock);
	
	private class SocketInputStream extends InputStream {

		private boolean closed = false;
		
		@Override
		public void close() throws IOException {
			if (!closed) {
				closeInput(sock);
				closed = true;
			}
			super.close();
		}
		
		@Override
		protected void finalize() throws Throwable {
			close();
			super.finalize();
		}
		
		@Override
		public int read() throws IOException {
			byte[] buffer = new byte[1];
			int size = recv(sock, buffer, 0, 1);
			if (size == 0) {
				return -1;
			}
			return buffer[0];
		}
		
		@Override
		public int read(byte[] buffer) throws IOException {
			int fullSize = buffer.length;
			int index = 0;
			int size;
			do {
				size = recv(sock, buffer, index, Math.min(fullSize, Socket.BUFFER_SIZE));
				fullSize -= size;
				index += size;
			} while (fullSize != 0 && size != 0);
			return index;
		}

		
	}
	
	private class SocketOutputStream extends OutputStream {

		private boolean closed = false;
		
		@Override
		public void close() throws IOException {
			if (!closed) {
				closeOutput(sock);
				closed = true;
			}
			super.close();
		}
		
		@Override
		protected void finalize() throws Throwable {
			close();
			super.finalize();
		}
		
		@Override
		public void write(int c) throws IOException {
			byte[] res = new byte[1];
			res[0] = (byte)c;
			send(sock, res, 0, 1);
		}
		
		@Override
		public void write(byte[] buffer) throws IOException {
			int fullSize = buffer.length;
			int index = 0;
			int size;
			do {
				size = Math.min(fullSize, Socket.BUFFER_SIZE);
				send(sock, buffer, index, size);
				fullSize -= size;
				index += size;
			} while (fullSize != 0 && size != 0);
		}

	}

	private long sock;
	private InputStream inputStream;
	private OutputStream outputStream;
	
	private InetSocketAddress localAddress;
	private InetSocketAddress remoteAddress;
	
	/**
	 * This function fills in the local address field (if it is empty) using
	 * data from the native socket API
	 * @throws IOException
	 */
	private void retrieveLocalAddress() throws IOException {
		if (localAddress == null) {
			int gotLocalAddress = getLocalAddress(sock);
			int gotLocalPort = getLocalPort(sock);
			localAddress = new InetSocketAddress(new InetAddress(gotLocalAddress), gotLocalPort);
		}
	}
	
	/**
	 * Creates a new empty unconnected socket. Use <code>connect</code> to connnect it.
	 * <p><em>The signature of this method differs from JDK. In JDK this constructor
	 * doesn't raise any exceptions, but there are possible errors on a socket creation
	 * which it's undesirable to suppress. And as soon as the other constructors use {@link IOException}
	 * to handle such situations, here we raise it too.</em></p>
	 * @throws IOException in the case of a socket creation error
	 */
	public Socket() throws IOException {
		Socket.init();
		sock = create();
		inputStream = new SocketInputStream();
		outputStream = new SocketOutputStream();
	}
	
	/**
	 * Creates a socket and connects it to the specified host and port
	 * @param host the target host name to connect to. If it's <code>null</code>, the loopback would be used
	 * @param port the target port
	 * @throws UnknownHostException if the IP address of the selected host can't be determined
	 * @throws IOException in the case of a socket creation error
	 * @throws IllegalArgumentException if port number is outside [0..65535]
	 */
	public Socket(String host, int port) throws UnknownHostException, IOException {
		this(InetAddress.getByName(host), port);
	}
	
	/**
	 * Creates a socket and connects it to the specified address and port
	 * @param address the target address to connect to
	 * @param port the target port
	 * @throws IOException in the case of a socket creation error
	 * @throws IllegalArgumentException if port number is outside [0..65535]
	 * @throws NullPointerException if address is null
	 */
	public Socket(InetAddress address, int port) throws IOException {
		this();
		if (address == null) throw new NullPointerException("address can't be null");
		if (port < 0 || port > 65535) {
			throw new IllegalArgumentException("port should be between 0 and 65535");
		}
		
		// No explicit binding, just connect
		connect(sock, address.getRawAddress(), (short)port);
		remoteAddress = new InetSocketAddress(address, port);
		
		// ...and get implicit binding data 
		retrieveLocalAddress();
	}
	
	/**
	 * Creates a socket and connects it to the specified host and port. Also binds it to the selected local address and
	 * local port. If zero local port selected, the OS would find the proper port by itself.
	 * @param host the target host name to connect to. If it's <code>null</code>, the loopback would be used
	 * @param port the target port
	 * @throws UnknownHostException if the IP address of the selected host can't be determined
	 * @throws IOException in the case of a socket creation error
	 * @throws IllegalArgumentException if port number is outside [0..65535]
	 */
	public Socket(String host, int port, InetAddress localAddr, int localPort) throws UnknownHostException, IOException {
		this();
		if (port < 0 || port > 65535) {
			throw new IllegalArgumentException("port should be between 0 and 65535");
		}
		if (localPort < 0 || localPort > 65535) {
			throw new IllegalArgumentException("localPort should be between 0 and 65535");
		}

		// First bind
		bind(new InetSocketAddress(localAddr, localPort));
		localAddress = new InetSocketAddress(localAddr, localPort);
		
		// ...then connect
		InetAddress address = InetAddress.getByName(host);
		connect(sock,  address.getRawAddress(), (short)port);
		remoteAddress = new InetSocketAddress(address, port);

	}

	/**
	 * Creates a socket and connects it to the specified host and port. Also binds it to the selected local address and
	 * local port. If zero local port selected, the OS would find the proper port by itself. If local address is null,
	 * the OS will select any proper local address for the socket  
	 * @param host the target host name to connect to. If it's <code>null</code>, the loopback would be used
	 * @param port the target port
	 * @throws UnknownHostException if the IP address of the selected host can't be determined
	 * @throws IOException in the case of a socket creation error
	 * @throws NullPointerException if the <code>address</code> is <code>null</code>
	 * @throws IllegalArgumentException if <code>port</code> is outside <code>[0..65535]</code>
	 */
	public Socket(InetAddress address, int port, InetAddress localAddr, int localPort) throws IOException {
		this();
		if (address == null) throw new NullPointerException("address can't be null");
		if (port < 0 || port > 65535) {
			throw new IllegalArgumentException("port should be between 0 and 65535");
		}
		if (localPort < 0 || localPort > 65535) {
			throw new IllegalArgumentException("localPort should be between 0 and 65535");
		}

		// First bind
		bind(new InetSocketAddress(localAddr, localPort));
		localAddress = new InetSocketAddress(localAddr, localPort);
		
		// ...then connect
		connect(sock, address.getRawAddress(), (short)port);
		remoteAddress = new InetSocketAddress(address, port);
	}
	
	/**
	 * Connects the socket to a remote address
	 * @param endpoint the server address
	 * @throws IOException in the case of a connection problem
	 * @throws IllegalArgumentException if endpoint is null or it's a {@link SocketAddress} 
	 * subclass that's not supported by this socket
	 */
	public void connect(SocketAddress endpoint) throws IOException {
		if (endpoint == null) {
			throw new IllegalArgumentException("endpoint can't be null");
		}
		
		if (endpoint instanceof InetSocketAddress) {
	
			// First connect
			InetSocketAddress inetSocketAddress = (InetSocketAddress)endpoint;
			connect(sock, inetSocketAddress.getAddress().getRawAddress(), (short)(inetSocketAddress.getPort()));
			remoteAddress = inetSocketAddress;
	
			// ...then get binding data (if no explicit binding is done before)
			retrieveLocalAddress();
		} else {
			throw new IllegalArgumentException("only InetSocketAddress supported so far");
		}
	}
	
	/**
	 * Connects the socket to the server. If it can't connect during the specified timeout,
	 * an exception is thrown
	 * @param endpoint the server address
	 * @param timeout in milliseconds. <code>timeout</code> of zero means no timeout at all
	 * @throws IOException in the case of a connection problem
	 * @throws SocketTimeoutException if the socket can't connect before <code>timeout</code> expires. 
	 * @throws IllegalArgumentException if endpoint is <code>null</code> or it's a {@link SocketAddress} 
	 * subclass that's not supported by this socket. Or if <code>timeout < 0</code>
	 */
	public void connect(SocketAddress endpoint, int timeout) throws IOException, SocketTimeoutException {
		if (endpoint == null) {
			throw new IllegalArgumentException("endpoint can't be null");
		}
		
		if (timeout < 0) {
			throw new IllegalArgumentException("timeout should be 0 or positive");
		}
		
		if (endpoint instanceof InetSocketAddress) {
			// First connect
			InetSocketAddress inetSocketAddress = (InetSocketAddress)endpoint;
			connectTimeout(sock, inetSocketAddress.getAddress().getRawAddress(), (short)(inetSocketAddress.getPort()), timeout);
			remoteAddress = inetSocketAddress;
			
			// ...then get binding data (if no explicit binding is done before)
			retrieveLocalAddress();

		} else {
			throw new IllegalArgumentException("only InetSocketAddress supported so far");
		}
		
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
	
	/**
	 * Gets the remote address of the socket.
	 * <p>If this socket was closed after it was connected, this function continues to return 
	 * the address to which it was connected</p>
	 * @return the address to which this socket is connected
	 */
	public InetAddress getInetAddress() {
		if (remoteAddress != null) {
			return remoteAddress.getAddress();
		} else {
			return null;
		}
	}

	/**
	 * Gets the local address of the socket (the address to which this socket is bound)
	 * <p>If this socket was closed after it was bound, this function continues to return 
	 * the address to which it was bound</p>
	 * <p><i>There is a difference between Avian and JDK classpath here. In JDK for an unbound socket you get
	 * a "wildcard" address which points to nothing (or anything). In our implementation in this case you 
	 * get a <code>null</code> value instead. This decision is made cause a wildcard address is absolutely
	 * useless for the user.</i></p>
	 * @return the address to which this socket is bound or <code>null</code> if it isn't bound yet
	 */
	public InetAddress getLocalAddress() {
		if (localAddress != null) {
			return localAddress.getAddress();
		} else {
			return null;
		}
	}
	
	/**
	 * Gets the remote port of the socket
	 * <p>If this socket was closed after it was connected, this function continues to return 
	 * the port to which it was connected</p>
	 * @return the port to which this socket is connected, or 0 if it's not connected yet
	 */
	public int getPort() {
		if (remoteAddress != null) {
			return remoteAddress.getPort();
		} else {
			return 0;
		}
	}
	
	/**
	 * Gets the local port of the socket (the port to which this socket is bound)
	 * <p>If this socket was closed after it was bound, this function continues to return 
	 * the port to which it was bound</p>
	 * @return the port to which this socket is bound, or -1 if it's not bound yet
	 */
	public int getLocalPort() {
		if (localAddress != null) {
			return localAddress.getPort();
		} else {
			return -1;
		}
	}
	
	/**
	 * Gets the full remote address (with port)
	 * @return the remote address or <code>null</code> if the socket hasn't been connected
	 */
	public SocketAddress getRemoteSocketAddress() {
		return remoteAddress;
	}
	
	/**
	 * Gets the full local address (with port)
	 * @return the local address or <code>null</code> if the socket hasn't been bound
	 */
	public SocketAddress getLocalSocketAddress() {
		return localAddress;
	}

	/**
	 * Gets the input stream of the socket. Closing this stream means closing the socket input. 
	 * @return the input stream of the socket.
	 */
	public InputStream getInputStream() {
		return inputStream;
	}
	
	/**
	 * Gets the output stream of the socket. Closing this stream means closing the socket output. 
	 * @return the output stream of the socket.
	 */
	public OutputStream getOutputStream() {
		return outputStream;
	}

	public void setTcpNoDelay(boolean on) throws SocketException {}

	@Override
	public void close() throws IOException {
		close(sock);
	}
	
	public void shutdownInput() throws IOException {
		inputStream.close();
	}
	
	public void shutdownOutput() throws IOException {
		outputStream.close();
	}
	
	
	@Override
	protected void finalize() throws Throwable {
		close();
		super.finalize();
	}
}
