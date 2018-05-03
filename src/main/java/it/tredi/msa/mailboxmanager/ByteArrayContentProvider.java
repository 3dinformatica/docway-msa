package it.tredi.msa.mailboxmanager;

public class ByteArrayContentProvider implements ContentProvider {
	
	private byte []content;

	public ByteArrayContentProvider(byte []content) {
		this.content = content;
	}

	@Override
	public byte[] getContent() throws Exception {
		return content;
	}
	
}
