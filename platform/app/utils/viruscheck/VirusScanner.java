package utils.viruscheck;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import utils.AccessLog;
import utils.ErrorReporter;
import utils.exceptions.InternalServerException;

public class VirusScanner {

	protected static final byte[] INSTREAM = "zINSTREAM\0".getBytes();
	protected static final Pattern FOUND = Pattern.compile("^stream: (.+) FOUND$");
	protected static final String OK = "stream: OK";
	protected static final int CHUNK = 4096;

	protected static final byte[] PING = "zPING\0".getBytes();
	protected static final String PONG = "PONG";

	protected static final byte[] VERSION = "zVERSION\0".getBytes();

	protected static final int defaultTimeout = 1000 * 20;
	protected static final int defaultPort = 3310;
	protected static final String defaultHost = "127.0.0.1";

	private SocketAddress address;
	private int timeout;

	public VirusScanner(InetSocketAddress address, int timeout) {
		this.address = address;
		this.timeout = timeout;
	}
	
	public VirusScanner() {
		this.address = new InetSocketAddress(defaultHost, defaultPort);
		this.timeout = defaultTimeout;
	}

	public String scan(InputStream inputStream) throws InternalServerException {
		try {
		  return scan(inputStream, this.address, this.timeout);
		} catch (IOException e) {			
			ErrorReporter.report("VirusScanner", null, e);
			AccessLog.logException("Virus Scanner IO", e);
			throw new InternalServerException("error.internal", "IO Problem while scanning for virus");
		}
	}

	public boolean ping() {
		return ping(this.address, this.timeout);
	}

	public static boolean ping(SocketAddress address, int timeout) {
		try (SocketChannel socketChannel = SocketChannel.open(address)) {
			socketChannel.write((ByteBuffer) ByteBuffer.wrap(PING));

			socketChannel.socket().setSoTimeout(timeout);

			ByteBuffer data = ByteBuffer.allocate(1024);
			socketChannel.read(data);
			String status = new String(data.array());
			status = status.substring(0, status.indexOf(0));
			if (PONG.equals(status)) {
				return true;
			}
		} catch (IOException ex) {
			AccessLog.logException("VirusScanner IO", ex);
		}
		return false;
	}
	

	public static String scan(InputStream inputStream, SocketAddress address, int timeout) throws IOException, InternalServerException {
		try (SocketChannel socketChannel = SocketChannel.open(address)) {
			socketChannel.write((ByteBuffer) ByteBuffer.wrap(INSTREAM));
			ByteBuffer size = ByteBuffer.allocate(4);
			byte[] b = new byte[CHUNK];
			int chunk = CHUNK;
			while (chunk != -1) {
				chunk = inputStream.read(b);				
				if (chunk > 0) {
					size.clear();
					size.putInt(chunk).flip();
					socketChannel.write(size);
					socketChannel.write(ByteBuffer.wrap(b, 0, chunk));
				}
			}
			size.clear();
			size.putInt(0).flip();
			socketChannel.write(size);

			return scanResult(socketChannel, timeout);
		} finally {
			inputStream.close();
		}
	}

	private static String scanResult(SocketChannel socketChannel, int timeout) throws IOException, InternalServerException {
		socketChannel.socket().setSoTimeout(timeout);
		ByteBuffer data = ByteBuffer.allocate(1024);
		socketChannel.read(data);
		String status = new String(data.array());
		status = status.substring(0, status.indexOf(0));
		AccessLog.log("Virus Scanner status="+status);
		Matcher matcher = FOUND.matcher(status);
		if (matcher.matches()) {
			return matcher.group(1);
		} else if (OK.equals(status)) {
			return null;
		}
		throw new InternalServerException("error.internal", status);
	}

}