/* Copyright (c) 2008-2013, Avian Contributors

   Permission to use, copy, modify, and/or distribute this software
   for any purpose with or without fee is hereby granted, provided
   that the above copyright notice and this permission notice appear
   in all copies.

   There is NO WARRANTY for this software.  See license.txt for
   details. */

/*
 * This file implements a simple cross-platform JNI sockets API
 * It is used from different classes of the default Avian classpath
 */

#include "sockets.h"

#ifndef PLATFORM_WINDOWS
#	include <arpa/inet.h>
#endif

namespace avian {
namespace classpath {
namespace sockets {

#define NO_TIMEOUT			-1

int last_socket_error() {
#ifdef PLATFORM_WINDOWS
		int error = WSAGetLastError();
#else
		int error = errno;
#endif
		return error;
}


void init(JNIEnv* ONLY_ON_WINDOWS(e)) {
#ifdef PLATFORM_WINDOWS
  static bool wsaInitialized = false;
  if (not wsaInitialized) {
	WSADATA data;
	int r = WSAStartup(MAKEWORD(2, 2), &data);
	if (r or LOBYTE(data.wVersion) != 2 or HIBYTE(data.wVersion) != 2) {
	  throwNew(e, "java/io/IOException", "WSAStartup failed");
	} else {
	  wsaInitialized = true;
	}
  }
#endif
}

SOCKET create(JNIEnv* e) {
	SOCKET sock;
	if (INVALID_SOCKET == (sock = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP))) {
		char buf[255];
		sprintf(buf, "Can't create the socket. System error: %d", last_socket_error());
		throwNew(e, "java/io/IOException", buf);
		return 0;	// This doesn't matter cause we have risen an exception
	}
	return sock;
}

bool connect(JNIEnv* e, SOCKET sock, uint32_t addr, uint16_t port) {
	sockaddr_in adr;
	memset(&adr, 0, sizeof(sockaddr_in));

	adr.sin_family = AF_INET;
#ifdef PLATFORM_WINDOWS
	adr.sin_addr.S_un.S_addr = htonl(addr);
#else
	adr.sin_addr.s_addr = htonl(addr);
#endif
	adr.sin_port = htons (port);

	if (SOCKET_ERROR == ::connect(sock, (sockaddr* )&adr, sizeof(adr)))
	{
		char buf[255];
#ifdef PLATFORM_WINDOWS
		char* address_text = inet_ntoa(adr.sin_addr);
#else
		char address_text[INET_ADDRSTRLEN];
		inet_ntop(AF_INET, &(adr.sin_addr), address_text, INET_ADDRSTRLEN);
#endif
		sprintf(buf, "Can't connect the socket to address %s:%d. System error: %d", address_text, port, last_socket_error());
		throwNew(e, "java/net/SocketException", buf);
		return false;
	}
	return true;
}

bool connect(JNIEnv* e, SOCKET sock, uint32_t addr, uint16_t port, int timeout) {

	if (timeout > 0) {
		// Setting the socket to non-blocking mode
		u_long mode = 1;	// Non-blocking mode
		int res = ioctlsocket(sock, FIONBIO, &mode);
		if (NO_ERROR != res) {
			char buf[255];
			sprintf(buf, "Can't change the socket to non-blocking mode. System error: %d", last_socket_error());
			throwNew(e, "java/io/IOException", buf);
			return false;
		}
	}

	// Initiating connection
	if (!connect(e, sock, addr, port)) {
		return false;
	}

	if (timeout > 0) {
		// Setting the socket to blocking mode again
		u_long mode = 0;
		int res = ioctlsocket(sock, FIONBIO, &mode);
		if (NO_ERROR != res) {
			char buf[255];
			sprintf(buf, "Can't change the socket to blocking mode. System error: %d", last_socket_error());
			throwNew(e, "java/io/IOException", buf);
			return false;
		}

		// Waiting for the socket to connect (we will check if we can write to the socket)
		timeval timeVal;
		timeVal.tv_sec = timeout / 1000;
		int usec = (timeout % 1000) * 1000;
		timeVal.tv_usec = usec;

		fd_set writefds;
		FD_ZERO(&writefds);
		FD_SET(sock, &writefds);
		if (SOCKET_ERROR == select(sock + 1, NULL, &writefds, NULL, &timeVal)) {
			char buf[255];
			sprintf(buf, "Can't wait for the socket to be writable. System error: %d", last_socket_error());
			throwNew(e, "java/io/IOException", buf);
			return false;
		}

		if (FD_ISSET(sock, &writefds)) {
			return true;
		} else {
			char buf[255];
			sprintf(buf, "Connection timeout. System error: %d", last_socket_error());
			throwNew(e, "java/net/SocketTimeoutException", buf);
			return false;
		}
	}

	return true;
}

void bind(JNIEnv* e, SOCKET sock, uint32_t addr, uint16_t port) {
	sockaddr_in adr;
	memset(&adr, 0, sizeof(sockaddr_in));

	adr.sin_family = AF_INET;
#ifdef PLATFORM_WINDOWS
	adr.sin_addr.S_un.S_addr = htonl(addr);
#else
	adr.sin_addr.s_addr = htonl(addr);
#endif
	adr.sin_port = htons (port);

	if (SOCKET_ERROR == ::bind(sock, (sockaddr* )&adr, sizeof(sockaddr)))
	{
		char buf[255];
#ifdef PLATFORM_WINDOWS
		char* address_text = inet_ntoa(adr.sin_addr);
#else
		char address_text[INET_ADDRSTRLEN];
		inet_ntop(AF_INET, &(adr.sin_addr), address_text, INET_ADDRSTRLEN);
#endif

		sprintf(buf, "Can't bind the socket to address %s:%d. System error: %d", address_text, port, last_socket_error());
		throwNew(e, "java/net/BindException", buf);
		return;
	}
}


uint32_t getLocalAddress(JNIEnv* e, SOCKET sock) {
	sockaddr_in adr;
	socklen_t adrlen = sizeof(adr);
	if (SOCKET_ERROR == getsockname(sock, (sockaddr* )&adr, &adrlen)) {
		char buf[255];
		sprintf(buf, "Can't get the socket local address. System error: %d", last_socket_error());
		throwNew(e, "java/io/IOException", buf);
		return 0;
	}
#ifdef PLATFORM_WINDOWS
	return ntohl(adr.sin_addr.S_un.S_addr);
#else
	return ntohl(adr.sin_addr.s_addr);
#endif
}

uint16_t getLocalPort(JNIEnv* e, SOCKET sock) {
	sockaddr_in adr;
	socklen_t adrlen = sizeof(adr);
	if (SOCKET_ERROR == getsockname(sock, (sockaddr* )&adr, &adrlen)) {
		char buf[255];
		sprintf(buf, "Can't get the socket local port. System error: %d", last_socket_error());
		throwNew(e, "java/io/IOException", buf);
		return 0;
	}
	return ntohs(adr.sin_port);
}

uint32_t getRemoteAddress(JNIEnv* e, SOCKET sock) {
	sockaddr_in adr;
	socklen_t adrlen = sizeof(adr);
	if (SOCKET_ERROR == getpeername(sock, (sockaddr* )&adr, &adrlen)) {
		char buf[255];
		sprintf(buf, "Can't get the socket remote address. System error: %d", last_socket_error());
		throwNew(e, "java/io/IOException", buf);
		return 0;
	}
#ifdef PLATFORM_WINDOWS
	return ntohl(adr.sin_addr.S_un.S_addr);
#else
	return ntohl(adr.sin_addr.s_addr);
#endif
}

uint16_t getRemotePort(JNIEnv* e, SOCKET sock) {
	sockaddr_in adr;
	socklen_t adrlen = sizeof(adr);
	if (SOCKET_ERROR == getpeername(sock, (sockaddr* )&adr, &adrlen)) {
		char buf[255];
		sprintf(buf, "Can't get the socket remote port. System error: %d", last_socket_error());
		throwNew(e, "java/io/IOException", buf);
		return 0;
	}
	return ntohs(adr.sin_port);
}


bool listen(JNIEnv* e, SOCKET sock, int backlog) {
	if (SOCKET_ERROR == ::listen(sock, backlog)) {
		char buf[255];
		sprintf(buf, "Can't set the socket to the listening state. System error: %d", last_socket_error());
		throwNew(e, "java/io/IOException", buf);
		return false;
	} else {
		return true;
	}
}

SOCKET accept(JNIEnv* e, SOCKET sock, uint32_t* client_addr, uint16_t* client_port) {
	sockaddr_in adr;
	socklen_t adrlen = sizeof(adr);
	memset(&adr, 0, sizeof(sockaddr_in));

	SOCKET client_socket = ::accept(sock, (sockaddr* )&adr, &adrlen);
	if (INVALID_SOCKET == client_socket) {
		char buf[255];
		sprintf(buf, "Can't accept the incoming connection. System error: %d", last_socket_error());
		throwNew(e, "java/io/IOException", buf);
		return INVALID_SOCKET;
	}

	if (client_addr != NULL) {
	#ifdef PLATFORM_WINDOWS
		*client_addr = ntohl(adr.sin_addr.S_un.S_addr);
	#else
		*client_addr = ntohl(adr.sin_addr.s_addr);
	#endif
	}

	if (client_port != NULL) {
		*client_port = ntohs (adr.sin_port);
	}

	return client_socket;
}

void send(JNIEnv* e, SOCKET sock, const char* buff_ptr, int buff_size) {
	if (SOCKET_ERROR == ::send(sock, buff_ptr, buff_size, 0)) {
		char buf[255];
		sprintf(buf, "Can't send data through the socket. System error: %d", last_socket_error());
		throwNew(e, "java/io/IOException", buf);
		return;
	}
}

int recv(JNIEnv* e, SOCKET sock, char* buff_ptr, int buff_size, bool peek) {
	int flag = peek ? MSG_PEEK : 0;

	int length = ::recv(sock, buff_ptr, buff_size, flag);
	if (SOCKET_ERROR == length) {
		char buf[255];
		sprintf(buf, "Can't receive data through the socket. System error: %d", last_socket_error());
		throwNew(e, "java/io/IOException", buf);
		return 0;	// This doesn't matter cause we have risen an exception
	}
	return length;
}

void close(JNIEnv* e, SOCKET sock) {
	if (SOCKET_ERROR == ::closesocket(sock)) {
		int errcode = last_socket_error();
		char buf[255];
		sprintf(buf, "Can't shutdown the socket. System error: %d", errcode);
		throwNew(e, "java/io/IOException", buf);
	}
}

void shutdown_input(JNIEnv* e, SOCKET sock) {
	if (SOCKET_ERROR == ::shutdown(sock, SD_RECEIVE)) {
		int errcode = last_socket_error();
		if (errcode != WSAENOTCONN) {
			char buf[255];
			sprintf(buf, "Can't shutdown the socket. System error: %d", errcode);
			throwNew(e, "java/io/IOException", buf);
		}
	}
}

void shutdown_output(JNIEnv* e, SOCKET sock) {
	if (SOCKET_ERROR == ::shutdown(sock, SD_SEND)) {
		int errcode = last_socket_error();
		if (errcode != WSAENOTCONN) {
			char buf[255];
			sprintf(buf, "Can't shutdown the socket. System error: %d", errcode);
			throwNew(e, "java/io/IOException", buf);
		}
	}
}

}
}
}
