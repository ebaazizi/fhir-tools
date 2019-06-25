package org.sitenv.spring.dao;

import org.sitenv.spring.model.DafDiagnosticReport;
import org.sitenv.spring.util.SearchParameterMap;

import java.util.List;

public interface DiagnosticReportDao {
	
	public DafDiagnosticReport getDiagnosticReportById(int id);

	public DafDiagnosticReport getDiagnosticReportByVersionId(int theId, String versionId);

	public List<DafDiagnosticReport> getDiagnosticReportHistoryById(int theId);

	public List<DafDiagnosticReport> search(SearchParameterMap theMap);

}
