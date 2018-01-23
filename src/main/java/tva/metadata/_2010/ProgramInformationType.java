//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2013.02.25 at 10:09:26 AM GMT 
//


package tva.metadata._2010;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;

import com.youview.refdata.schemas._2011_07_06.ExtendedTargetingInformationType;
import tva.mpeg7._2008.UniqueIDType;


/**
 * <p>Java class for ProgramInformationType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ProgramInformationType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="BasicDescription" type="{urn:tva:metadata:2010}BasicContentDescriptionType"/>
 *         &lt;element name="OtherIdentifier" type="{urn:tva:mpeg7:2008}UniqueIDType" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="MemberOf" type="{urn:tva:metadata:2010}BaseMemberOfType" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="DerivedFrom" type="{urn:tva:metadata:2010}DerivedFromType"/>
 *       &lt;/sequence>
 *       &lt;attGroup ref="{urn:tva:metadata:2010}fragmentIdentification"/>
 *       &lt;attribute name="programId" use="required" type="{urn:tva:metadata:2010}CRIDType" />
 *       &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang"/>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ProgramInformationType", propOrder = {
    "basicDescription",
    "TargetingInformation",
    "otherIdentifier",
    "memberOf",
    "derivedFrom"
})
public class ProgramInformationType {

    @XmlElement(name = "BasicDescription", required = true)
    protected BasicContentDescriptionType basicDescription;
    @XmlElement(name = "OtherIdentifier")
    protected List<UniqueIDType> otherIdentifier;
    @XmlElement(name = "MemberOf")
    protected List<BaseMemberOfType> memberOf;
    @XmlElement(name = "DerivedFrom", required = true)
    protected DerivedFromType derivedFrom;
    @XmlAttribute(required = true)
    protected String programId;
    @XmlAttribute(namespace = "http://www.w3.org/XML/1998/namespace")
    protected String lang;
    @XmlAttribute
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar fragmentExpirationDate;

    // this element was added here by hand to meet the requirement of 4300CP,
    // in reference to the YV specification document.
    @XmlElement(name = "TargetingInformation")
    protected List<ExtendedTargetingInformationType> targetingInformation;

    public List<ExtendedTargetingInformationType> getTargetingInformation() {
        if (targetingInformation == null) {
            targetingInformation = new ArrayList<ExtendedTargetingInformationType>();
        }
        return targetingInformation;
    }

    /**
     * Gets the value of the basicDescription property.
     * 
     * @return
     *     possible object is
     *     {@link BasicContentDescriptionType }
     *     
     */
    public BasicContentDescriptionType getBasicDescription() {
        return basicDescription;
    }

    /**
     * Sets the value of the basicDescription property.
     * 
     * @param value
     *     allowed object is
     *     {@link BasicContentDescriptionType }
     *     
     */
    public void setBasicDescription(BasicContentDescriptionType value) {
        this.basicDescription = value;
    }

    /**
     * Gets the value of the otherIdentifier property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the otherIdentifier property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getOtherIdentifier().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link UniqueIDType }
     * 
     * 
     */
    public List<UniqueIDType> getOtherIdentifier() {
        if (otherIdentifier == null) {
            otherIdentifier = new ArrayList<UniqueIDType>();
        }
        return this.otherIdentifier;
    }

    /**
     * Gets the value of the memberOf property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the memberOf property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getMemberOf().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link BaseMemberOfType }
     * 
     * 
     */
    public List<BaseMemberOfType> getMemberOf() {
        if (memberOf == null) {
            memberOf = new ArrayList<BaseMemberOfType>();
        }
        return this.memberOf;
    }

    /**
     * Gets the value of the derivedFrom property.
     * 
     * @return
     *     possible object is
     *     {@link DerivedFromType }
     *     
     */
    public DerivedFromType getDerivedFrom() {
        return derivedFrom;
    }

    /**
     * Sets the value of the derivedFrom property.
     * 
     * @param value
     *     allowed object is
     *     {@link DerivedFromType }
     *     
     */
    public void setDerivedFrom(DerivedFromType value) {
        this.derivedFrom = value;
    }

    /**
     * Gets the value of the programId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getProgramId() {
        return programId;
    }

    /**
     * Sets the value of the programId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setProgramId(String value) {
        this.programId = value;
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

    /**
     * Gets the value of the fragmentExpirationDate property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getFragmentExpirationDate() {
        return fragmentExpirationDate;
    }

    /**
     * Sets the value of the fragmentExpirationDate property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setFragmentExpirationDate(XMLGregorianCalendar value) {
        this.fragmentExpirationDate = value;
    }

}
