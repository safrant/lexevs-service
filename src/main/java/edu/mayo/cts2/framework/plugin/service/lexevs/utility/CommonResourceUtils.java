package edu.mayo.cts2.framework.plugin.service.lexevs.utility;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.LexGrid.LexBIG.DataModel.Collections.CodingSchemeRenderingList;
import org.LexGrid.LexBIG.DataModel.Collections.LocalNameList;
import org.LexGrid.LexBIG.DataModel.Core.CodingSchemeSummary;
import org.LexGrid.LexBIG.DataModel.Core.CodingSchemeVersionOrTag;
import org.LexGrid.LexBIG.DataModel.Core.ResolvedConceptReference;
import org.LexGrid.LexBIG.DataModel.InterfaceElements.CodingSchemeRendering;
import org.LexGrid.LexBIG.Exceptions.LBException;
import org.LexGrid.LexBIG.Exceptions.LBInvocationException;
import org.LexGrid.LexBIG.Exceptions.LBParameterException;
import org.LexGrid.LexBIG.Extensions.Generic.MappingExtension;
import org.LexGrid.LexBIG.Extensions.Generic.MappingExtension.MappingSortOption;
import org.LexGrid.LexBIG.LexBIGService.CodedNodeSet;
import org.LexGrid.LexBIG.LexBIGService.LexBIGService;
import org.LexGrid.LexBIG.Utility.Constructors;
import org.LexGrid.LexBIG.Utility.Iterators.ResolvedConceptReferencesIterator;
import org.LexGrid.codingSchemes.CodingScheme;

import edu.mayo.cts2.framework.model.command.Page;
import edu.mayo.cts2.framework.model.command.ResolvedFilter;
import edu.mayo.cts2.framework.model.core.SortCriteria;
import edu.mayo.cts2.framework.model.directory.DirectoryResult;
import edu.mayo.cts2.framework.plugin.service.lexevs.naming.VersionNameConverter;
import edu.mayo.cts2.framework.plugin.service.lexevs.transform.LexEvsToCTS2Transformer;
import edu.mayo.cts2.framework.service.command.restriction.MapQueryServiceRestrictions.CodeSystemRestriction;
import edu.mayo.cts2.framework.service.command.restriction.MapVersionQueryServiceRestrictions.EntitiesRestriction;
import edu.mayo.cts2.framework.service.profile.ResourceQuery;
import edu.mayo.cts2.framework.service.profile.mapentry.MapEntryQuery;

public class CommonResourceUtils{
	private final static String UNCHECKED = "unchecked";
	private final static String RAWTYPES = "rawtypes";
	
	@SuppressWarnings({ RAWTYPES, UNCHECKED })
	public static <EntryType> DirectoryResult<EntryType> createDirectoryResultWithEntryDescriptions(
			LexEvsToCTS2Transformer transformer,
			ResolvedConceptReferenceResults resolvedConceptReferenceResults,
			String descriptionType) {
		
		List<EntryType> list = new ArrayList<EntryType>();
		DirectoryResult<EntryType> directoryResult = new DirectoryResult<EntryType>(list, true);
		EntryType entry = null;
		if(resolvedConceptReferenceResults != null){
			ResolvedConceptReference[] resolvedConceptReferences = resolvedConceptReferenceResults.getResolvedConceptReference();
			if(resolvedConceptReferences != null){
				for(ResolvedConceptReference reference : resolvedConceptReferences){
					if(descriptionType.equals(Constants.FULL_DESCRIPTION)){
						entry = (EntryType) transformer.transformFullDescription(reference);
					}
					else if(descriptionType.equals(Constants.SUMMARY_DESCRIPTION)){
						entry = (EntryType) transformer.transformSummaryDescription(reference);
					}
					list.add(entry);
				}
			}			
			directoryResult = new DirectoryResult<EntryType>(list, resolvedConceptReferenceResults.isAtEnd());
		}

		return directoryResult;
	}	

	@SuppressWarnings({ UNCHECKED, RAWTYPES })
	public static <EntryType, DataType> DirectoryResult<EntryType> createDirectoryResultWithEntryDescriptions(
			LexEvsToCTS2Transformer transformer,
			DataType[] dataCollection, 
			boolean atEnd,
			String descriptionType) {
		List<EntryType> list = new ArrayList<EntryType>();
		DirectoryResult<EntryType> directoryResult = new DirectoryResult<EntryType>(list, true);
		EntryType entry = null;
		
		if(dataCollection != null){
			for (DataType data : dataCollection) {
				if(descriptionType.equals(Constants.FULL_DESCRIPTION)){
					entry = (EntryType) transformer.transformFullDescription(data);
				}
				else if(descriptionType.equals(Constants.SUMMARY_DESCRIPTION)){
					entry = (EntryType) transformer.transformSummaryDescription(data);
				}
				
				list.add(entry);
			}
			directoryResult = new DirectoryResult<EntryType>(list, atEnd);
		}
		
		return directoryResult;
	}
	
	@SuppressWarnings({ RAWTYPES, UNCHECKED })
	public static <EntryType> DirectoryResult<EntryType> createDirectoryResultWithEntryFullVersionDescriptions(
			LexBIGService lexBigService, 
			LexEvsToCTS2Transformer transformer, 
			CodingSchemeRendering[] csRendering, 
			boolean atEnd2){
		List<EntryType> list = new ArrayList<EntryType>();
		boolean atEnd = true;
		DirectoryResult<EntryType> directoryResult = new DirectoryResult<EntryType>(list, atEnd);
		
		if(csRendering != null){
			for (CodingSchemeRendering render : csRendering) {
				String codingSchemeName = render.getCodingSchemeSummary().getCodingSchemeURI();			
				String version = render.getCodingSchemeSummary().getRepresentsVersion();
				CodingSchemeVersionOrTag tagOrVersion = Constructors.createCodingSchemeVersionOrTagFromVersion(version);
				CodingScheme codingScheme;
				try {
					codingScheme = lexBigService.resolveCodingScheme(codingSchemeName, tagOrVersion);
					list.add((EntryType) transformer.transformFullDescription(codingScheme));
				} catch (LBException e) {
					throw new RuntimeException(e);
				}
			}
			directoryResult = new DirectoryResult<EntryType>(list, atEnd);
		}
		
		return directoryResult;
	}
	
	// --------------------------------------------
	public static <T extends ResourceQuery> CodingSchemeRendering[] getRenderingListForQuery(
			LexBIGService lexBigService, 
			VersionNameConverter nameConverter, 
			QueryData<T> queryData,
			MappingExtension mappingExtension,
			SortCriteria sortCriteria){
		
		CodingSchemeRenderingList renderingList = null;
		String codingSchemeName = queryData.getNameVersionPairName();
		
		try {
			renderingList = lexBigService.getSupportedCodingSchemes();
		} catch (LBInvocationException e) {
			throw new RuntimeException();
		}
		
		renderingList = CommonResourceUtils.restrictRenderingListBySchemeNameAndMapExtension(renderingList, codingSchemeName, mappingExtension);
		renderingList = CommonResourceUtils.restrictRenderingListByQuery(renderingList, queryData.getFilters(), nameConverter);
		
		return renderingList.getCodingSchemeRendering();
	}
	
	
	public static CodingSchemeRenderingList restrictRenderingListBySchemeNameAndMapExtension(
			CodingSchemeRenderingList renderingList, 
			String codingSchemeName, 
			MappingExtension mappingExtension) {
		
		if(renderingList == null){
			return renderingList;
		}

		boolean restrictBOTH = (codingSchemeName != null && mappingExtension != null);
		boolean restrictNAME = (!restrictBOTH && codingSchemeName != null);
		boolean restrictMAP = (!restrictBOTH && mappingExtension != null);
		
		CodingSchemeRenderingList temp = new CodingSchemeRenderingList();
		
		CodingSchemeRendering[] csRendering = renderingList.getCodingSchemeRendering();
		for(CodingSchemeRendering render : csRendering) {
			CodingSchemeSummary codingSchemeSummary = render.getCodingSchemeSummary();
			String uri = codingSchemeSummary.getCodingSchemeURI();
			String version = codingSchemeSummary.getRepresentsVersion();
			
			if(restrictBOTH){
				if (codingSchemeSummary.getLocalName().equals(codingSchemeName)) {
					// Add if valid Mapping Coding Scheme
					if (CommonMapUtils.validateMappingCodingScheme(uri, version, mappingExtension)) {
						temp.addCodingSchemeRendering(render);
					}
				}
			}
			else if(restrictNAME){
				if (codingSchemeSummary.getLocalName().equals(codingSchemeName)) {
					temp.addCodingSchemeRendering(render);
				}
			}
			else if(restrictMAP){
				if (CommonMapUtils.validateMappingCodingScheme(uri, version, mappingExtension)) {
					temp.addCodingSchemeRendering(render);
				}
			}
			else{
				temp.addCodingSchemeRendering(render);
			}
			
		}
		
		return temp;
	}

	public static CodingSchemeRenderingList restrictRenderingListByQuery(
			CodingSchemeRenderingList renderingList, 
			Set<ResolvedFilter> filters,
			VersionNameConverter nameConverter) {
		
		if(renderingList != null && filters != null){
			Iterator<ResolvedFilter> filtersItr = filters.iterator();
			while (filtersItr.hasNext() && (renderingList.getCodingSchemeRenderingCount() > 0)) {
				ResolvedFilter resolvedFilter = filtersItr.next();
				renderingList = CommonSearchFilterUtils.filterRenderingList(resolvedFilter, 
						renderingList, nameConverter);
			}
		}
		
		return renderingList;
	}
	


	public static <T extends ResourceQuery> List<CodingScheme> getCodingSchemeList(
			LexBIGService lexBigService, 
			VersionNameConverter nameConverter,
			MappingExtension mappingExtension,
			QueryData<T> queryData,
			SortCriteria sortCriteria) {

		List<CodingScheme> codingSchemeList = new ArrayList<CodingScheme>();
		
		CodingSchemeRendering[] codingSchemeRendering;
		codingSchemeRendering = CommonResourceUtils.getRenderingListForQuery(lexBigService, nameConverter, queryData, mappingExtension, sortCriteria); 

		if(codingSchemeRendering != null){
			CodeSystemRestriction codeSystemRestriction = queryData.getCodeSystemRestriction();
			EntitiesRestriction entitiesRestriction = queryData.getEntitiesRestriction();
			
			codingSchemeList = CommonSearchFilterUtils.getCodingSchemeListFromCodeSchemeRenderings(lexBigService, codingSchemeRendering, codeSystemRestriction);
			
			
	//		if(entitiesRestriction != null && codingSchemeList != null){
	//			codingSchemeList = CommonSearchFilterUtils.filterCodingSchemeListByEntitiesRestriction(codingSchemeList, entitiesRestriction);
	//		}
		}
		
		return codingSchemeList;
	}

	public static <T extends ResourceQuery> CodedNodeSet getCodedNodeSet(
			LexBIGService lexBigService, 
			QueryData<T> queryData,
			SortCriteria sortCriteria){
		CodedNodeSet codedNodeSet = null;
		boolean codingSchemeExists = false;
		
		if(queryData.hasNameAndVersion()){
			try {
				// Get Code Node Set from LexBIG service for given coding scheme
				LocalNameList entityTypes = new LocalNameList();
				CodingSchemeRenderingList codingSchemeRenderingList = lexBigService.getSupportedCodingSchemes();
				codingSchemeExists = CommonUtils.queryContainsValidCodingScheme(queryData, codingSchemeRenderingList);			
				if(codingSchemeExists){
					codedNodeSet = lexBigService.getNodeSet(queryData.getNameVersionPairName(), queryData.getVersionOrTag() , entityTypes);
				}
			} catch (LBException e) {
				throw new RuntimeException(e);
			}
			Set<ResolvedFilter> filters = queryData.getFilters();
			if(codingSchemeExists && (filters != null)){
				for(ResolvedFilter filter : filters){
					CommonSearchFilterUtils.filterCodedNodeSetByResolvedFilter(filter, codedNodeSet);
				}
			}
		}
		
		return codedNodeSet;
	}
	
	public static ResolvedConceptReferenceResults getMapReferenceResults(
			MapEntryQuery query, SortCriteria sortCriteria, Page page,
			VersionNameConverter nameConverter, MappingExtension mappingExtension) {
		
		ResolvedConceptReferencesIterator iterator;
		QueryData<MapEntryQuery> queryData;

		queryData = new QueryData<MapEntryQuery>(query, nameConverter);
		
		String codingScheme = queryData.getCodeSystemVersionName();
		CodingSchemeVersionOrTag versionOrTag = queryData.getVersionOrTag();
		String relationsContainerName = null;
		List<MappingSortOption> sortOptionList = null;
		
		try {
			iterator = mappingExtension.resolveMapping(codingScheme, versionOrTag, relationsContainerName, sortOptionList);
		} catch (LBParameterException e) {
			throw new RuntimeException(e);
		}
		
		
		// TODO: Should be able to remove when reference class implements "get" method, then just need following return method
		return CommonPageUtils.getPageFromIterator(iterator, page);	
		
		
//		ArrayList<ResolvedConceptReference> referenceList = new ArrayList<ResolvedConceptReference>();
//		boolean atEnd = true;
//		int start = page.getStart();
//		int end = page.getEnd();
//		int index = 0;
//		try {
//			// Move iterator to start index
//			while((index < start) && (iterator.numberRemaining() > 0)){
//				index++;
//				iterator.next();
//			}
//			
//			// Collect page references
//			while((start < end) && (iterator.numberRemaining() > 0)){
//				ResolvedConceptReference ref = iterator.next();
//				referenceList.add(ref);
//			}
//		} catch (LBResourceUnavailableException e) {
//			throw new RuntimeException(e);
//		} catch (LBInvocationException e) {
//			throw new RuntimeException(e);
//		}
//		
//		ResolvedConceptReference [] resolvedConceptReference = (ResolvedConceptReference[]) referenceList.toArray();
//		return new ResolvedConceptReferenceResults(resolvedConceptReference, atEnd);
	}

}
