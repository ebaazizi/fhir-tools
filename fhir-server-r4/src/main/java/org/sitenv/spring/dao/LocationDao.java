package org.sitenv.spring.dao;

import java.util.List;

import org.sitenv.spring.model.DafLocation;
import org.sitenv.spring.util.SearchParameterMap;

public interface LocationDao {

	public DafLocation getLocationById(int id);

	public DafLocation getLocationByVersionId(int theId, String versionId);

	public List<DafLocation> getLocationHistoryById(int theId);

	public List<DafLocation> search(SearchParameterMap searchParameterMap);
}