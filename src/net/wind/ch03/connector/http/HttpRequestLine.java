package net.wind.ch03.connector.http;

/**
 * 
 * @author netwind
 *
 */
final class HttpRequestLine {

	// Contants
	public static final int INITIAL_METHOD_SIZE = 8;
	public static final int INITIAL_URI_SIZE = 64;
	public static final int INITIAL_PROTOCOL_SIZE = 8;
	public static final int MAX_METHOD_SIZE = 1024;
	public static final int MAX_URI_SIZE = 32768;
	public static final int MAX_PROTOCOL_SIZE = 1024;

	public char[] method;
	public int methodEnd;
	public char[] uri;
	public int uriEnd;
	public char[] protocol;
	public int protocolEnd;

	public HttpRequestLine(char[] method, int methodEnd, char[] uri,
			int uriEnd, char[] protocol, int protocolEnd) {
		this.method = method;
		this.methodEnd = methodEnd;
		this.uri = uri;
		this.uriEnd = uriEnd;
	}

	public HttpRequestLine() {
		this(new char[INITIAL_METHOD_SIZE], 0, new char[INITIAL_URI_SIZE], 0,
				new char[INITIAL_PROTOCOL_SIZE], 0);
	}

	public void recycle() {
		methodEnd = 0;
		uriEnd = 0;
		protocolEnd = 0;
	}

	/**
	 * 判断uri是否包含给定字符数组
	 * 
	 * @param buf
	 * @return
	 */
	public int indexOf(char[] buf) {
		return indexOf(buf, buf.length);
	}

	/**
	 * 
	 * @param buf
	 * @param end
	 *            或许更应该命名为length
	 * @return
	 */
	public int indexOf(char[] buf, int end) {
		char firstChar = buf[0];
		int pos = 0;
		while (pos < uriEnd) {
			pos = indexOf(firstChar, pos);
			if (pos == -1) {// 首字符不在，返回
				return -1;
			}
			if ((uriEnd - pos) < end) {// 首字符在，但是不够buf长度
				return -1;
			}
			for (int i = 0; i <= end - 1; i++) {
				if (uri[pos + i] != buf[i]) {
					break;
				}
				if (i == (end - 1)) {// 基准条件
					return pos;
				}
			}
			pos++;// 下一个位置继续向后搜索
		}
		return -1;
	}

	// uri是否包含指定字符串
	public int indexOf(String str) {
		return indexOf(str.toCharArray(), str.length());
	}

	/**
	 * 字符index
	 * 
	 * @param c
	 * @param start
	 * @return
	 */
	public int indexOf(char c, int start) {
		for (int i = start; i < uriEnd; i++) {
			if (uri[i] == c) {
				return i;
			}
		}
		return -1;
	}

	public int hasCode() {
		// without implements
		return 0;
	}

	public boolean equals(Object obj) {
		// without implements
		return false;
	}
}
