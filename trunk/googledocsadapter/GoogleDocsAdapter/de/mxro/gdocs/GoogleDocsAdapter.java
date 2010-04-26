package de.mxro.gdocs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import com.google.gdata.client.DocumentQuery;
import com.google.gdata.client.docs.DocsService;
import com.google.gdata.data.MediaContent;
import com.google.gdata.data.PlainTextConstruct;
import com.google.gdata.data.docs.DocumentListEntry;
import com.google.gdata.data.docs.DocumentListFeed;
import com.google.gdata.data.media.MediaByteArraySource;
import com.google.gdata.data.media.MediaSource;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.ServiceException;

import de.mxro.filesystem.v01.IncludedFile;
import de.mxro.string.filter.Filter;

public class GoogleDocsAdapter {

	final DocsService client;
	final String username;
	final String password;
	

	public DocumentListEntry uploadFile(String filepath, String title)
	throws IOException, ServiceException  {
		File file = new File(filepath);
		String mimeType = DocumentListEntry.MediaType.fromFileName(file.getName()).getMimeType();

		DocumentListEntry newDocument = new DocumentListEntry();
		newDocument.setFile(file, mimeType);
		newDocument.setTitle(new PlainTextConstruct(title));

		// Prevent collaborators from sharing the document with others?
		// newDocument.setWritersCanInvite(false);

		return client.insert(new URL("https://docs.google.com/feeds/default/private/full/"), newDocument);
	}

	public String prepareHTMLDocumentBeforeUpload(String html) {
		return Filter.regExReplace(" style=", ".png style=",
				Filter.regExReplace("<A NAME='tempreplace' /><br>", ".png><br>", 
				Filter.regExReplace("<A NAME='tempreplace' /><img src=http://docs.google.com/File?id=", "<img src=", Filter.identity))).perform(html);


	}

	public String prepareHTMLDocumentAfterDownload(String html) {
		String onlyBody = Filter.onlyBody(
				Filter.regExReplace(".png style=", " style=", 
						Filter.regExReplace("<img src=", "<A NAME='tempreplace' /><img src=http://docs.google.com/File?id=", 
								Filter.regExReplace(".png><br>", "<A NAME='tempreplace' /><br>", 
								Filter.identity)))).perform(html);
		/*System.out.println("===== ORIGINAL HTML =====");
		System.out.println(html);
		System.out.println("=====  =====");
		System.out.println("===== STRIPPED HTML =====");

		System.out.println(onlyBody);*/
		return onlyBody;
	}

	public DocumentListEntry updateDocument(DocumentListEntry entry, String newContent) {
		try {
			String mimeType = DocumentListEntry.MediaType.HTML.getMimeType();
			MediaSource source = new MediaByteArraySource(newContent.getBytes("UTF-8"), mimeType);

			entry.setMediaSource(source);
			entry.setWritersCanInvite(false);


			return entry.updateMedia(true);



		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();

		} catch (IOException e) {

			e.printStackTrace();
		} catch (ServiceException e) {

			e.printStackTrace();
		}
		return null;
	}

	private String downloadDocumentContent(String exportUrl)
	throws IOException, MalformedURLException, ServiceException {
		//System.out.println("Exporting document from: " + exportUrl);

		MediaContent mc = new MediaContent();
		mc.setUri(exportUrl);
		MediaSource ms = client.getMedia(mc);

		InputStream inStream = null;
		ByteArrayOutputStream outStream= null;


		try {
			inStream = ms.getInputStream();
			outStream = new ByteArrayOutputStream();

			int c;
			while ((c = inStream.read()) != -1) {
				outStream.write(c);
			}
		} finally {
			if (inStream != null) {
				inStream.close();
			}
			if (outStream != null) {
				outStream.flush();
				return de.mxro.Utils.fromInputStream(new ByteArrayInputStream(outStream.toByteArray()));
				//outStream.close();
			}
		}
		return null;
	}




	public String downloadDocument(String resourceId, String format)
	throws IOException, MalformedURLException, ServiceException {
		String docId = resourceId.substring(resourceId.lastIndexOf(":") + 1);


		String exportUrl = "https://docs.google.com/feeds/download/documents/Export?docId=" +
		docId + "&exportFormat=" + format;
		return downloadDocumentContent(exportUrl);
	}





	/**
	 * Returns list of Documents in Google Docs
	 * @return
	 */
	public List<DocumentListEntry> getDocumentList() {
		try {

			// Create a new Documents service


			// Get a list of all documents
			URL metafeedUrl = new URL("http://docs.google.com/feeds/default/private/documents");

			DocumentListFeed resultFeed = client.getFeed(metafeedUrl, DocumentListFeed.class);
			List<DocumentListEntry> entries = resultFeed.getEntries();

			return entries;
		}
		catch(AuthenticationException e) {
			e.printStackTrace();
		}
		catch(MalformedURLException e) {
			e.printStackTrace();
		}
		catch(ServiceException e) {
			e.printStackTrace();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public GoogleDocsAdapter(String username, String password) {
		super();
		this.username = username;
		this.password = password;
		client = new DocsService("GoogleDocumentsAdapter-0.1");
		try {
			client.setUserCredentials(username,password);
		} catch (AuthenticationException e) {

			e.printStackTrace();
		}
	}


}
