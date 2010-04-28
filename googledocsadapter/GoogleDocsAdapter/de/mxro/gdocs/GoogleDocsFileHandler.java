package de.mxro.gdocs;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gdata.data.docs.DocumentListEntry;
import com.google.gdata.util.ServiceException;

import de.mxro.FileHandler;
import de.mxro.Utils;
import de.mxro.string.filter.Filter;

public class GoogleDocsFileHandler implements FileHandler {

	private final GoogleDocsAdapter adapter;
		
	public String uploadFile(File file) {
		try {
			DocumentListEntry entry = adapter.uploadFile(file.getAbsolutePath(), file.getName());
            try {

			
			String html = adapter.downloadDocument("document:"+entry.getDocId(), "html");
			if (html == null) return null;
			// <img src=dgbrkvpg_103g8w4g7hc_b.jpg><br></body>
			
			String imageName = Filter.regExBetween("<img src=", "><br></body>", Filter.identity).perform(html);
			String imageWithoutExtension = de.mxro.Utils.removeExtension(imageName);
			String imageURL = "http://docs.google.com/File?id="+imageWithoutExtension;
			return imageURL;
            } finally {
			  entry.delete();
            }
			
			
		} catch (IOException e) {
			
			e.printStackTrace();
		} catch (ServiceException e) {
			
			e.printStackTrace();
		}
		return null;
	}

	public GoogleDocsFileHandler(GoogleDocsAdapter adapter) {
		super();
		this.adapter = adapter;
	}

	public boolean canHandle(File file) {
		String extension = Utils.getExtension(file.getAbsolutePath());
		return extension.equals("png") ||
		       extension.equals("jpg") ||
		       extension.equals("jpeg") ||
		       extension.equals("gif");
	}

}
