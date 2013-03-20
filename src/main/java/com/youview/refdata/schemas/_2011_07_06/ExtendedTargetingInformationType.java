//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2013.02.25 at 10:09:26 AM GMT 
//


package com.youview.refdata.schemas._2011_07_06;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import tva.metadata._2010.ControlledTermType;
import tva.metadata.extended._2010.TargetingInformationType;


/**
 * <p>Java class for ExtendedTargetingInformationType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ExtendedTargetingInformationType">
 *   &lt;complexContent>
 *     &lt;extension base="{urn:tva:metadata:extended:2010}TargetingInformationType">
 *       &lt;sequence>
 *         &lt;element name="TargetPlace" type="{http://refdata.youview.com/schemas/2011-07-06}TargetPlaceType" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="TargetUserGroup" type="{urn:tva:metadata:2010}ControlledTermType" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ExtendedTargetingInformationType", propOrder = {
    "targetPlace",
    "targetUserGroup"
})
public class ExtendedTargetingInformationType
    extends TargetingInformationType
{

    @XmlElement(name = "TargetPlace")
    protected List<TargetPlaceType> targetPlace;
    @XmlElement(name = "TargetUserGroup")
    protected List<ControlledTermType> targetUserGroup;

    /**
     * Gets the value of the targetPlace property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the targetPlace property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getTargetPlace().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TargetPlaceType }
     * 
     * 
     */
    public List<TargetPlaceType> getTargetPlace() {
        if (targetPlace == null) {
            targetPlace = new ArrayList<TargetPlaceType>();
        }
        return this.targetPlace;
    }

    /**
     * Gets the value of the targetUserGroup property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the targetUserGroup property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getTargetUserGroup().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ControlledTermType }
     * 
     * 
     */
    public List<ControlledTermType> getTargetUserGroup() {
        if (targetUserGroup == null) {
            targetUserGroup = new ArrayList<ControlledTermType>();
        }
        return this.targetUserGroup;
    }

}
