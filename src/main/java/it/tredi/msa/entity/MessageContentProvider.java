package it.tredi.msa.entity;

import java.io.ByteArrayOutputStream;
import java.util.Enumeration;

import javax.mail.Header;
import javax.mail.Message;

import it.tredi.mail.MessageUtils;

public class MessageContentProvider implements ContentProvider {
	
	private Message message;
	private boolean addHeaders;

	public MessageContentProvider(Message message, boolean addHeaders) {
		this.message = message;
		this.addHeaders = addHeaders;
	}

	@Override
	public byte[] getContent() throws Exception {
		if (addHeaders) {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			Enumeration<?> e = message.getAllHeaders();
			while (e.hasMoreElements()) {
				Header h = (Header)e.nextElement();
				String terminator = e.hasMoreElements()?"\n":"\n\n";
				outputStream.write(new String(h.getName() +": " + h.getValue() + terminator).getBytes("US-ASCII"));
			}
        	message.writeTo(outputStream);
        	return outputStream.toByteArray();
		}
		else
			return MessageUtils.getEML(message);
	}

}
