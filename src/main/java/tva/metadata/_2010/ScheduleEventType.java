//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2013.02.25 at 10:09:26 AM GMT 
//


package tva.metadata._2010;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * <p>Java class for ScheduleEventType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ScheduleEventType">
 *   &lt;complexContent>
 *     &lt;extension base="{urn:tva:metadata:2010}ProgramLocationType">
 *       &lt;sequence>
 *         &lt;element name="PublishedStartTime" type="{http://www.w3.org/2001/XMLSchema}dateTime" minOccurs="0"/>
 *         &lt;element name="PublishedEndTime" type="{http://www.w3.org/2001/XMLSchema}dateTime" minOccurs="0"/>
 *         &lt;element name="PublishedDuration" type="{http://www.w3.org/2001/XMLSchema}duration" minOccurs="0"/>
 *         &lt;element name="Live" type="{urn:tva:metadata:2010}FlagType" minOccurs="0"/>
 *         &lt;element name="Repeat" type="{urn:tva:metadata:2010}FlagType" minOccurs="0"/>
 *         &lt;element name="Free" type="{urn:tva:metadata:2010}FlagType" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang"/>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ScheduleEventType", propOrder = {
    "publishedStartTime",
    "publishedEndTime",
    "publishedDuration",
    "live",
    "repeat",
    "free"
})
@XmlSeeAlso({
    BroadcastEventType.class
})
public class ScheduleEventType
    extends ProgramLocationType
{

    @XmlElement(name = "PublishedStartTime")
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar publishedStartTime;
    @XmlElement(name = "PublishedEndTime")
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar publishedEndTime;
    @XmlElement(name = "PublishedDuration")
    protected Duration publishedDuration;
    @XmlElement(name = "Live")
    protected FlagType live;
    @XmlElement(name = "Repeat")
    protected FlagType repeat;
    @XmlElement(name = "Free")
    protected FlagType free;
    @XmlAttribute(namespace = "http://www.w3.org/XML/1998/namespace")
    protected String lang;

    /**
     * Gets the value of the publishedStartTime property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getPublishedStartTime() {
        return publishedStartTime;
    }

    /**
     * Sets the value of the publishedStartTime property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setPublishedStartTime(XMLGregorianCalendar value) {
        this.publishedStartTime = value;
    }

    /**
     * Gets the value of the publishedEndTime property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getPublishedEndTime() {
        return publishedEndTime;
    }

    /**
     * Sets the value of the publishedEndTime property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setPublishedEndTime(XMLGregorianCalendar value) {
        this.publishedEndTime = value;
    }

    /**
     * Gets the value of the publishedDuration property.
     * 
     * @return
     *     possible object is
     *     {@link Duration }
     *     
     */
    public Duration getPublishedDuration() {
        return publishedDuration;
    }

    /**
     * Sets the value of the publishedDuration property.
     * 
     * @param value
     *     allowed object is
     *     {@link Duration }
     *     
     */
    public void setPublishedDuration(Duration value) {
        this.publishedDuration = value;
    }

    /**
     * Gets the value of the live property.
     * 
     * @return
     *     possible object is
     *     {@link FlagType }
     *     
     */
    public FlagType getLive() {
        return live;
    }

    /**
     * Sets the value of the live property.
     * 
     * @param value
     *     allowed object is
     *     {@link FlagType }
     *     
     */
    public void setLive(FlagType value) {
        this.live = value;
    }

    /**
     * Gets the value of the repeat property.
     * 
     * @return
     *     possible object is
     *     {@link FlagType }
     *     
     */
    public FlagType getRepeat() {
        return repeat;
    }

    /**
     * Sets the value of the repeat property.
     * 
     * @param value
     *     allowed object is
     *     {@link FlagType }
     *     
     */
    public void setRepeat(FlagType value) {
        this.repeat = value;
    }

    /**
     * Gets the value of the free property.
     * 
     * @return
     *     possible object is
     *     {@link FlagType }
     *     
     */
    public FlagType getFree() {
        return free;
    }

    /**
     * Sets the value of the free property.
     * 
     * @param value
     *     allowed object is
     *     {@link FlagType }
     *     
     */
    public void setFree(FlagType value) {
        this.free = value;
    }

    /**
     * Gets the value of the lang property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLang() {
        return lang;
    }

    /**
     * Sets the value of the lang property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLang(String value) {
        this.lang = value;
    }

}
