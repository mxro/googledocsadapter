/**
 * @author Max Rohde
 *
 */
 
import com.google.gdata.client.docs.DocsService;
import com.google.gdata.data.docs.DocumentListEntry;
import com.google.gdata.data.docs.DocumentListFeed;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.ServiceException;

import de.mxro.gdocs.GoogleDocsAdapter;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * This is a test template
 */

  public class Documents {
    
    public static void main(String[] args) {
      
    	GoogleDocsAdapter adapter = new GoogleDocsAdapter("mxrogm@gmail.com", "neverforgetlove");
    	
    	List<DocumentListEntry> entries = adapter.getDocumentList();
        for(int i=0; i<entries.size(); i++) {
          DocumentListEntry entry = entries.get(i);
         
          System.out.println("\t" + entry.getTitle().getPlainText());
        }
        System.out.println("\nTotal Entries: "+entries.size());
    	
      try {
        
        // Create a new Documents service
        DocsService myService = new DocsService("My Application");
        myService.setUserCredentials("mxrogm@googlemail.com","neverforgetlove");
        
        // Get a list of all entries
        URL metafeedUrl = new URL("http://docs.google.com/feeds/default/private/full");
        System.out.println("Getting Documents entries...\n");
        DocumentListFeed resultFeed = myService.getFeed(metafeedUrl, DocumentListFeed.class);
        
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
    }
  }
