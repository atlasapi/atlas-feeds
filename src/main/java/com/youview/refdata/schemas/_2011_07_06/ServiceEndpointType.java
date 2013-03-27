//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2013.02.25 at 10:09:26 AM GMT 
//


package com.youview.refdata.schemas._2011_07_06;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import tva.metadata._2010.ControlledTermType;


/**
 * <p>Java class for ServiceEndpointType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ServiceEndpointType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="EndpointType" type="{urn:tva:metadata:2010}ControlledTermType"/>
 *       &lt;/sequence>
 *       &lt;attribute name="locator" use="required" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ServiceEndpointType", propOrder = {
    "endpointType"
})
public class ServiceEndpointType {

    @XmlElement(name = "EndpointType", required = true)
    protected ControlledTermType endpointType;
    @XmlAttribute(required = true)
    @XmlSchemaType(name = "anyURI")
    protected String locator;

    /**
     * Gets the value of the endpointType property.
     * 
     * @return
     *     possible object is
     *     {@link ControlledTermType }
     *     
     */
    public ControlledTermType getEndpointType() {
        return endpointType;
    }

    /**
     * Sets the value of the endpointType property.
     * 
     * @param value
     *     allowed object is
     *     {@link ControlledTermType }
     *     
     */
    public void setEndpointType(ControlledTermType value) {
        this.endpointType = value;
    }

    /**
     * Gets the value of the locator property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLocator() {
        return locator;
    }

    /**
     * Sets the value of the locator property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLocator(String value) {
        this.locator = value;
    }

}
