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
import tva.metadata.extended._2010.PackageTableType;


/**
 * <p>Java class for ExtendedPackageTableType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ExtendedPackageTableType">
 *   &lt;complexContent>
 *     &lt;extension base="{urn:tva:metadata:extended:2010}PackageTableType">
 *       &lt;sequence>
 *         &lt;element name="PackageMembership" type="{http://refdata.youview.com/schemas/Metadata/2012-11-19}PackageMembershipType" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ExtendedPackageTableType", propOrder = {
    "packageMembership"
})
public class ExtendedPackageTableType
    extends PackageTableType
{

    @XmlElement(name = "PackageMembership")
    protected List<PackageMembershipType> packageMembership;

    /**
     * Gets the value of the packageMembership property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the packageMembership property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPackageMembership().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link PackageMembershipType }
     * 
     * 
     */
    public List<PackageMembershipType> getPackageMembership() {
        if (packageMembership == null) {
            packageMembership = new ArrayList<PackageMembershipType>();
        }
        return this.packageMembership;
    }

}
