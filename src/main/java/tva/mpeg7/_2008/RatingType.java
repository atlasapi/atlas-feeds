//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2013.02.25 at 10:09:26 AM GMT 
//


package tva.mpeg7._2008;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * <p>Java class for RatingType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="RatingType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="RatingValue" type="{http://www.w3.org/2001/XMLSchema}float"/>
 *         &lt;element name="RatingScheme">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;extension base="{urn:tva:mpeg7:2008}TermUseType">
 *                 &lt;attribute name="best" type="{http://www.w3.org/2001/XMLSchema}float" />
 *                 &lt;attribute name="worst" type="{http://www.w3.org/2001/XMLSchema}float" />
 *                 &lt;attribute name="style" use="required">
 *                   &lt;simpleType>
 *                     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}NMTOKEN">
 *                       &lt;enumeration value="higherBetter"/>
 *                       &lt;enumeration value="lowerBetter"/>
 *                     &lt;/restriction>
 *                   &lt;/simpleType>
 *                 &lt;/attribute>
 *               &lt;/extension>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RatingType", propOrder = {
    "ratingValue",
    "ratingScheme"
})
public class RatingType {

    @XmlElement(name = "RatingValue")
    protected float ratingValue;
    @XmlElement(name = "RatingScheme", required = true)
    protected RatingType.RatingScheme ratingScheme;

    /**
     * Gets the value of the ratingValue property.
     * 
     */
    public float getRatingValue() {
        return ratingValue;
    }

    /**
     * Sets the value of the ratingValue property.
     * 
     */
    public void setRatingValue(float value) {
        this.ratingValue = value;
    }

    /**
     * Gets the value of the ratingScheme property.
     * 
     * @return
     *     possible object is
     *     {@link RatingType.RatingScheme }
     *     
     */
    public RatingType.RatingScheme getRatingScheme() {
        return ratingScheme;
    }

    /**
     * Sets the value of the ratingScheme property.
     * 
     * @param value
     *     allowed object is
     *     {@link RatingType.RatingScheme }
     *     
     */
    public void setRatingScheme(RatingType.RatingScheme value) {
        this.ratingScheme = value;
    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;extension base="{urn:tva:mpeg7:2008}TermUseType">
     *       &lt;attribute name="best" type="{http://www.w3.org/2001/XMLSchema}float" />
     *       &lt;attribute name="worst" type="{http://www.w3.org/2001/XMLSchema}float" />
     *       &lt;attribute name="style" use="required">
     *         &lt;simpleType>
     *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}NMTOKEN">
     *             &lt;enumeration value="higherBetter"/>
     *             &lt;enumeration value="lowerBetter"/>
     *           &lt;/restriction>
     *         &lt;/simpleType>
     *       &lt;/attribute>
     *     &lt;/extension>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "")
    public static class RatingScheme
        extends TermUseType
    {

        @XmlAttribute
        protected Float best;
        @XmlAttribute
        protected Float worst;
        @XmlAttribute(required = true)
        @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
        protected String style;

        /**
         * Gets the value of the best property.
         * 
         * @return
         *     possible object is
         *     {@link Float }
         *     
         */
        public Float getBest() {
            return best;
        }

        /**
         * Sets the value of the best property.
         * 
         * @param value
         *     allowed object is
         *     {@link Float }
         *     
         */
        public void setBest(Float value) {
            this.best = value;
        }

        /**
         * Gets the value of the worst property.
         * 
         * @return
         *     possible object is
         *     {@link Float }
         *     
         */
        public Float getWorst() {
            return worst;
        }

        /**
         * Sets the value of the worst property.
         * 
         * @param value
         *     allowed object is
         *     {@link Float }
         *     
         */
        public void setWorst(Float value) {
            this.worst = value;
        }

        /**
         * Gets the value of the style property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getStyle() {
            return style;
        }

        /**
         * Sets the value of the style property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setStyle(String value) {
            this.style = value;
        }

    }

}
