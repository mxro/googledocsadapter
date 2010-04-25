package de.mxro.gdocs;

import static org.junit.Assert.*;

import org.junit.Test;

public class GoogleDocsFileHandlerTest {

	@Test
	public void testUploadFile() {
		String input = "<div align=left id=gxmd><img src=http://docs.google.com/File?id=dgbrkvpg_92c5qn83ds_b style=\"HEIGHT:124px; WIDTH:158px\"><br><br>";
		GoogleDocsFileHandler handler = new GoogleDocsFileHandler(new GoogleDocsAdapter("user", "pass"));
		String url = handler.uploadFile(new java.io.File("/Users/mx/Desktop/test.png"));
		System.out.println(url);
	}

}
