package tva.metadata.custom;

//This class was created to extend the classes in the tva.metadata._2010 package. The reason is that
//these classes were automatically generated and I don't know how, and it doesn't really feel safe
//to modify them. The 4300CP is in reference to the YV specification document that requires the
//changes done here.

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.youview.refdata.schemas._2011_07_06.ExtendedTargetingInformationType;
import tva.metadata._2010.ProgramInformationType;
import tva.mpeg7._2008.UniqueIDType;

/**
 * This class adds a targeting information field to the ProgramInformation element.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ProgramInformationType", propOrder = {
        "basicDescription",
        "otherIdentifier",
        "TargetingInformation",
        "memberOf",
        "derivedFrom"
})
public class ProgramInformationType4300CP extends ProgramInformationType {
    @XmlElement(name = "TargetingInformation")
    protected List<ExtendedTargetingInformationType> TargetingInformation;

    public List<ExtendedTargetingInformationType> getTargetingInformation() {
        if (TargetingInformation == null) {
            TargetingInformation = new ArrayList<ExtendedTargetingInformationType>();
        }
        return TargetingInformation;
    }
}
