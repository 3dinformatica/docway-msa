package it.tredi.msa.mailboxmanager;

import java.io.ByteArrayOutputStream;

import javax.mail.Part;

public class PartContentProvider implements ContentProvider {
	
	private Part part;
	
	public PartContentProvider(Part part) {
		this.part = part;
	}

	@Override
	public byte[] getContent() throws Exception {
        Object content = part.getContent();
        if (content instanceof java.io.InputStream) {
        	int count = 0;
        	ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] b = new byte[64 * 1024];
            while ((count = ((java.io.InputStream)content).read(b)) > 0)
                outputStream.write(b, 0, count);
            return outputStream.toByteArray();
        }
        else
            return content.toString().getBytes();
	}

}
