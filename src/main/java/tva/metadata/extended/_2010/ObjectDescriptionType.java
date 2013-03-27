//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2013.02.25 at 10:09:26 AM GMT 
//


package tva.metadata.extended._2010;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import com.youview.refdata.schemas._2011_07_06.ExtendedObjectDescriptionType;
import tva.mpeg7._2008.TextualType;


/**
 * <p>Java class for ObjectDescriptionType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ObjectDescriptionType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Description" type="{urn:tva:mpeg7:2008}TextualType" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="ContentDescription" type="{urn:tva:metadata:extended:2010}ExtendedContentDescriptionType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ObjectDescriptionType", propOrder = {
    "description",
    "contentDescription"
})
@XmlSeeAlso({
    ExtendedObjectDescriptionType.class
})
public class ObjectDescriptionType {

    @XmlElement(name = "Description")
    protected List<TextualType> description;
    @XmlElement(name = "ContentDescription")
    protected ExtendedContentDescriptionType contentDescription;

    /**
     * Gets the value of the description property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the description property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDescription().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TextualType }
     * 
     * 
     */
    public List<TextualType> getDescription() {
        if (description == null) {
            description = new ArrayList<TextualType>();
        }
        return this.description;
    }

    /**
     * Gets the value of the contentDescription property.
     * 
     * @return
     *     possible object is
     *     {@link ExtendedContentDescriptionType }
     *     
     */
    public ExtendedContentDescriptionType getContentDescription() {
        return contentDescription;
    }

    /**
     * Sets the value of the contentDescription property.
     * 
     * @param value
     *     allowed object is
     *     {@link ExtendedContentDescriptionType }
     *     
     */
    public void setContentDescription(ExtendedContentDescriptionType value) {
        this.contentDescription = value;
    }

}
