package nl.siegmann.epublib.epub;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;

import junit.framework.TestCase;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.service.MediatypeService;

import java.io.FileInputStream;
import java.io.IOException;

import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Guide;
import nl.siegmann.epublib.domain.GuideReference;
import nl.siegmann.epublib.domain.Metadata;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.Spine;
import nl.siegmann.epublib.domain.TOCReference;
import nl.siegmann.epublib.domain.TableOfContents;
import nl.siegmann.epublib.epub.EpubReader;

public class EpubReaderTest extends TestCase {
	
	public void testCover_only_cover() {
		try {
			Book book = new Book();
			
			book.setCoverImage(new Resource(this.getClass().getResourceAsStream("/book1/cover.png"), "cover.png"));

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			(new EpubWriter()).write(book, out);
			byte[] epubData = out.toByteArray();
			Book readBook = new EpubReader().readEpub(new ByteArrayInputStream(epubData));
			assertNotNull(readBook.getCoverImage());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assertTrue(false);
		}

	}

	public void testCover_cover_one_section() {
		try {
			Book book = new Book();
			
			book.setCoverImage(new Resource(this.getClass().getResourceAsStream("/book1/cover.png"), "cover.png"));
			book.addSection("Introduction", new Resource(this.getClass().getResourceAsStream("/book1/chapter1.html"), "chapter1.html"));
			book.generateSpineFromTableOfContents();
			
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			(new EpubWriter()).write(book, out);
			byte[] epubData = out.toByteArray();
			Book readBook = new EpubReader().readEpub(new ByteArrayInputStream(epubData));
			assertNotNull(readBook.getCoverPage());
			assertEquals(1, readBook.getSpine().size());
			assertEquals(1, readBook.getTableOfContents().size());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assertTrue(false);
		}
	}

	public void testReadEpub_opf_ncx_docs() {
		try {
			Book book = new Book();
			
			book.setCoverImage(new Resource(this.getClass().getResourceAsStream("/book1/cover.png"), "cover.png"));
			book.addSection("Introduction", new Resource(this.getClass().getResourceAsStream("/book1/chapter1.html"), "chapter1.html"));
			book.generateSpineFromTableOfContents();
			
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			(new EpubWriter()).write(book, out);
			byte[] epubData = out.toByteArray();
			Book readBook = new EpubReader().readEpub(new ByteArrayInputStream(epubData));
			assertNotNull(readBook.getCoverPage());
			assertEquals(1, readBook.getSpine().size());
			assertEquals(1, readBook.getTableOfContents().size());
			assertNotNull(readBook.getOpfResource());
			assertNotNull(readBook.getNcxResource());
			assertEquals(MediatypeService.NCX, readBook.getNcxResource().getMediaType());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assertTrue(false);
		}
	}

	public void testReadEpub_with_xhtml_toc() throws IOException {

		FileInputStream in = new FileInputStream("target/sample.epub");
		Book book = new EpubReader().readEpub(in);

		Metadata meta = book.getMetadata();
		System.out.println("firstTitle="+meta.getFirstTitle());
		System.out.println("titles="+meta.getTitles());
		System.out.println("authors="+meta.getAuthors());
		System.out.println("format="+meta.getFormat());
		System.out.println("descriptions="+meta.getDescriptions());
		System.out.println("language="+meta.getLanguage());
		System.out.println("contributors="+meta.getContributors());
		System.out.println("dates="+meta.getDates());
		System.out.println("identifiers="+meta.getIdentifiers());
		System.out.println("otherProperties="+meta.getOtherProperties());
		System.out.println("publishers="+meta.getPublishers());
		System.out.println("subjects="+meta.getSubjects());
		System.out.println("types="+meta.getTypes());
		
		// Navigate
		System.out.println("SPINE--------");
		Spine spine = book.getSpine();
		for(int i=0;i<spine.size();i++) {
			Resource res = spine.getResource(i);
			System.out.println("SPINE.resource["+i+"="+res);
		}
		
		System.out.println("TOC--------");
		TableOfContents toc = book.getTableOfContents();
		assertTrue("XHTML TOC Could not be parsed", toc.size() > 0 );
		for(TOCReference tocRef:toc.getTocReferences()) {
			System.out.println("TOC.ref.title="+tocRef.getTitle()+", res.href="+tocRef.getResource().getHref());
		}
		for(Resource res:toc.getAllUniqueResources()) {
			System.out.println("TOC.resource="+res);
		}
		
		System.out.println("GUIDE--------");
		Guide guide = book.getGuide();
		Resource coverPage = guide.getCoverPage();
		System.out.println("GUIDE.coverPage="+coverPage);
		GuideReference coverRef = guide.getCoverReference();
		System.out.println("GUIDE.coverReference="+coverRef);
		for(GuideReference ref:guide.getReferences()) {
			System.out.println("GUIDE.reference="+ref);
		}
	    
	}
}
