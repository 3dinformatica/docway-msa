package it.tredi.msa.entity;

public class StringContentProvider implements ContentProvider {
	
	private String content;

	public StringContentProvider(String content) {
		this.content = content;
	}

	@Override
	public byte[] getContent() throws Exception {
		return content.getBytes();
	}
	
}
