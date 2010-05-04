package de.mxro.gdocs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import com.google.gdata.client.docs.DocsService;
import com.google.gdata.data.MediaContent;
import com.google.gdata.data.PlainTextConstruct;
import com.google.gdata.data.docs.DocumentEntry;
import com.google.gdata.data.docs.DocumentListEntry;
import com.google.gdata.data.docs.DocumentListFeed;
import com.google.gdata.data.docs.PresentationEntry;
import com.google.gdata.data.docs.SpreadsheetEntry;
import com.google.gdata.data.media.MediaByteArraySource;
import com.google.gdata.data.media.MediaSource;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.ServiceException;

import de.mxro.string.filter.Filter;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GoogleDocsAdapter {

    final DocsService client;
    final String username;
    final String password;

    public DocumentListEntry uploadFile(String filepath, String title)
            throws IOException, ServiceException {
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

    public DocumentListEntry getDocumentEntry(List<DocumentListEntry> list, String documentID) {

        for (DocumentListEntry e : list) {
            if (e.getDocId().equals(documentID)) {
                return e;
            }
        }
        return null;
    }

    public DocumentListEntry updateDocument(DocumentListEntry entry, String newContent) throws UnsupportedEncodingException, ServiceException, IOException {
       
            String mimeType = DocumentListEntry.MediaType.HTML.getMimeType();
            MediaSource source = new MediaByteArraySource(newContent.getBytes("UTF-8"), mimeType);

            entry.setMediaSource(source);
            entry.setWritersCanInvite(false);
            entry.setEtag("*");

            return entry.updateMedia(true);



    }

    private String downloadDocumentContent(String exportUrl)
            throws IOException, MalformedURLException, ServiceException {
        //System.out.println("Exporting document from: " + exportUrl);

        MediaContent mc = new MediaContent();
        mc.setUri(exportUrl);
        MediaSource ms;
        try {
            ms = client.getMedia(mc);


        } catch (com.google.gdata.util.ResourceNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        InputStream inStream = null;
        ByteArrayOutputStream outStream = null;


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

        String kind = resourceId.substring(0, resourceId.lastIndexOf(":"));
        System.out.println(docId + ":" + kind);

        if (!kind.equals("document")) {
            return null;
        }

        String exportUrl = "https://docs.google.com/feeds/download/documents/Export?docId="
                + docId + "&exportFormat=" + format;
        return downloadDocumentContent(exportUrl);
    }

    public boolean trashDocument(DocumentListEntry entry) {
        try {
            client.delete(new URL(entry.getEditLink().getHref()), entry.getEtag());
            return true;
        } catch (IOException ex) {
            Logger.getLogger(GoogleDocsAdapter.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ServiceException ex) {
            Logger.getLogger(GoogleDocsAdapter.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    public LinkedList<DocumentListEntry> downloadDocumentsList() {
        try {

            // Create a new Documents service


            // Get a list of all documents
            URL metafeedUrl = new URL("http://docs.google.com/feeds/default/private/documents");

            DocumentListFeed resultFeed = client.getFeed(metafeedUrl, DocumentListFeed.class);
            List<DocumentListEntry> entries = resultFeed.getEntries();

            LinkedList<DocumentListEntry> list = new LinkedList<DocumentListEntry>();
            for (DocumentListEntry e : entries) {
                if (e.getType().equals("document")) {
                    list.add(e);
                }
            }

            return list;
        } catch (AuthenticationException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ServiceException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public DocumentListEntry createNewDocument(String title, String type)
            throws IOException, ServiceException {
        DocumentListEntry newEntry = null;
        if (type.equals("document")) {
            newEntry = new DocumentEntry();
        } else if (type.equals("presentation")) {
            newEntry = new PresentationEntry();
        } else if (type.equals("spreadsheet")) {
            newEntry = new SpreadsheetEntry();
        }
        newEntry.setTitle(new PlainTextConstruct(title));

        // Prevent collaborators from sharing the document with others?
        // newEntry.setWritersCanInvite(false);

        // You can also hide the document on creation
        // newEntry.setHidden(true);

        return client.insert(new URL("https://docs.google.com/feeds/default/private/full/"), newEntry);
    }

    public DocumentListEntry createNewDocument(DocumentListEntry entry)
            throws IOException, ServiceException {


        return client.insert(new URL("https://docs.google.com/feeds/default/private/full/"), entry);
    }
    private boolean connected;

    public boolean isConnected() {
        return connected;
    }

    public GoogleDocsAdapter(String username, String password) {
        super();
        this.username = username;
        this.password = password;
        client = new DocsService("GoogleDocumentsAdapter-0.1");
        try {
            client.setUserCredentials(username, password);
            connected = true;
        } catch (AuthenticationException e) {
            connected = false;
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException, ServiceException {
    }
}
