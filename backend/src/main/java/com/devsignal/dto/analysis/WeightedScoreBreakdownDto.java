package com.devsignal.dto.analysis;

public record WeightedScoreBreakdownDto(
		int projectQualityPoints,
		int technicalBreadthPoints,
		int documentationPoints,
		int originalityPoints,
		int activityPoints,
		int totalPoints
) {}
