/* Copyright (c) 2008-2013, Avian Contributors

   Permission to use, copy, modify, and/or distribute this software
   for any purpose with or without fee is hereby granted, provided
   that the above copyright notice and this permission notice appear
   in all copies.

   There is NO WARRANTY for this software.  See license.txt for
   details. */

#include "jni.h"
#include "avian/machine.h"

#include "sockets.h"

using namespace avian::classpath::sockets;

extern "C" JNIEXPORT void JNICALL
Java_java_net_DefaultSocketImpl_init(JNIEnv* e, jclass) {
	init(e);
}

extern "C" JNIEXPORT SOCKET JNICALL
Java_java_net_DefaultSocketImpl_create(JNIEnv* e, jclass) {
	return create(e);
}

extern "C" JNIEXPORT void JNICALL
Java_java_net_DefaultSocketImpl_connect(JNIEnv* e, jclass, SOCKET sock, long addr, short port) {
	connect(e, sock, addr, port);
}

extern "C" JNIEXPORT void JNICALL
Java_java_net_DefaultSocketImpl_connectTimeout(JNIEnv* e, jclass, SOCKET sock, long addr, short port, int timeout) {
	connect(e, sock, addr, port, timeout);
}

extern "C" JNIEXPORT void JNICALL
Java_java_net_DefaultSocketImpl_bind(JNIEnv* e, jclass, SOCKET sock, long addr, short port) {
	bind(e, sock, addr, port);
}

extern "C" JNIEXPORT void JNICALL
Java_java_net_DefaultSocketImpl_listenNative(JNIEnv* e, jclass, SOCKET sock, int backlog) {
	listen(e, sock, backlog);
}

extern "C" JNIEXPORT SOCKET JNICALL
Java_java_net_DefaultSocketImpl_accept(JNIEnv* e, jclass, SOCKET sock) {

	uint32_t client_addr;
	uint16_t client_port;
	return accept(e, sock, &client_addr, &client_port);
}

extern "C" JNIEXPORT void JNICALL
Java_java_net_DefaultSocketImpl_bindAny(JNIEnv* e, jclass, SOCKET sock) {
	bind(e, sock, INADDR_ANY, 0);
}

extern "C" JNIEXPORT jint JNICALL
Java_java_net_DefaultSocketImpl_getLocalAddress(JNIEnv* e, jclass, SOCKET sock) {
	return getLocalAddress(e, sock);
}

extern "C" JNIEXPORT jint JNICALL
Java_java_net_DefaultSocketImpl_getLocalPort(JNIEnv* e, jclass, SOCKET sock) {
	return getLocalPort(e, sock);
}

extern "C" JNIEXPORT jint JNICALL
Java_java_net_DefaultSocketImpl_getRemoteAddress(JNIEnv* e, jclass, SOCKET sock) {
	return getRemoteAddress(e, sock);
}

extern "C" JNIEXPORT jint JNICALL
Java_java_net_DefaultSocketImpl_getRemotePort(JNIEnv* e, jclass, SOCKET sock) {
	return getRemotePort(e, sock);
}

extern "C" JNIEXPORT void JNICALL
Java_java_net_DefaultSocketImpl_close(JNIEnv* e, jclass, SOCKET sock) {
	close(e, sock);
}

extern "C" JNIEXPORT void JNICALL
Java_java_net_DefaultSocketImpl_shutdownOutput(JNIEnv* e, jclass, SOCKET sock) {
	shutdown_output(e, sock);
}

extern "C" JNIEXPORT void JNICALL
Java_java_net_DefaultSocketImpl_shutdownInput(JNIEnv* e, jclass, SOCKET sock) {
	shutdown_input(e, sock);
}

extern "C" JNIEXPORT void JNICALL
Avian_java_net_DefaultSocketImpl_send(vm::Thread* t, vm::object, uintptr_t* arguments) {		/* SOCKET s, object buffer_obj, int start_pos, int count  */
	SOCKET& s = *(reinterpret_cast<SOCKET*>(&arguments[0]));
	vm::object buffer_obj = reinterpret_cast<vm::object>(arguments[2]);
	int32_t& start_pos = *(reinterpret_cast<int32_t*>(&arguments[3]));
	int32_t& count = *(reinterpret_cast<int32_t*>(&arguments[4]));
	char* buffer = reinterpret_cast<char*>(&vm::byteArrayBody(t, buffer_obj, start_pos));
	avian::classpath::sockets::send((JNIEnv*)t, s, buffer, count);
}

extern "C" JNIEXPORT int64_t JNICALL
Avian_java_net_DefaultSocketImpl_recv(vm::Thread* t, vm::object, uintptr_t* arguments) {		/* SOCKET s, object buffer_obj, int start_pos, int count  */
	SOCKET& s = *(reinterpret_cast<SOCKET*>(&arguments[0]));
	vm::object buffer_obj = reinterpret_cast<vm::object>(arguments[2]);
	int32_t& start_pos = *(reinterpret_cast<int32_t*>(&arguments[3]));
	int32_t& count = *(reinterpret_cast<int32_t*>(&arguments[4]));
	char* buffer = reinterpret_cast<char*>(&vm::byteArrayBody(t, buffer_obj, start_pos));
	return avian::classpath::sockets::recv((JNIEnv*)t, s, buffer, count, false);
}

extern "C" JNIEXPORT int64_t JNICALL
Avian_java_net_DefaultSocketImpl_available(vm::Thread* t, vm::object, uintptr_t* arguments) {		/* SOCKET s, object buffer_obj, int start_pos, int count  */
	SOCKET& s = *(reinterpret_cast<SOCKET*>(&arguments[0]));
	vm::object buffer_obj = reinterpret_cast<vm::object>(arguments[2]);
	int32_t& start_pos = *(reinterpret_cast<int32_t*>(&arguments[3]));
	int32_t& count = *(reinterpret_cast<int32_t*>(&arguments[4]));
	char* buffer = reinterpret_cast<char*>(&vm::byteArrayBody(t, buffer_obj, start_pos));
	return avian::classpath::sockets::recv((JNIEnv*)t, s, buffer, count, true);
}


extern "C" JNIEXPORT jint JNICALL
Java_java_net_InetAddress_ipv4AddressForName(JNIEnv* e,
                                             jclass,
                                             jstring name)
{
  const char* chars = e->GetStringUTFChars(name, 0);
  if (chars) {
#ifdef PLATFORM_WINDOWS
    hostent* host = gethostbyname(chars);
    e->ReleaseStringUTFChars(name, chars);
    if (host) {
      return ntohl(reinterpret_cast<in_addr*>(host->h_addr_list[0])->s_addr);
    } else {
      fprintf(stderr, "trouble %d\n", WSAGetLastError());
    }
#else
    addrinfo hints;
    memset(&hints, 0, sizeof(addrinfo));
    hints.ai_family = AF_INET;
    hints.ai_socktype = SOCK_STREAM;

    addrinfo* result;
    int r = getaddrinfo(chars, 0, &hints, &result);
    e->ReleaseStringUTFChars(name, chars);

    jint address;
    if (r != 0) {
      address = 0;
    } else {
      address = ntohl
        (reinterpret_cast<sockaddr_in*>(result->ai_addr)->sin_addr.s_addr);

      freeaddrinfo(result);
    }

    return address;
#endif
  }
  return 0;
}

