package nl.siegmann.epublib.epub;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import nl.siegmann.epublib.Constants;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.MediaType;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.TOCReference;
import nl.siegmann.epublib.domain.TableOfContents;
import nl.siegmann.epublib.service.MediatypeService;
import nl.siegmann.epublib.util.ResourceUtil;
import nl.siegmann.epublib.util.StringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

/**
 * Try to parse XHTML toc resources
 * 
 * @author matti@pehrs.com
 * 
 */
public class XHTMLTocParser {

	private static final Logger log = LoggerFactory
			.getLogger(XHTMLTocParser.class);

	/**
	 * Try to parse EPubs that have the TOC in XHTML
	 * 
	 * @param book
	 */
	static void processTocResource(Book book) {
		Resource tocResource = book.getSpine().getTocResource();
		log.debug("TOC Resource=" + tocResource);
		if (tocResource == null) {
			return;
		}
		String href = tocResource.getHref();
		MediaType mediaType = tocResource.getMediaType();
		if (MediatypeService.XHTML.equals(mediaType)) {
			try {
				// Parse the xhtml TOC
				Document tocDoc = ResourceUtil.getAsDocument(tocResource);
				Element root = tocDoc.getDocumentElement();
				Element nav = DOMUtil.getFirstElementByTagNameNS(root,
						"http://www.idpf.org/2007/ops", "nav");
				if (nav == null) {
					// A lot of them do not use the qualified name <epub:nav>
					// but only '<nav>'
					// Try to get the first <nav
					NodeList navs = tocDoc.getElementsByTagName("nav");
					if (navs != null && navs.getLength() > 0) {
						nav = (Element) navs.item(0);
					}
				}
				if (nav != null) {
					// Process the nav structure...
					NodeList ols = nav.getElementsByTagName("ol");
					if (ols != null && ols.getLength() > 0) {
						Element topOl = (Element) ols.item(0);
						parseXhtmlToc(book, topOl);
					}
				}
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			} catch (SAXException e) {
				log.error(e.getMessage(), e);
			} catch (ParserConfigurationException e) {
				log.error(e.getMessage(), e);
			}
		}
	}

	/**
	 * Parse the top
	 * <OL>
	 * element in the XHTML
	 * 
	 * @param topOl
	 */
	private static void parseXhtmlToc(Book book, Element topOl) {

		NodeList childs = topOl.getChildNodes();
		List<TOCReference> tocRefs = new ArrayList<TOCReference>();
		for (int c = 0; c < childs.getLength(); c++) {
			Node child = childs.item(c);
			if ("li".equals(child.getNodeName())) {
				log.debug("OL: found li element");
				Element li = (Element) child;
				NodeList liChilds = li.getChildNodes();
				readTOCReferences(book, liChilds, tocRefs);
			}
		}
		TableOfContents tableOfContents = new TableOfContents(tocRefs);
		book.setTableOfContents(tableOfContents);
	}

	private static void readTOCReferences(Book book, NodeList liChilds,
			List<TOCReference> tocRefs) {
		for (int lc = 0; lc < liChilds.getLength(); lc++) {
			Node liChild = liChilds.item(lc);
			// Find the "a" tag
			if ("a".equals(liChild.getNodeName())) {
				Element a = (Element) liChild;
				String reference = a.getAttribute("href");
				log.debug("Chapter HREF: " + reference);
				String title = DOMUtil.getTextChildrenContent(a);
				if (title == null || title.length() == 0) {
					// Parse for a "toc-label" title
					if (liChild.hasChildNodes()) {
						NodeList liChildren = liChild.getChildNodes();
						for (int lci = 0; lci < liChildren.getLength(); lci++) {
							Node liChildChild = liChildren.item(lci);
							NamedNodeMap attrs = liChildChild.getAttributes();
							if (attrs != null) {
								Node classAttr = liChildChild.getAttributes()
										.getNamedItem("class");
								if (classAttr != null) {
									if(classAttr.getNodeValue().equals("toc-label")) {
										 title = liChildChild.getTextContent();
									}
								}
							}
						}
						if (title == null || title.length() == 0) {
							// Try just the first child
							Node liChildChild = liChild.getChildNodes().item(0);
							switch (liChildChild.getNodeType()) {
							case Node.TEXT_NODE:
								title = ((Text) liChildChild).getData().trim();
								break;
							case Node.ELEMENT_NODE:
								title = DOMUtil
										.getTextChildrenContent((Element) liChildChild);
								break;
							default:
							}

						}
					}

				}
				if (title == null || title.length() == 0) {
					// Then do deep copy of whatever text is in there
					title = DOMUtil.getDeepTextChildrenContent(a);
				}
				log.debug("Chapter Title: " + title);

				String href = StringUtil.substringBefore(reference,
						Constants.FRAGMENT_SEPARATOR_CHAR);
				String fragmentId = StringUtil.substringAfter(reference,
						Constants.FRAGMENT_SEPARATOR_CHAR);

				// Find the resource
				Resource resource = book.getResources().getByHref(href);
				if (resource == null) {
					log.error("Resource with href " + href
							+ " in XHTML TOC document not found");
				}

				log.debug("new TOCReference: " + title + ", resource="
						+ resource);
				TOCReference tocReference = new TOCReference(title, resource,
						fragmentId);
				tocReference.setTitle(title);
				tocRefs.add(tocReference);
			}
		}
	}
}
