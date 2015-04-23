package net.wind.ch03.connector.http;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.catalina.util.StringManager;

public class SocketInputStream extends InputStream {

	/**
	 * 回车
	 */
	private static final byte CR = (byte) '\r';

	/**
	 * 换行
	 */
	private static final byte LF = (byte) '\n';

	/**
	 * 空格
	 */
	private static final byte SP = (byte) ' ';
	/**
	 * 水平制表符
	 */
	private static final byte HT = (byte) '\t';
	/**
	 * 冒号
	 */
	private static final byte COLON = (byte) ':';
	/**
	 * 大小写ASCII码差值
	 */
	private static final int LC_OFFSET = 'A' - 'a';
	/**
	 * 内部buf
	 */
	protected byte[] buf;
	/**
	 * 最后合法byte
	 */
	protected int count;
	/**
	 * buf中的位置
	 */
	protected int pos;
	/**
	 * 底层输入流
	 */
	protected InputStream is;

	public SocketInputStream(InputStream is, int bufferSize) {
		this.is = is;
		buf = new byte[bufferSize];
	}

	protected static StringManager sm = StringManager
			.getManager(Constants.Package);

	/**
	 * 读取HttpRequestLine对象并复制到buffer中<br/>
	 * 本方法用于Http请求头解析。不要试图用本来读取请求body
	 * 
	 * @param requestLine
	 *            HttpRequestLine对象
	 * @throws IOException
	 *             当读取输入流错误，或buffer不够容纳整行则抛出此异常
	 */
	public void readRequestLine(HttpRequestLine requestLine) throws IOException {
		// 声明周期检查重置
		if (requestLine.methodEnd != 0) {
			requestLine.recycle();
		}
		// 检查是否空行
		int chr = 0;
		do {
			try {
				chr = read();
			} catch (IOException e) {
				chr = -1;
			}
		} while ((chr == CR) || (chr == LF));// 跳过CR或LF
		if (chr == -1) {
			throw new EOFException(sm.getString("requestStream.readline.error"));
		}
		pos--;

		// 读取请求方法名
		int maxRead = requestLine.method.length;
		int readStart = pos;
		int readCount = 0;

		boolean space = false;
		while (!space) {
			// buffer满了，扩容
			if (readCount >= maxRead) {
				if ((2 * maxRead) < HttpRequestLine.MAX_METHOD_SIZE) {
					char[] newBuffer = new char[2 * maxRead];
					System.arraycopy(requestLine.method, 0, newBuffer, 0,
							maxRead);
					requestLine.method = newBuffer;
					maxRead = requestLine.method.length;
				}
			} else {
				throw new IOException(
						sm.getString("requestStream.readline.toolong"));
			}
			// 已到内部buffer最后
			if (pos >= count) {
				int val = read();
				if (val == -1) {
					throw new IOException(
							sm.getString("requestStream.readline.error"));
				}
				pos = 0;
				readStart = 0;
			}
			if (buf[pos] == SP) {
				space = true;
			}
			requestLine.method[readCount] = (char) buf[pos];
			readCount++;
			pos++;
		}
		requestLine.uriEnd = readCount - 1;
		// 读取URI
		maxRead = requestLine.uri.length;
		readStart = pos;
		readCount = 0;

		space = false;
		boolean eol = false;

		while (!eol) {
			// buff满了，扩容
			if (readCount >= maxRead) {
				if ((2 * maxRead) <= HttpRequestLine.MAX_URI_SIZE) {
					char[] newBuffer = new char[2 * maxRead];
					System.arraycopy(requestLine.uri, 0, newBuffer, 0, maxRead);
					requestLine.uri = newBuffer;
					maxRead = requestLine.uri.length;
				} else {
					throw new IOException(
							sm.getString("requestStream.readline.toolong"));
				}
			}
			// buffer最后
			if (pos >= count) {
				int val = read();
				if (val == -1) {
					throw new IOException(
							sm.getString("requestStream.readline.error"));
				}
				pos = 0;
				readStart = 0;
			}
			if (buf[pos] == SP) {
				space = true;
			} else if (buf[pos] == CR || buf[pos] == LF) {
				// Http/0.9 style request
				eol = true;
				space = true;
			}
			requestLine.uri[readCount] = (char) buf[pos];
			readCount++;
			pos++;
		}
		requestLine.uriEnd = readCount - 1;

		// Reading protocol

		maxRead = requestLine.protocol.length;
		readStart = pos;
		readCount = 0;

		while (!eol) {
			// if the buffer is full, extend it
			if (readCount >= maxRead) {
				if ((2 * maxRead) <= HttpRequestLine.MAX_PROTOCOL_SIZE) {
					char[] newBuffer = new char[2 * maxRead];
					System.arraycopy(requestLine.protocol, 0, newBuffer, 0,
							maxRead);
					requestLine.protocol = newBuffer;
					maxRead = requestLine.protocol.length;
				} else {
					throw new IOException(
							sm.getString("requestStream.readline.toolong"));
				}
			}
			// We're at the end of the internal buffer
			if (pos >= count) {
				// Copying part (or all) of the internal buffer to the line
				// buffer
				int val = read();
				if (val == -1)
					throw new IOException(
							sm.getString("requestStream.readline.error"));
				pos = 0;
				readStart = 0;
			}
			if (buf[pos] == CR) {
				// Skip CR.
			} else if (buf[pos] == LF) {
				eol = true;
			} else {
				requestLine.protocol[readCount] = (char) buf[pos];
				readCount++;
			}
			pos++;
		}

		requestLine.protocolEnd = readCount;

	}

	/**
	 * Read a header, and copies it to the given buffer. This function is meant
	 * to be used during the HTTP request header parsing. Do NOT attempt to read
	 * the request body using it.
	 *
	 * @param requestLine
	 *            Request line object
	 * @throws IOException
	 *             If an exception occurs during the underlying socket read
	 *             operations, or if the given buffer is not big enough to
	 *             accomodate the whole line.
	 */
	public void readHeader(HttpHeader header) throws IOException {

		// Recycling check
		if (header.nameEnd != 0)
			header.recycle();

		// Checking for a blank line
		int chr = read();
		if ((chr == CR) || (chr == LF)) { // Skipping CR
			if (chr == CR)
				read(); // Skipping LF
			header.nameEnd = 0;
			header.valueEnd = 0;
			return;
		} else {
			pos--;
		}

		// Reading the header name

		int maxRead = header.name.length;
		int readStart = pos;
		int readCount = 0;

		boolean colon = false;

		while (!colon) {
			// if the buffer is full, extend it
			if (readCount >= maxRead) {
				if ((2 * maxRead) <= HttpHeader.MAX_NAME_SIZE) {
					char[] newBuffer = new char[2 * maxRead];
					System.arraycopy(header.name, 0, newBuffer, 0, maxRead);
					header.name = newBuffer;
					maxRead = header.name.length;
				} else {
					throw new IOException(
							sm.getString("requestStream.readline.toolong"));
				}
			}
			// We're at the end of the internal buffer
			if (pos >= count) {
				int val = read();
				if (val == -1) {
					throw new IOException(
							sm.getString("requestStream.readline.error"));
				}
				pos = 0;
				readStart = 0;
			}
			if (buf[pos] == COLON) {
				colon = true;
			}
			char val = (char) buf[pos];
			if ((val >= 'A') && (val <= 'Z')) {
				val = (char) (val - LC_OFFSET);
			}
			header.name[readCount] = val;
			readCount++;
			pos++;
		}

		header.nameEnd = readCount - 1;

		// Reading the header value (which can be spanned over multiple lines)

		maxRead = header.value.length;
		readStart = pos;
		readCount = 0;

		int crPos = -2;

		boolean eol = false;
		boolean validLine = true;

		while (validLine) {

			boolean space = true;

			// Skipping spaces
			// Note : Only leading white spaces are removed. Trailing white
			// spaces are not.
			while (space) {
				// We're at the end of the internal buffer
				if (pos >= count) {
					// Copying part (or all) of the internal buffer to the line
					// buffer
					int val = read();
					if (val == -1)
						throw new IOException(
								sm.getString("requestStream.readline.error"));
					pos = 0;
					readStart = 0;
				}
				if ((buf[pos] == SP) || (buf[pos] == HT)) {
					pos++;
				} else {
					space = false;
				}
			}

			while (!eol) {
				// if the buffer is full, extend it
				if (readCount >= maxRead) {
					if ((2 * maxRead) <= HttpHeader.MAX_VALUE_SIZE) {
						char[] newBuffer = new char[2 * maxRead];
						System.arraycopy(header.value, 0, newBuffer, 0, maxRead);
						header.value = newBuffer;
						maxRead = header.value.length;
					} else {
						throw new IOException(
								sm.getString("requestStream.readline.toolong"));
					}
				}
				// We're at the end of the internal buffer
				if (pos >= count) {
					// Copying part (or all) of the internal buffer to the line
					// buffer
					int val = read();
					if (val == -1)
						throw new IOException(
								sm.getString("requestStream.readline.error"));
					pos = 0;
					readStart = 0;
				}
				if (buf[pos] == CR) {
				} else if (buf[pos] == LF) {
					eol = true;
				} else {
					// FIXME : Check if binary conversion is working fine
					int ch = buf[pos] & 0xff;
					header.value[readCount] = (char) ch;
					readCount++;
				}
				pos++;
			}

			int nextChr = read();

			if ((nextChr != SP) && (nextChr != HT)) {
				pos--;
				validLine = false;
			} else {
				eol = false;
				// if the buffer is full, extend it
				if (readCount >= maxRead) {
					if ((2 * maxRead) <= HttpHeader.MAX_VALUE_SIZE) {
						char[] newBuffer = new char[2 * maxRead];
						System.arraycopy(header.value, 0, newBuffer, 0, maxRead);
						header.value = newBuffer;
						maxRead = header.value.length;
					} else {
						throw new IOException(
								sm.getString("requestStream.readline.toolong"));
					}
				}
				header.value[readCount] = ' ';
				readCount++;
			}

		}

		header.valueEnd = readCount;

	}

	/**
	 * Read byte.
	 */
	@Override
	public int read() throws IOException {
		if (pos >= count) {
			fill();
			if (pos >= count)
				return -1;
		}
		return buf[pos++] & 0xff;
	}

	/**
     *
     */
	/*
	 * public int read(byte b[], int off, int len) throws IOException {
	 * 
	 * }
	 */

	/**
     *
     */
	/*
	 * public long skip(long n) throws IOException {
	 * 
	 * }
	 */

	/**
	 * Returns the number of bytes that can be read from this input stream
	 * without blocking.
	 */
	public int available() throws IOException {
		return (count - pos) + is.available();
	}

	/**
	 * Close the input stream.
	 */
	public void close() throws IOException {
		if (is == null)
			return;
		is.close();
		is = null;
		buf = null;
	}

	// ------------------------------------------------------ Protected Methods

	/**
	 * Fill the internal buffer using data from the undelying input stream.
	 */
	protected void fill() throws IOException {
		pos = 0;
		count = 0;
		int nRead = is.read(buf, 0, buf.length);
		if (nRead > 0) {
			count = nRead;
		}
	}

}
