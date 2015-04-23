package net.wind.ch03.connector.http;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import javax.servlet.ServletException;

import net.wind.ch02.StaticResourceProcessor;

import org.apache.catalina.connector.http.SocketInputStream;

@SuppressWarnings("deprecation")
public class HttpProcessor {
	private HttpRequestLine requestLine = new HttpRequestLine();

	public void process(Socket socket) {
		SocketInputStream input = null;
		OutputStream output = null;
		try {
			input = new SocketInputStream(socket.getInputStream(), 2048);
			output = socket.getOutputStream();
			// 创建HttpRequest并解析请求
			request = new HttpRequest(input);
			// 创建HttpResponse
			response = new HttpResponse(output);
			response.setRequest(request);
			response.setHeader("Server", "My Servlet Container");

			praseResult(input, output);
			parseHeader(input);

			// 区别对待是静态资源请求还是Servlet请求
			if (request.getRequestUri().startsWith("/servlet/")) {
				ServletProcessor processor = new ServletProcessor();
				processor.process(request, response);
			} else {
				StaticResourceProcessor processor = new StaticResourceProcessor();
				processor.process(request, response);
			}

			// 关闭socket连接
			socket.close();
			// 应用不用关闭
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void parseRequest(SocketInputStream input, OutputStream output) throws IOException, ServletException{
		// 解析输入的request行
		input.readRequestLine(requestLine);
		String method = new String(requestLine.method, 0, requestLine.methodEnd);
		String uri = null;
		String protocol = new String(requestLine.protocol, 0, requestLine.protocolEnd);
	}
}
