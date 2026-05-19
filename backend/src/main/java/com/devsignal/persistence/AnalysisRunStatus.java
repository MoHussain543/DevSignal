package com.devsignal.persistence;

public enum AnalysisRunStatus {
	PROCESSING("processing"),
	COMPLETED("completed"),
	PARTIAL("partial"),
	FAILED("failed");

	private final String dbValue;

	AnalysisRunStatus(String dbValue) {
		this.dbValue = dbValue;
	}

	public String dbValue() {
		return dbValue;
	}

	public static AnalysisRunStatus fromDbValue(String value) {
		for (AnalysisRunStatus status : values()) {
			if (status.dbValue.equalsIgnoreCase(value)) {
				return status;
			}
		}
		throw new IllegalArgumentException("Unknown AnalysisRunStatus database value: " + value);
	}
}
