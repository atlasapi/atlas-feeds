package org.atlasapi.feeds.lakeview.validation.rules;

import com.google.inject.internal.Objects;

public class ValidationResult {

	public enum ValidationResultType { SUCCESS, FAILURE, INFO }

	private String validationRuleName;
	private ValidationResultType result;
	private String details;
	
	public ValidationResult(String validationRuleName, ValidationResultType result, String details) {
		this.validationRuleName = validationRuleName;
		this.result = result;
		this.details = details;
		
	}
	
	public ValidationResult(String validationRuleName, ValidationResultType result) {
		this.validationRuleName = validationRuleName;
		this.result = result;
	}

	public String getValidationRuleName() {
		return validationRuleName;
	}

	public ValidationResultType getResult() {
		return result;
	}

	public String getDetails() {
		return details;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(validationRuleName, result, details);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ValidationResult other = (ValidationResult) obj;
		if (details == null) {
			if (other.details != null)
				return false;
		} else if (!details.equals(other.details))
			return false;
		if (result != other.result)
			return false;
		if (validationRuleName == null) {
			if (other.validationRuleName != null)
				return false;
		} else if (!validationRuleName.equals(other.validationRuleName))
			return false;
		return true;
	}
}
