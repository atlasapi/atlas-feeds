package org.atlasapi.feeds.youview.resolutionapi;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.collect.ImmutableList;
import com.google.gson.annotations.SerializedName;

@XmlRootElement(name = "ion", namespace = "http://bbc.co.uk/2008/iplayer/ion")
public class ResolutionApiOutput {
    @XmlElement
    private Blocklist blocklist;
    
    public ResolutionApiOutput() {
        
    }

    public ResolutionApiOutput(Blocklist blocklist) {
        this.blocklist = blocklist;
    }

    public static ResolutionApiOutput empty() {
        return new ResolutionApiOutput(
                        new ResolutionApiOutput.Blocklist(
                            ImmutableList.<ResolutionApiOutput.Resolution>of()
                        )
        );
    }

    public static ResolutionApiOutput from(Resolution resolution) {
        return new ResolutionApiOutput(
                        new ResolutionApiOutput.Blocklist(
                                ImmutableList.of(resolution)
                        )
        );
    }

    public Blocklist getBlocklist() {
        return blocklist;
    }

    
    public static class Blocklist {
        @XmlElement
        @SerializedName(value = "blocklist")
        private List<Resolution> resolution;
        
        public Blocklist() {
            
        }

        public Blocklist(List<Resolution> resolution) {
            this.resolution = resolution;
        }

        public List<Resolution> getResolution() {
            return resolution;
        }

    }

    public static class Resolution {

        @XmlElement
        private String inputId;
        @XmlElement
        private String inputType;
        @XmlElement
        private String resolvedAs;
        @XmlElement
        private String resolvedType;
        
        public Resolution() {
            
        }

        private Resolution(String inputId, String inputType, String resolvedAs, String resolvedType) {
            this.inputId = checkNotNull(inputId);
            this.inputType = checkNotNull(inputType);
            this.resolvedAs = checkNotNull(resolvedAs);
            this.resolvedType = checkNotNull(resolvedType);
        }

        public String getInputId() {
            return inputId;
        }

        public String getInputType() {
            return inputType;
        }

        public String getResolvedAs() {
            return resolvedAs;
        }

        public String getResolvedType() {
            return resolvedType;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String inputId;
            private String inputType;
            private String resolvedAs;
            private String resolvedType;

            public Builder withInputId(String inputId) {
                this.inputId = inputId;
                return this;
            }

            public Builder withInputType(String inputType) {
                this.inputType = inputType;
                return this;
            }

            public Builder withResolvedAs(String resolvedAs) {
                this.resolvedAs = resolvedAs;
                return this;
            }

            public Builder withResolvedType(String resolvedType) {
                this.resolvedType = resolvedType;
                return this;
            }

            public Resolution build() {
                return new Resolution(inputId, inputType, resolvedAs, resolvedType);
            }

        }

    }

}
