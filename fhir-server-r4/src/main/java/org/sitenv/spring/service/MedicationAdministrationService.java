package org.sitenv.spring.service;

import org.sitenv.spring.model.DafMedicationAdministration;
import org.sitenv.spring.util.SearchParameterMap;

import java.util.List;

public interface MedicationAdministrationService {
	
	public DafMedicationAdministration getMedicationAdministrationById(int id);
	
	public DafMedicationAdministration getMedicationAdministrationByVersionId(int theId, String versionId);
	
	public List<DafMedicationAdministration> getMedicationAdministrationHistoryById(int theId);
	
	public List<DafMedicationAdministration> search(SearchParameterMap paramMap);
}
