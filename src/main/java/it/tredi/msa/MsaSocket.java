package it.tredi.msa;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MsaSocket {
	
	private int port;
	private ServerSocket msaSocket;
	
	private static final Logger logger = LogManager.getLogger(MsaSocket.class.getName());

	// Singleton
    private static MsaSocket instance = null;
    
    /**
     * Costruttore privato
     */
    private MsaSocket(int port) throws IOException {
    	this.port = port;
    	this.initSocket();
    }
	
    /**
     * Ritorna l'oggetto contenente il Socket del servizio MSA
	 * @return
	 */
	public static MsaSocket getInstance(int port) throws IOException {
		if (instance == null) {
			synchronized (MsaSocket.class) {
				if (instance == null) {
					if (logger.isInfoEnabled())
						logger.info("MsaSocket instance is null... create one");
					instance = new MsaSocket(port);
				}
			}
		}
		return instance;
	}
	
	/**
	 * Inizializzazione del socket di MSA
	 * @throws IOException
	 */
	private void initSocket() throws IOException {
		InetSocketAddress testSocketAddress = new InetSocketAddress("127.0.0.1", port);
		msaSocket = new ServerSocket();
		msaSocket.bind(testSocketAddress);
	}
	
}
