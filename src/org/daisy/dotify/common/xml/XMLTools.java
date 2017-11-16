package org.daisy.dotify.common.xml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.Attributes;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Provides some xml tools.
 * 
 * @author Joel Håkansson
 */
public class XMLTools {

	private static final byte[] USC_4_BE = new byte[]{0x00, 0x00, (byte)0xFE, (byte)0xFF};
	private static final byte[] USC_4_LE = new byte[]{(byte)0xFF, (byte)0xFE, 0x00, 0x00};
	private static final byte[] USC_4_2143 = new byte[]{0x00, 0x00, (byte)0xFF, (byte)0xFE};
	private static final byte[] USC_4_3412 = new byte[]{(byte)0xFE, (byte)0xFF, 0x00, 0x00};
	private static final byte[] UTF_8 = new byte[] {(byte)0x3C, (byte)0x3F, 0x78, 0x6D};
	static final Pattern XML_DECL = Pattern.compile("\\A\\s*<\\?xml.*?encoding\\s*=\\s*[\"'](?<ENCODING>[^'\"]*)[\"'].*\\?>");


	private XMLTools() {}
	

	/**
	 * Gets the declared encoding from the given string. If the string
	 * doesn't start with an XML declaration, an empty optional is returned.
	 * @param text the xml
	 * @return returns a string with the declared encoding
	 */
	public static Optional<String> getDeclaredEncoding(String text) {
		Matcher m = XML_DECL.matcher(text);
		String enc;
		if (m.find() && (enc=m.group("ENCODING"))!=null) {
			return Optional.of(enc);
		}
		return Optional.empty();
	}
	
	/**
	 * Detects XML encoding based on this algorithm: <a href="https://www.w3.org/TR/xml/#sec-guessing">https://www.w3.org/TR/xml/#sec-guessing</a>.
	 * 
	 * @param data the input bytes
	 * @return returns the detected charset
	 */
	public static String detectXmlEncoding(byte[] data) {
		Charset preliminary = guessEncoding(data); 
		String decl = new String(Arrays.copyOf(data, Math.min(4*100, data.length)), preliminary);
		Optional<String> specifiedEncoding = getDeclaredEncoding(decl);
		//FIXME: else return ...
		return specifiedEncoding.orElse(StandardCharsets.UTF_8.name());
	}
	
	private static Charset guessEncoding(byte[] data) {
		// Based on https://www.w3.org/TR/xml/#sec-guessing
		if (data.length<4) {
			throw new IllegalArgumentException();
		}
		byte[] magic = Arrays.copyOf(data, 4);
		// Find group of encodings that can be used to decode the declaration (if any)
		if (Arrays.equals(magic, USC_4_BE)) {
			//TODO: 
		} else if (Arrays.equals(magic, USC_4_LE)) {
			// Note that this test must come before UTF-16 below
			//TODO:
		} else if (Arrays.equals(magic, USC_4_2143)) {
			//TODO:
		} else if (Arrays.equals(magic, USC_4_3412)) {
			// Note that this test must come before UTF-16 below
			//TODO:
		} else if (magic[0]==(byte)0xFE && magic[1]==(byte)0xFF) {
			// UTF-16, big endian
			return StandardCharsets.UTF_16BE;
		} else if (magic[0]==(byte)0xFF && magic[1]==(byte)0xFE) {
			// UTF-16, little endian
			return StandardCharsets.UTF_16LE;
		} else if (magic[0]==(byte)0xEF && magic[1]==(byte)0xBB && magic[2]==(byte)0xBF) {
			System.out.println("UTF-8 with BOM");
			return StandardCharsets.UTF_8;
		} else if (Arrays.equals(magic, UTF_8)) {
			System.out.println("UTF-8 no BOM");
			return StandardCharsets.UTF_8;
		}
		System.out.println("Not detected");
		return StandardCharsets.UTF_8;
	}

	/**
	 * <p>Transforms the xml with the specified parameters. By default, this method will set up a caching entity resolver, which
	 * will reduce the amount of fetching of dtd's from the Internet.</p>
	 * 
	 * <p>This method will attempt to create Source and Result objects from the supplied source, result and xslt objects. 
	 * This process supports several types of objects from which Sources and Results are typically created, such as files, 
	 * strings and URLs.</p>
	 * 
	 * <p>This method will create its own instance of a transformer factory.</p>
	 * 
	 * @param source the source xml
	 * @param result the result xml
	 * @param xslt the xslt
	 * @param params xslt parameters
	 * @throws XMLToolsException if the transformation is unsuccessful
	 */
	public static void transform(Object source, Object result, Object xslt, Map<String, Object> params) throws XMLToolsException {
		transform(toSource(source), toResult(result), toSource(xslt), params);
	}
	
	/**
	 * <p>Transforms the xml with the specified parameters. By default, this method will set up a caching entity resolver, which
	 * will reduce the amount of fetching of dtd's from the Internet.</p>
	 * 
	 * <p>This method will attempt to create Source and Result objects from the supplied source, result and xslt objects. 
	 * This process supports several types of objects from which Sources and Results are typically created, such as files, 
	 * strings and URLs.</p>
	 * 
	 * @param source the source xml
	 * @param result the result xml
	 * @param xslt the xslt
	 * @param params xslt parameters
	 * @param factory the transformer factory
	 * @throws XMLToolsException if the transformation is unsuccessful
	 */
	public static void transform(Object source, Object result, Object xslt, Map<String, Object> params, TransformerFactory factory) throws XMLToolsException {
		transform(toSource(source), toResult(result), toSource(xslt), params, factory);
	}
	
	/**
	 * Transforms the xml with the specified parameters. By default, this method will set up a caching entity resolver, which
	 * will reduce the amount of fetching of dtd's from the Internet. 
	 * 
	 * <p>This method will create its own instance of a transformer factory.</p>
	 * @param source the source xml
	 * @param result the result xml
	 * @param xslt the xslt
	 * @param params xslt parameters
	 * @throws XMLToolsException if the transformation is unsuccessful
	 */
	public static void transform(Source source, Result result, Source xslt, Map<String, Object> params) throws XMLToolsException {
		transform(source, result, xslt, params, TransformerFactory.newInstance());
	}

	/**
	 * <p>Transforms the xml with the specified parameters. By default, this method will set up a caching entity resolver, which
	 * will reduce the amount of fetching of dtd's from the Internet.</p>
	 * @param source the source xml
	 * @param result the result xml
	 * @param xslt the xslt
	 * @param params xslt parameters
	 * @param factory the transformer factory
	 * @throws XMLToolsException if the transformation is unsuccessful
	 */
	public static void transform(Source source, Result result, Source xslt, Map<String, Object> params, TransformerFactory factory) throws XMLToolsException {
		Transformer transformer;
		try {
			transformer = factory.newTransformer(xslt);
		} catch (TransformerConfigurationException e) {
			throw new XMLToolsException(e);
		} catch (TransformerFactoryConfigurationError e) {
			throw new XMLToolsException(e);
		}

		for (String name : params.keySet()) {
			transformer.setParameter(name, params.get(name));
		}
		
		SAXParserFactory parserFactory = SAXParserFactory.newInstance();
		transformer.setURIResolver(new CachingURIResolver(parserFactory));
        //Create a SAXSource, hook up an entityresolver
        if(source.getSystemId()!=null && source.getSystemId().length()>0) {
        	try {
				if (source instanceof SAXSource) {
					transformer.transform(setEntityResolver((SAXSource) source), result);
				} else {
					SAXParser parser = parserFactory.newSAXParser();
					parser.getXMLReader().setFeature("http://xml.org/sax/features/validation", false);
					try (InputStream is = new URLCache().openStream(new URI(source.getSystemId()).toURL())) {
						InputSource isource = new InputSource(is);
						isource.setSystemId(source.getSystemId());
						SAXSource saxSource = new SAXSource(parser.getXMLReader(), isource);
						saxSource.setSystemId(source.getSystemId());
						transformer.transform(setEntityResolver(saxSource), result);
					}
				}
        	} catch (TransformerException e) {
    			throw new XMLToolsException(e);
    		} catch (Exception e) {
    			//TODO: really catch everything?
				e.printStackTrace();
			}
        }
	}
	
	private static SAXSource setEntityResolver(SAXSource source) {
		if(source.getXMLReader().getEntityResolver()==null) {
			source.getXMLReader().setEntityResolver(new EntityResolverCache());
		}
		return source;
	}

	private static Source toSource(Object source) throws XMLToolsException {
		if (source instanceof File) {
			return new StreamSource((File) source);
		} else if (source instanceof String) {
			return new StreamSource((String) source);
		} else if (source instanceof URL) {
			try {
				// Compare to {@link StreamSource#StreamSource(File)}
				return new StreamSource(((URL) source).toURI().toASCIIString());
			} catch (URISyntaxException e) {
				throw new XMLToolsException(e);
			}
		} else if (source instanceof URI) {
			return new StreamSource(((URI) source).toASCIIString());
		} else if (source instanceof Source) {
			return (Source) source;
		} else {
			throw new XMLToolsException("Failed to create source: " + source);
		}
	}

	private static Result toResult(Object result) throws XMLToolsException {
		if (result instanceof File) {
			return new StreamResult((File) result);
		} else if (result instanceof OutputStream) {
			return new StreamResult((OutputStream) result);
		} else if (result instanceof String) {
			return new StreamResult((String) result);
		} else if (result instanceof URL) {
			try {
				return new StreamResult(((URL) result).toURI().toASCIIString());
			} catch (URISyntaxException e) {
				throw new XMLToolsException(e);
			}
		} else if (result instanceof URI) {
			return new StreamResult(((URI) result).toASCIIString());
		} else if (result instanceof Result) {
			return (Result) result;
		} else {
			throw new XMLToolsException("Failed to create result: " + result);
		}
	}

	/**
	 * Returns true if the specified file is well formed XML.
	 * @param f the file
	 * @return returns true if the file is well formed XML, false otherwise
	 * @throws XMLToolsException if a parser cannot be configured or if parsing fails
	 */
	public static final boolean isWellformedXML(File f) throws XMLToolsException {
		return parseXML(f)!=null;
	}
	
	/**
	 * Returns true if the contents at the specified URI is well formed XML.
	 * @param uri the URI
	 * @return returns true if the file is well formed XML, false otherwise
	 * @throws XMLToolsException if a parser cannot be configured or if parsing fails
	 */
	public static final boolean isWellformedXML(URI uri) throws XMLToolsException {
		return parseXML(uri)!=null;
	}
	
	/**
	 * Asserts that the specified file is well formed and returns some root node information.
	 * @param f the file
	 * @return returns the root node, or null if file is not well formed
	 * @throws XMLToolsException if a parser cannot be configured or if parsing fails
	 */
	public static final XMLInfo parseXML(File f) throws XMLToolsException {
		return parseXML(f, false);
	}
	
	/**
	 * Asserts that the contents at the specified URI is well formed and returns some root node information.
	 * @param uri the URI
	 * @return returns the root node, or null if file is not well formed
	 * @throws XMLToolsException if a parser cannot be configured or if parsing fails
	 */
	public static final XMLInfo parseXML(URI uri) throws XMLToolsException {
		return parseXML(uri, false);
	}
	
	/**
	 * Returns some root node information and optionally asserts that the specified
	 * file is well formed.
	 * @param f the file
	 * @param peek true if the parsing should stop after reading the root element. If true,
	 * the file may or may not be well formed beyond the first start tag.
	 * @return returns the root node, or null if file is not well formed
	 * @throws XMLToolsException if a parser cannot be configured or if parsing fails
	 */
	public static final XMLInfo parseXML(File f, boolean peek) throws XMLToolsException {
		return parseXML(f.toURI(), peek);
	}
	
	/**
	 * Returns some root node information and optionally asserts that the contents at the
	 * specified URI is well formed.
	 * @param uri the URI
	 * @param peek true if the parsing should stop after reading the root element. If true,
	 * the file may or may not be well formed beyond the first start tag.
	 * @return returns the root node, or null if file is not well formed
	 * @throws XMLToolsException if a parser cannot be configured or if parsing fails
	 */
	public static final XMLInfo parseXML(URI uri, boolean peek) throws XMLToolsException {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setNamespaceAware(true);
		SAXParser saxParser = null;
		try {
			saxParser = factory.newSAXParser();
		} catch (ParserConfigurationException e) {
			throw new XMLToolsException("Failed to set up XML parser.", e);
		} catch (SAXException e) {
			throw new XMLToolsException("Failed to set up XML parser.", e);
		}
		XMLHandler dh = new XMLHandler(peek);
		try (InputStream is = uri.toURL().openStream()) {
	        XMLReader reader = saxParser.getXMLReader();
	        if (dh != null) {
	            reader.setContentHandler(dh);
	            reader.setEntityResolver(dh);
				//since we sometimes have loadDTD turned off,
				//we use lexical handler to get the pub and sys id of prolog
				reader.setProperty("http://xml.org/sax/properties/lexical-handler", dh);
	            reader.setErrorHandler(dh);
	            reader.setDTDHandler(dh);
	        }
	        InputSource source = new InputSource(is);
	        source.setSystemId(uri.toASCIIString());
			saxParser.getXMLReader().parse(source);
		} catch (StopParsing e) {
			//thrown if peek is true
		} catch (SAXException e) {
			return null;
		} catch (IOException e) {
			throw new XMLToolsException(e);
		}
		return dh.root;
	}
	
	private static class XMLHandler extends DefaultHandler implements LexicalHandler {
		private final EntityResolver resolver;
		private final boolean peek;
		private final XMLInfo.Builder builder;
		private XMLInfo root = null;
		
		XMLHandler(boolean peek) {
			this.resolver = new EntityResolverCache();
			this.peek = peek;
			this.builder = new XMLInfo.Builder();
		}

		@Override
		public void startElement(String uri, String localName, String qName,
				Attributes attributes) throws SAXException {
			if (this.root == null) {
				this.root = builder.uri(uri).localName(localName).qName(qName).attributes(attributes).build();
				if (peek) {
					throw new StopParsing();
				}
			}
		}

		@Override
		public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
			if (root == null) {
				//set prolog entity in builder
				builder.publicId(publicId);
				builder.systemId(systemId);
			}
			return resolver.resolveEntity(publicId, systemId);
		}

		@Override
		public void startDTD(String name, String publicId, String systemId) throws SAXException {
			builder.publicId(publicId);
			builder.systemId(systemId);
		}

		@Override
		public void endDTD() throws SAXException {
			// no-op
		}

		@Override
		public void startEntity(String name) throws SAXException {
			// no-op
		}

		@Override
		public void endEntity(String name) throws SAXException {
			// no-op
		}

		@Override
		public void startCDATA() throws SAXException {
			// no-op
		}

		@Override
		public void endCDATA() throws SAXException {
			// no-op
		}

		@Override
		public void comment(char[] ch, int start, int length)
				throws SAXException {
			// no-op
		}
	}

	private static class StopParsing extends SAXException {

		private static final long serialVersionUID = -4335028194855324300L;
		
	}

}
