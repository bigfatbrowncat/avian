package java.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DefaultSocketImpl extends SocketImpl {

	private static final int BUFFER_SIZE = 65535;
	
	private class SocketInputStream extends InputStream {

		private boolean closed = false;
		private boolean eof = false;
		private byte[] testBuffer = new byte[BUFFER_SIZE]; 
		
		@Override
		public void close() throws IOException {
			if (!closed) {
				shutdownInput(sock);
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
				//System.out.println("zero");
				return -1;
			}
			return buffer[0];
		}
		
		@Override
		public int read(byte[] buffer, int offset, int length) throws IOException {
			//Thread.currentThread().getStackTrace().
			System.out.print("> reading buffer ");
			int index = offset;
			int size;
			do {
				size = recv(sock, buffer, index, Math.min(length, BUFFER_SIZE));
				length -= size;
				index += size;
			} while (length != 0 && size != 0);
			System.out.print(length == 0 ? " length out " : " size out ");
			System.out.println("size is " + size);

			if (size == 0) {
				eof = true;
			}
			
			return index;
		}

		@Override
		public int available() throws IOException {
			return (int)DefaultSocketImpl.available(sock);
		}
	}
	private class SocketOutputStream extends OutputStream {

		private boolean closed = false;
		
		@Override
		public void close() throws IOException {
			if (!closed) {
				shutdownOutput(sock);
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
			//System.err.print((char)res[0]);
			send(sock, res, 0, 1);
		}
		
		/*@Override
		public void write(byte[] buffer, int offset, int length) throws IOException {
			System.out.println("> writing " + buffer.length + " bytes");
			System.out.flush();
			int fullSize = buffer.length;
			int index = offset;
			int size;
			do {
				size = Math.min(fullSize, BUFFER_SIZE);
				send(sock, buffer, index, size);
				fullSize -= size;
				index += size;
			} while (fullSize != 0 && size != 0);
		}*/
	}

	
	/**
	 * This method is called from all routines that depend on winsock in windows,
	 * so it has public visibility
	 * @throws IOException
	 */
	public static native void init() throws IOException;
	private static boolean initialized = false;
	
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

	private static native void listenNative(/* SOCKET */long sock, int backlog);
	private static native /* SOCKET */long accept(/* SOCKET */long sock);
	
	private static native int getLocalAddress(/* SOCKET */long sock) throws IOException;
	private static native int getLocalPort(/* SOCKET */long sock) throws IOException;
	private static native int getRemoteAddress(/* SOCKET */long sock) throws IOException;
	private static native int getRemotePort(/* SOCKET */long sock) throws IOException;

	
	private static native void send(/* SOCKET */long sock, byte[] buffer, int start_pos, int count) throws IOException;
	private static native int recv(/* SOCKET */long sock, byte[] buffer, int start_pos, int count) throws IOException;
	private static native long available(/* SOCKET */long sock) throws IOException;
	
	private static native void close(/* SOCKET */long sock);
	private static native void shutdownOutput(/* SOCKET */long sock);
	private static native void shutdownInput(/* SOCKET */long sock);

	private long sock;
	
	private InputStream inputStream;
	private OutputStream outputStream;
	
	private InetSocketAddress localAddress;
	
	protected InetSocketAddress getLocalAddress() {
		return localAddress;
	}
	
	protected DefaultSocketImpl() throws IOException {
		if (!initialized) {
			init();
			initialized = true;
		}
		
		sock = create();
		inputStream = new SocketInputStream();
		outputStream = new SocketOutputStream();
	}
	
	public DefaultSocketImpl(String host, int port) throws UnknownHostException, IOException {
		this(InetAddress.getByName(host), port);
	}

	public DefaultSocketImpl(InetAddress address, int port) throws IOException {
		this();
		if (address == null) throw new NullPointerException("address can't be null");
		if (port < 0 || port > 65535) {
			throw new IllegalArgumentException("port should be between 0 and 65535");
		}
		
		// No explicit binding, just connect
		connect(sock, address.getRawAddress(), (short)port);
		this.address = address;
		this.port = port;
		
		// ...and get implicit binding data 
		retrieveLocalAddress();
	}
	
	public DefaultSocketImpl(String host, int port, InetAddress localAddr, int localPort) throws UnknownHostException, IOException {
		this();
		
		if (port < 0 || port > 65535) {
			throw new IllegalArgumentException("port should be between 0 and 65535");
		}
		if (localPort < 0 || localPort > 65535) {
			throw new IllegalArgumentException("localPort should be between 0 and 65535");
		}

		// First bind
		bind(new InetSocketAddress(localAddr, localPort));
		
		// ...then connect
		address = InetAddress.getByName(host);
		this.port = port;
		connect(sock,  address.getRawAddress(), (short)port);
	}
	
	public DefaultSocketImpl(InetAddress address, int port, InetAddress localAddr, int localPort) throws IOException {
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
		retrieveLocalAddress();
		localport = localPort;
		
		// ...then connect
		connect(sock, address.getRawAddress(), (short)port);
	}
	
	@Override
	protected void accept(SocketImpl s) throws IOException {
		DefaultSocketImpl dsa = (DefaultSocketImpl)s;
		dsa.sock = accept(sock);
	}

	private void bind(SocketAddress bindpoint) throws IOException, SocketException {

		if (bindpoint == null) {
			bindAny(sock);
			retrieveLocalAddress();
		} else if (bindpoint instanceof InetSocketAddress) {
			InetSocketAddress inetBindpoint = (InetSocketAddress)bindpoint;
			bind(sock, inetBindpoint.getAddress().getRawAddress(), (short) inetBindpoint.getPort());
			localport = inetBindpoint.getPort();
			localAddress = new InetSocketAddress(InetAddress.getByName(inetBindpoint.getHostName()), localport); 
		} else {
			throw new IllegalArgumentException("only InetSocketAddress supported so far");
		}
	}
	
	@Override
	protected void bind(InetAddress host, int port) throws IOException {
		bind(new InetSocketAddress(host, port));
		localAddress = new InetSocketAddress(host, port); 
	}

	@Override
	protected void close() throws IOException {
		// Shutting down input and output streams
		((SocketInputStream)inputStream).close();
		((SocketOutputStream)outputStream).close();

		// Closing the socket descriptor
		close(sock);
	}

	private void connect(SocketAddress endpoint) throws IOException {
		if (endpoint == null) {
			throw new IllegalArgumentException("endpoint can't be null");
		}
		
		if (endpoint instanceof InetSocketAddress) {
	
			// First connect
			InetSocketAddress inetSocketAddress = (InetSocketAddress)endpoint;
			connect(sock, inetSocketAddress.getAddress().getRawAddress(), (short)(inetSocketAddress.getPort()));
	
			// ...then get binding data (if no explicit binding is done before)
			retrieveLocalAddress();
		} else {
			throw new IllegalArgumentException("only InetSocketAddress supported so far");
		}
	}
	
	@Override
	protected void connect(InetAddress address, int port) throws IOException {
		connect(new InetSocketAddress(address, port));
		
	}

	@Override
	protected void connect(SocketAddress address, int timeout)
			throws IOException {
		if (address == null) {
			throw new IllegalArgumentException("endpoint can't be null");
		}
		
		if (timeout < 0) {
			throw new IllegalArgumentException("timeout should be 0 or positive");
		}
		
		if (address instanceof InetSocketAddress) {
			// First connect
			InetSocketAddress inetSocketAddress = (InetSocketAddress)address;
			connectTimeout(sock, inetSocketAddress.getAddress().getRawAddress(), (short)(inetSocketAddress.getPort()), timeout);
			
			// ...then get binding data (if no explicit binding is done before)
			//retrieveLocalAddress();

		} else {
			throw new IllegalArgumentException("only InetSocketAddress supported so far");
		}
		
	}

	@Override
	protected void connect(String host, int port) throws IOException {
		connect(new InetSocketAddress(host, port));
	}

	@Override
	protected void create(boolean stream) throws IOException {
		if (stream == true) {
			// Creating TCP socket
			//init();
			sock = create();
			inputStream = new SocketInputStream();
			outputStream = new SocketOutputStream();
		} else {
			throw new RuntimeException("datagram sockets aren't supported yet");
		}
		
	}

	@Override
	protected InputStream getInputStream() {
		return inputStream;
	}

	@Override
	protected OutputStream getOutputStream() {
		return outputStream;
	}

	@Override
	protected void listen(int backlog) {
		listenNative(sock, backlog);
	}
	
	
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
	
	@Override
	protected void sendUrgentData(int data) throws IOException {
		throw new RuntimeException("datagram sockets aren't supported yet");
	}
}
