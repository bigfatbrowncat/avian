package java.net;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class SocketImpl {
	protected InetAddress address;
	protected FileDescriptor fd;
	protected int localport;
	protected int port;
	
	protected abstract void accept(SocketImpl s) throws IOException;
	protected abstract void bind(InetAddress host, int port) throws IOException;
	protected abstract void close() throws IOException;
	protected abstract void connect(InetAddress address, int port) throws IOException;
	protected abstract void connect(SocketAddress address, int timeout) throws IOException;
	protected abstract void connect(String host, int port) throws IOException;
	protected abstract void create(boolean stream) throws IOException;

	protected abstract InputStream getInputStream();
	protected abstract OutputStream getOutputStream();
	
	protected abstract void listen(int backlog);
	protected abstract void sendUrgentData(int data) throws IOException;

	
	protected void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
		throw new RuntimeException("Not implemented yet");
	}
	protected void shutdownInput() throws IOException {
		getInputStream().close();
	}
	protected void shutdownOutput() throws IOException {
		getOutputStream().close();
	}
	protected boolean supportsUrgentData() {
		return false;
	}

	protected FileDescriptor getFileDescriptor() {
		return fd;
	}
	protected InetAddress getInetAddress() {
		return address;
	}
	protected int getPort() {
		return port;
	}
	protected int getLocalPort() {
		return localport;
	}
	
}
