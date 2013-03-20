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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import tva.mpeg7._2008.ParentalGuidanceType;


/**
 * <p>Java class for TVAParentalGuidanceType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="TVAParentalGuidanceType">
 *   &lt;complexContent>
 *     &lt;extension base="{urn:tva:mpeg7:2008}ParentalGuidanceType">
 *       &lt;sequence>
 *         &lt;element name="ExplanatoryText" type="{urn:tva:metadata:2010}ExplanationType" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TVAParentalGuidanceType", propOrder = {
    "explanatoryText"
})
public class TVAParentalGuidanceType
    extends ParentalGuidanceType
{

    @XmlElement(name = "ExplanatoryText")
    protected List<ExplanationType> explanatoryText;

    /**
     * Gets the value of the explanatoryText property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the explanatoryText property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getExplanatoryText().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ExplanationType }
     * 
     * 
     */
    public List<ExplanationType> getExplanatoryText() {
        if (explanatoryText == null) {
            explanatoryText = new ArrayList<ExplanationType>();
        }
        return this.explanatoryText;
    }

}
