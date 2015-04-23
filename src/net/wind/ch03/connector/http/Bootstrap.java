package net.wind.ch03.connector.http;

public final class Bootstrap {

	public static void main(String[] args) {
		HttpConnector connector = new HttpConnector();
		connector.start();
	}

}
