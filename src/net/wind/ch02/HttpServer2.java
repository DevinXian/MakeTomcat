package net.wind.ch02;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpServer2 {
	/**
	 * WEB_ROOT变量指明存放静态文件的目录，在本地文件系统中为<br/>
	 * 当前java目录下的webroot
	 */
	private static final String SHUTDOWN_COMMAND = "/SHUTDOWN";
	private boolean shutdown = false;

	public static void main(String[] args) {
		HttpServer1 server = new HttpServer1();
		server.await();
	}

	public void await() {
		ServerSocket serverSocket = null;
		int port = 8080;
		try {
			serverSocket = new ServerSocket(port, 1,
					InetAddress.getByName("127.0.0.1"));
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		while (!shutdown) {
			Socket socket = null;
			InputStream input = null;
			OutputStream output = null;
			try {
				socket = serverSocket.accept();
				input = socket.getInputStream();
				output = socket.getOutputStream();

				// 创建Request对象并解析
				Request request = new Request(input);
				request.parse();

				// 创建Response对象，输出响应内容
				Response response = new Response(output);
				response.setRequest(request);
				// 判断是静态文件请求还是servlet（servlet请求路由不同）
				// servlet路由格式：/servlet/servletName（servletName是servlet类名）
				if (request.getUri().startsWith("/servlet/")) {
					ServletProcessor2 processor = new ServletProcessor2();
					processor.process(request, response);
				} else {
					StaticResourceProcessor processor = new StaticResourceProcessor();
					processor.process(request, response);
				}
				// 关闭socket套接字
				socket.close();
				// 检查是否关闭命令
				shutdown = SHUTDOWN_COMMAND.equals(request.getUri());
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	}

}
