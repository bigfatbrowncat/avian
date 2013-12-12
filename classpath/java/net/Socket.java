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
	
	private SocketImpl socketImpl = new DefaultSocketImpl();

	private InetSocketAddress localAddress;
	private InetSocketAddress remoteAddress;

	/**
	 * Creates a new empty unconnected socket. Use <code>connect</code> to connnect it.
	 * <p><em>The signature of this method differs from JDK. In JDK this constructor
	 * doesn't raise any exceptions, but there are possible errors on a socket creation
	 * which it's undesirable to suppress. And as soon as the other constructors use {@link IOException}
	 * to handle such situations, here we raise it too.</em></p>
	 * @throws IOException in the case of a socket creation error
	 */
	public Socket() throws IOException {
		socketImpl = new DefaultSocketImpl();
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
		socketImpl = new DefaultSocketImpl(host, port);
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
		socketImpl = new DefaultSocketImpl(address, port);
		remoteAddress = new InetSocketAddress(address, port);
		if (socketImpl instanceof DefaultSocketImpl) {
			if (localAddress == null) 
				localAddress = ((DefaultSocketImpl)socketImpl).getLocalAddress();
		}
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
		socketImpl = new DefaultSocketImpl(host, port, localAddr, localPort);
		InetAddress address = InetAddress.getByName(host);
		localAddress = new InetSocketAddress(localAddr, localPort);
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
		socketImpl = new DefaultSocketImpl(address, port, localAddr, localPort);
		localAddress = new InetSocketAddress(localAddr, localPort);
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
		socketImpl.connect(((InetSocketAddress)endpoint).getAddress(), ((InetSocketAddress)endpoint).getPort());
		if (socketImpl instanceof DefaultSocketImpl) {
			InetSocketAddress inetSocketAddress = (InetSocketAddress)endpoint;
			remoteAddress = inetSocketAddress;
			if (localAddress == null) 
				localAddress = ((DefaultSocketImpl)socketImpl).getLocalAddress();
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
		socketImpl.connect(endpoint, timeout);
		if (socketImpl instanceof DefaultSocketImpl) {
			InetSocketAddress inetSocketAddress = (InetSocketAddress)endpoint;
			remoteAddress = inetSocketAddress;
			if (localAddress == null) 
				localAddress = ((DefaultSocketImpl)socketImpl).getLocalAddress();
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
		
		socketImpl.bind(((InetSocketAddress)bindpoint).getAddress(), ((InetSocketAddress)bindpoint).getPort());

		if (bindpoint instanceof InetSocketAddress) {
			InetSocketAddress inetBindpoint = (InetSocketAddress)bindpoint;
			localAddress = inetBindpoint;
		}
	}
	
	/**
	 * Gets the remote address of the socket.
	 * <p>If this socket was closed after it was connected, this function continues to return 
	 * the address to which it was connected</p>
	 * @return the address to which this socket is connected
	 */
	public InetAddress getInetAddress() {
		return socketImpl.getInetAddress();
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
		return localAddress.getAddress();
	}
	
	/**
	 * Gets the remote port of the socket
	 * <p>If this socket was closed after it was connected, this function continues to return 
	 * the port to which it was connected</p>
	 * @return the port to which this socket is connected, or 0 if it's not connected yet
	 */
	public int getPort() {
		return socketImpl.getPort();
	}
	
	/**
	 * Gets the local port of the socket (the port to which this socket is bound)
	 * <p>If this socket was closed after it was bound, this function continues to return 
	 * the port to which it was bound</p>
	 * @return the port to which this socket is bound, or -1 if it's not bound yet
	 */
	public int getLocalPort() {
		return socketImpl.getLocalPort();
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
		return new InetSocketAddress(socketImpl.getInetAddress(), socketImpl.getLocalPort());
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

	public void setTcpNoDelay(boolean on) throws SocketException {}

	@Override
	public void close() throws IOException {
		socketImpl.close();
	}
	
	public void shutdownInput() throws IOException {
		socketImpl.shutdownInput();
	}
	
	public void shutdownOutput() throws IOException {
		socketImpl.shutdownOutput();
	}
	
	
	@Override
	protected void finalize() throws Throwable {
		close();
		super.finalize();
	}
}
