package com.skava.events.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XmlUtil {
	public static Document parseXml(String xmlData) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilder newDocumentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		return newDocumentBuilder.parse(new ByteArrayInputStream(xmlData.getBytes()));
	}
	public static Document jsonToXml(String jsonData) throws  JSONException, ParserConfigurationException, SAXException, IOException {
		JSONObject json = new JSONObject(jsonData);
		String xmlData = XML.toString(json);
		return parseXml("<root>" + xmlData + "</root>");
	}
	
	public static Object getNodeSet(Document document, String xpathExp) throws XPathExpressionException {
		XPathFactory xPathfactory = XPathFactory.newInstance();
		XPathExpression expr = xPathfactory.newXPath().compile(xpathExp);
		return expr.evaluate(document, XPathConstants.NODESET);
	}
	
	public static String getNodeValue(Document document, String xpathExp) throws XPathExpressionException {
		XPathFactory xPathfactory = XPathFactory.newInstance();
		XPathExpression expr = xPathfactory.newXPath().compile(xpathExp);
		return (String) expr.evaluate(document, XPathConstants.STRING);
	}
	
	public static JSONArray getMultipleNodeValues(Document document, String xpathExp, String childNode) throws XPathExpressionException, ParserConfigurationException, IOException, SAXException, TransformerException {
	    TransformerFactory transFactory;
	    Transformer transformer;
	    StringWriter buffer;
	    Document childDoc;
	    JSONArray valueArr = new JSONArray();
	    
	    NodeList nodes = (NodeList) getNodeSet(document, xpathExp);
        
        for(int i=0; i < nodes.getLength(); i++) {
            transFactory = TransformerFactory.newInstance();
            transformer = transFactory.newTransformer();
            buffer = new StringWriter();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.transform(new DOMSource(nodes.item(i)),
                  new StreamResult(buffer));
            childDoc = parseXml(buffer.toString());
            if(childNode.equals(".")) {
                valueArr.put(getNodeValue(childDoc, childNode));
            }
            else {
                valueArr.put(getNodeValue(childDoc, "//" + childNode));
            }
            
        }
        return valueArr;
    }
}
