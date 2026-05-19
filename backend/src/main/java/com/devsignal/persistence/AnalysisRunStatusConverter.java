package com.devsignal.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AnalysisRunStatusConverter implements AttributeConverter<AnalysisRunStatus, String> {

	@Override
	public String convertToDatabaseColumn(AnalysisRunStatus attribute) {
		return attribute == null ? null : attribute.dbValue();
	}

	@Override
	public AnalysisRunStatus convertToEntityAttribute(String dbData) {
		return dbData == null ? null : AnalysisRunStatus.fromDbValue(dbData);
	}
}
