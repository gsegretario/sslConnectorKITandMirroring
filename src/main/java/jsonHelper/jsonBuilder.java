package jsonHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import ClientImplementation.Constant3DEXP;

public class jsonBuilder {
	
	
	
	public static String retriveEngId(JSONObject scopedEng) throws Exception {
		int totItems = scopedEng.getInt(Constant3DEXP.TOTALITEMS);
		String engItemId ="";
		if(totItems == 1) {
			JSONArray membersArray = scopedEng.getJSONArray(Constant3DEXP.MEMBER);
			JSONObject resultMember = membersArray.getJSONObject(0);
			if(resultMember.has(Constant3DEXP.SCOPEENGITEM)) {
				JSONObject scopeEngItem = resultMember.getJSONObject(Constant3DEXP.SCOPEENGITEM);
				engItemId = scopeEngItem.getString(Constant3DEXP.IDENTIFIER);
			}	
		}
		return engItemId;
	}
	
	public static String retriveEngItemInfo(JSONObject engItemDetail, String infoRequested, String infoRetrived) throws Exception {
		String engItemTitle = "";
		JSONArray memberArray = engItemDetail.getJSONArray(Constant3DEXP.MEMBER);
		if(memberArray.length()>0 && !memberArray.isEmpty()) {
			JSONObject engAttribute = memberArray.getJSONObject(0);
			if(infoRequested.equalsIgnoreCase(Constant3DEXP.TITLE)) {
				infoRetrived = engAttribute.getString(Constant3DEXP.TITLE);
			}
			else if(infoRequested.equalsIgnoreCase(Constant3DEXP.ATTRIBUTE_COMAU_REVISION)){
				if(engAttribute.has(Constant3DEXP.ENTERPRISEATTRIBUTE_ENG)) {
					JSONObject custAttrJson = engAttribute.getJSONObject(Constant3DEXP.ENTERPRISEATTRIBUTE_ENG);
					infoRetrived = custAttrJson.getString(Constant3DEXP.ATTRIBUTE_COMAU_REVISION);
				}
			}
			else if(infoRequested.equalsIgnoreCase(Constant3DEXP.ATTRIBUTE_COMAU_ENG_ISKITREQUIRED)){
				if(engAttribute.has(Constant3DEXP.ENTERPRISEATTRIBUTE_ENG)) {
					JSONObject custAttrJson = engAttribute.getJSONObject(Constant3DEXP.ENTERPRISEATTRIBUTE_ENG);
					infoRetrived = custAttrJson.getString(Constant3DEXP.ATTRIBUTE_COMAU_ENG_ISKITREQUIRED);
				}			
			}
			else if(infoRequested.equalsIgnoreCase(Constant3DEXP.ATTRIBUTE_COMAU_CODE)){
				if(engAttribute.has(Constant3DEXP.ENTERPRISEATTRIBUTE_ENG)) {
					JSONObject custAttrJson = engAttribute.getJSONObject(Constant3DEXP.ENTERPRISEATTRIBUTE_ENG);
					infoRetrived = custAttrJson.getString(Constant3DEXP.ATTRIBUTE_COMAU_CODE);
				}
			}
			else if(infoRequested.equalsIgnoreCase(Constant3DEXP.ATTRIBUTE_ENTERPRISEITEMNUMBER)){
				if(engAttribute.has(Constant3DEXP.ENTERPRISEREFERENCE_ENG)) {
					JSONObject custAttrJson = engAttribute.getJSONObject(Constant3DEXP.ENTERPRISEREFERENCE_ENG);
					infoRetrived = custAttrJson.getString(Constant3DEXP.ATTRIBUTE_ENTERPRISEITEMNUMBER);
				}
			}
		}
		return infoRetrived;
	}
	
	public static List<String> listclassId (JSONObject scopedEngItem) throws Exception {
		//JSONObject root = new JSONObject(scopedEngItem);
		JSONArray memberArray = scopedEngItem.getJSONArray(Constant3DEXP.MEMBER);
		List<String> listClassId = new ArrayList <>();
		
		if(memberArray.length()>0 && !memberArray.isEmpty()) {
			for (int i = 0; i < memberArray.length(); i++) {
			    JSONObject memberObj = memberArray.getJSONObject(i);
			    JSONObject parentClassification = memberObj.getJSONObject(Constant3DEXP.PARENTCLASSIFICATION);
			    JSONArray pcMembers = parentClassification.getJSONArray(Constant3DEXP.MEMBER);
			    if(pcMembers.length()>0 && !pcMembers.isEmpty()) {
			    	for (int j = 0; j < pcMembers.length(); j++) {
				        JSONObject pcMember = pcMembers.getJSONObject(j);
				        String id = pcMember.getString(Constant3DEXP.ID);
				        listClassId.add(id);
				    }
			    } 
			}
		}
		else {
			throw new Exception ("Errore: Engineering Item "+scopedEngItem+" non classificato");
		}
		
		return listClassId;
	}
	
	public static String retriveMfnType(JSONObject mfnItem) throws Exception {
		String mfnType = "";
		JSONArray members = mfnItem.getJSONArray(Constant3DEXP.MEMBER);
		if(members.length()>0 && !members.isEmpty()) {
			JSONObject firstMember = members.getJSONObject(0);
			mfnType= firstMember.getString(Constant3DEXP.TYPE);
		}
		return mfnType;
	}
	
	public static JSONObject buildClassifyJson(String classID, String type, String identifier, String relativePath) {
        JSONObject root = new JSONObject();
        root.put(Constant3DEXP.CLASSID, classID);

        // Array ObjectsToClassify
        JSONArray objectsArray = new JSONArray();
        JSONObject objectToClassify = new JSONObject();
        
        objectToClassify.put(Constant3DEXP.SOURCE, Constant3DEXP._3DSPACE_URL);
        objectToClassify.put(Constant3DEXP.TYPE, type);
        objectToClassify.put(Constant3DEXP.IDENTIFIER, identifier);
        objectToClassify.put(Constant3DEXP.RELATIVEPATH, relativePath+identifier);

        objectsArray.put(objectToClassify);
        root.put(Constant3DEXP.OBJECTSTOCLASSIFY, objectsArray);

        return root;
    }
	
	public static boolean isMBOMrequired(JSONObject engItemDTO) throws Exception {
		boolean isRequired = false;;
		JSONArray memberArray = engItemDTO.getJSONArray(Constant3DEXP.MEMBER);
		if (memberArray != null && memberArray.length()>0 ) {
		    JSONObject firstMember = memberArray.getJSONObject(0);
		    if(firstMember!=null) {
		    	JSONObject customAttribute = firstMember.getJSONObject(Constant3DEXP.ENTERPRISEATTRIBUTE_ENG);
		    	String isMBOMRequired = customAttribute.getString(Constant3DEXP.ATTRIBUTE_COMAU_ENG_ISMBOMREQUIRED);
		    	if(isMBOMRequired.equalsIgnoreCase(Constant3DEXP.RANGEVALUE_YES)) {
		    		isRequired = true;
		    	}
		    }
		}
		return isRequired;
	}
	
	public static String retriveChangeId(JSONObject impactChange) throws Exception {
		String changeId = "";
		JSONArray member = impactChange.getJSONArray(Constant3DEXP.MEMBER);
		if(!member.isEmpty() && member.length()>0) {
			JSONObject members = member.getJSONObject(0);
			JSONArray impactingObjects = members.getJSONArray(Constant3DEXP.CHANGE_IMPACTINGOBJECTS);
			if(impactingObjects.length()>0 && !impactingObjects.isEmpty()) {
				for(int i = 0; i<impactingObjects.length(); i++)
				{
					JSONObject impactedObject = impactingObjects.getJSONObject(i);
					String impactType = impactedObject.getString(Constant3DEXP.CHANGE_IMPACTTYPE);
					if(impactType.equalsIgnoreCase(Constant3DEXP.REALIZED))
					{
						JSONObject referencedObject = impactedObject.getJSONObject(Constant3DEXP.REFERENCEDOBJECT);
						String type = referencedObject.getString(Constant3DEXP.TYPE);
						if(type.equalsIgnoreCase(Constant3DEXP.TYPE_CHANGEACTION))
						{
							changeId = referencedObject.getString(Constant3DEXP.IDENTIFIER);
						}
					}
				}
			}
			
		}
		
		return changeId;
	}
	
	public static String retriveChangeInfo(JSONObject changeDetail, String infoReturned, String inforequested) {
		if(inforequested.equals(Constant3DEXP.TITLE)) {
			infoReturned = changeDetail.getString(Constant3DEXP.TITLE);
		}else if(inforequested.equals(Constant3DEXP.NAME)) {
			infoReturned = changeDetail.getString(Constant3DEXP.NAME);
		}
		else if(inforequested.equals(Constant3DEXP.ATTRIBUTE_COMAU_CA_MBOMMIRRORBEHAVIOR)) {
			JSONObject ChangeActionEnterpriseAttributes = changeDetail.getJSONObject(Constant3DEXP.ENTERPRISEATTRIBUTE_CA);
			infoReturned = ChangeActionEnterpriseAttributes.getString(Constant3DEXP.ATTRIBUTE_COMAU_CA_MBOMMIRRORBEHAVIOR);
		}
		else if(inforequested.equals(Constant3DEXP.ATTRIBUTE_COMAU_CABS_KITMANAGEMENT)) {
			JSONObject ChangeActionEnterpriseAttributes = changeDetail.getJSONObject(Constant3DEXP.ENTERPRISEATTRIBUTE_CA);
			infoReturned = ChangeActionEnterpriseAttributes.getString(Constant3DEXP.ATTRIBUTE_COMAU_CABS_KITMANAGEMENT);
		}
		else if(inforequested.equals(Constant3DEXP.ATTRIBUTE_COMAU_CABS_RELATEDENGID)) {
			JSONObject ChangeActionEnterpriseAttributes = changeDetail.getJSONObject(Constant3DEXP.ENTERPRISEATTRIBUTE_CA);
			infoReturned = ChangeActionEnterpriseAttributes.getString(Constant3DEXP.ATTRIBUTE_COMAU_CABS_RELATEDENGID);
		}
		return infoReturned;
	}
	
	public static JSONObject buildChangeBody(String changeTitle, String engItemTitle, String purpose) throws Exception {
		
		JSONObject changeJSON = new JSONObject();
		changeJSON.put(Constant3DEXP.POLICY, Constant3DEXP.TYPE_CHANGEACTION);
		changeJSON.put(Constant3DEXP.TYPE, Constant3DEXP.TYPE_CHANGEACTION);
		changeJSON.put(Constant3DEXP.DESCRIPTION,changeTitle +" - "+purpose+" "+engItemTitle);
		changeJSON.put(Constant3DEXP.TITLE, changeTitle+" - "+purpose+" ");
		return changeJSON;
		
		}
	
public static void retriveMembersChange(JSONObject changeDetail, List<String> approvers,List<String> assignees) throws Exception {
	
	JSONObject members = changeDetail.getJSONObject(Constant3DEXP.MEMBERS);
	JSONArray listAssignees = members.getJSONArray(Constant3DEXP.ASSIGNEES);
	if(!members.isEmpty()) {
		if(!listAssignees.isEmpty()) {
			for(int i = 0; i<listAssignees.length(); i++) {
				assignees.add(listAssignees.getString(i));
			}
		}
		assignees.add(Constant3DEXP.USERAGENT);
			
		JSONArray listReviewers = members.getJSONArray(Constant3DEXP.REVIEWERS);
		if(!listReviewers.isEmpty()) {
			for(int i = 0; i<listReviewers.length(); i++) {
				approvers.add(listReviewers.getString(i));
			}
		}
		approvers.add(Constant3DEXP.USERAGENT);
		}
	}
	

/*public static JSONObject updateChangeActionJson(String cestamp, List<String> assignees, List<String> reviewers, String identifier, String mirrorMBOMBheavior,
		String changeName, String engChangeId, String KITCostruction, String relatedEngId, String objectType) {
        JSONObject root = new JSONObject();
        root.put(Constant3DEXP.CESTAMP, cestamp);
        root.put(Constant3DEXP.DESCRIPTION, "engineering CA reference ="+changeName+"-"+engChangeId);
        JSONObject customAttr = new JSONObject();
        if(!mirrorMBOMBheavior.equals("")) {
        	customAttr.put(Constant3DEXP.ATTRIBUTE_COMAU_CA_MBOMMIRRORBEHAVIOR, mirrorMBOMBheavior);
        	//customAttr.put("COMAU_CA_RelatedEngCA", changeName+"-"+engChangeId);
        }
        if(!KITCostruction.equals("")) {
        	customAttr.put(Constant3DEXP.ATTRIBUTE_COMAU_CABS_KITMANAGEMENT, KITCostruction);
        }
        if(!relatedEngId.equals("")) {
        	customAttr.put(Constant3DEXP.ATTRIBUTE_COMAU_CABS_RELATEDENGID, relatedEngId);
        }
    	root.put(Constant3DEXP.ENTERPRISEATTRIBUTE_CA, customAttr);
        
        // Array principale Constant3DEXP.ADD
        JSONArray addArray = new JSONArray();
        JSONObject addObject = new JSONObject();

        // Sezione Constant3DEXP.MEMBERS
        JSONArray membersArray = new JSONArray();

        
        JSONObject assigneesObj = new JSONObject();
        JSONObject reviewersObj = new JSONObject();
        List<String> listUserAgent = new ArrayList<>();
        if(mirrorMBOMBheavior.equalsIgnoreCase(Constant3DEXP.RANGEVALUE_CREATEMBOM)) {
        	// Assignees
        	assigneesObj.put(Constant3DEXP.ASSIGNEES, assignees);
            membersArray.put(assigneesObj);

            // Reviewers       
            reviewersObj.put(Constant3DEXP.REVIEWERS, reviewers);
            membersArray.put(reviewersObj);
        }
        else if(mirrorMBOMBheavior.equalsIgnoreCase(Constant3DEXP.RANGEVALUE_CREATEANDRELEASEMBOM)) {
        	listUserAgent.add(Constant3DEXP.USERAGENT);
        	// Assignees
        	assigneesObj.put(Constant3DEXP.ASSIGNEES, listUserAgent);
            membersArray.put(assigneesObj);

            // Reviewers       
            reviewersObj.put(Constant3DEXP.REVIEWERS, listUserAgent);
            membersArray.put(reviewersObj);
        }
        
        if(KITCostruction.equalsIgnoreCase(Constant3DEXP.RANGEVALUE_CREATEKIT)) {
        	// Assignees
        	assigneesObj.put(Constant3DEXP.ASSIGNEES, assignees);
            membersArray.put(assigneesObj);

            // Reviewers       
            reviewersObj.put(Constant3DEXP.REVIEWERS, reviewers);
            membersArray.put(reviewersObj);
        }
        else if(KITCostruction.equalsIgnoreCase(Constant3DEXP.RANGEVALUE_CREATEANDRELEASEKIT)) {
        	listUserAgent.add(Constant3DEXP.USERAGENT);
        	// Assignees
        	assigneesObj.put(Constant3DEXP.ASSIGNEES, listUserAgent);
            membersArray.put(assigneesObj);

            // Reviewers       
            reviewersObj.put(Constant3DEXP.REVIEWERS, listUserAgent);
            membersArray.put(reviewersObj);
        }

        addObject.put(Constant3DEXP.MEMBERS, membersArray);

        // Sezione Constant3DEXP.PROPOSEDCHANGE
        JSONArray proposedChangesArray = new JSONArray();
        JSONObject proposedChangeObj = new JSONObject();

        // Oggetto Constant3DEXP.WHERE
        JSONObject whereObj = new JSONObject();
        whereObj.put(Constant3DEXP.TYPE, objectType);
        whereObj.put(Constant3DEXP.RELATIVEPATH, Constant3DEXP.RELATIVEPATH_MFGITEM + identifier);
        whereObj.put(Constant3DEXP.IDENTIFIER, identifier);

        proposedChangeObj.put(Constant3DEXP.WHERE, whereObj);
        proposedChangeObj.put(Constant3DEXP.TARGET, Constant3DEXP.CURRENTVERSION);

        // Sezione Constant3DEXP.WHATS
        JSONArray whatsArray = new JSONArray();
        JSONObject whatObj = new JSONObject();
        whatObj.put(Constant3DEXP.WHAT, Constant3DEXP.CHANGEMATURITYRELEASE);
        whatsArray.put(whatObj);

        proposedChangeObj.put(Constant3DEXP.WHATS, whatsArray);
        proposedChangesArray.put(proposedChangeObj);

        addObject.put(Constant3DEXP.PROPOSEDCHANGES, proposedChangesArray);

        // Inserisci addObject in addArray
        addArray.put(addObject);
        root.put(Constant3DEXP.ADD, addArray);

        return root;
    }*/

public static JSONObject updateChangeActionJson(String cestamp, List<String> assignees, List<String> reviewers, String identifier, String mirrorMBOMBheavior,
		String changeName, String engChangeId, String KITCostruction, String relatedEngId, String objectType, String needs) {
        JSONObject root = new JSONObject();
        root.put(Constant3DEXP.CESTAMP, cestamp);
        root.put(Constant3DEXP.DESCRIPTION, "engineering CA reference ="+changeName+"-"+engChangeId);
        if(needs.equals("CUSTOMATTR")) {
        	JSONObject customAttr = new JSONObject();
        if(!mirrorMBOMBheavior.equals("")) {
        	customAttr.put(Constant3DEXP.ATTRIBUTE_COMAU_CA_MBOMMIRRORBEHAVIOR, mirrorMBOMBheavior);
        	//customAttr.put("COMAU_CA_RelatedEngCA", changeName+"-"+engChangeId);
        }
        if(!KITCostruction.equals("")) {
        	customAttr.put(Constant3DEXP.ATTRIBUTE_COMAU_CABS_KITMANAGEMENT, KITCostruction);
        }
        if(!relatedEngId.equals("")) {
        	customAttr.put(Constant3DEXP.ATTRIBUTE_COMAU_CABS_RELATEDENGID, relatedEngId);
        }
    	root.put(Constant3DEXP.ENTERPRISEATTRIBUTE_CA, customAttr);
        
        
        }
        // Array principale Constant3DEXP.ADD
        if(needs.equals("PROPOSEDCHANGE") || needs.equals("USER")) {
        	JSONArray addArray = new JSONArray();
            JSONObject addObject = new JSONObject();

            // Sezione Constant3DEXP.MEMBERS
            JSONArray membersArray = new JSONArray();

            
            JSONObject assigneesObj = new JSONObject();
            JSONObject reviewersObj = new JSONObject();
    		if(needs.equals("USER")){
    			List<String> listUserAgent = new ArrayList<>();
    			if(mirrorMBOMBheavior.equalsIgnoreCase(Constant3DEXP.RANGEVALUE_CREATEMBOM)) {
    				// Assignees
    				assigneesObj.put(Constant3DEXP.ASSIGNEES, assignees);
    				membersArray.put(assigneesObj);
    	
    				// Reviewers       
    				reviewersObj.put(Constant3DEXP.REVIEWERS, reviewers);
    				membersArray.put(reviewersObj);
    			}
    			else if(mirrorMBOMBheavior.equalsIgnoreCase(Constant3DEXP.RANGEVALUE_CREATEANDRELEASEMBOM)) {
    				listUserAgent.add(Constant3DEXP.USERAGENT);
    				// Assignees
    				assigneesObj.put(Constant3DEXP.ASSIGNEES, listUserAgent);
    				membersArray.put(assigneesObj);
    	
    				// Reviewers       
    				reviewersObj.put(Constant3DEXP.REVIEWERS, listUserAgent);
    				membersArray.put(reviewersObj);
    			}
    			
    			if(KITCostruction.equalsIgnoreCase(Constant3DEXP.RANGEVALUE_CREATEKIT)) {
    				// Assignees
    				assigneesObj.put(Constant3DEXP.ASSIGNEES, assignees);
    				membersArray.put(assigneesObj);
    	
    				// Reviewers       
    				reviewersObj.put(Constant3DEXP.REVIEWERS, reviewers);
    				membersArray.put(reviewersObj);
    			}
    			else if(KITCostruction.equalsIgnoreCase(Constant3DEXP.RANGEVALUE_CREATEANDRELEASEKIT)) {
    				listUserAgent.add(Constant3DEXP.USERAGENT);
    				// Assignees
    				assigneesObj.put(Constant3DEXP.ASSIGNEES, listUserAgent);
    				membersArray.put(assigneesObj);
    	
    				// Reviewers       
    				reviewersObj.put(Constant3DEXP.REVIEWERS, listUserAgent);
    				membersArray.put(reviewersObj);
    			}
    	
    			addObject.put(Constant3DEXP.MEMBERS, membersArray);
    		
    		}
            
    		if(needs.equals("PROPOSEDCHANGE")){
    			// Sezione Constant3DEXP.PROPOSEDCHANGE
    			JSONArray proposedChangesArray = new JSONArray();
    			JSONObject proposedChangeObj = new JSONObject();
    	
    			// Oggetto Constant3DEXP.WHERE
    			JSONObject whereObj = new JSONObject();
    			whereObj.put(Constant3DEXP.TYPE, objectType);
    			whereObj.put(Constant3DEXP.RELATIVEPATH, Constant3DEXP.RELATIVEPATH_MFGITEM + identifier);
    			whereObj.put(Constant3DEXP.IDENTIFIER, identifier);
    	
    			proposedChangeObj.put(Constant3DEXP.WHERE, whereObj);
    			proposedChangeObj.put(Constant3DEXP.TARGET, Constant3DEXP.CURRENTVERSION);
    	
    			// Sezione Constant3DEXP.WHATS
    			JSONArray whatsArray = new JSONArray();
    			JSONObject whatObj = new JSONObject();
    			whatObj.put(Constant3DEXP.WHAT, Constant3DEXP.CHANGEMATURITYRELEASE);
    			whatsArray.put(whatObj);
    	
    			proposedChangeObj.put(Constant3DEXP.WHATS, whatsArray);
    			proposedChangesArray.put(proposedChangeObj);
    	
    			addObject.put(Constant3DEXP.PROPOSEDCHANGES, proposedChangesArray);
    		}
            

            // Inserisci addObject in addArray
            addArray.put(addObject);
            root.put(Constant3DEXP.ADD, addArray);
        }
        return root;
    }

public static JSONObject buildJsonChangeOwner(String company, String collaborativeSpace, String user, String changeId) {
	JSONObject changeOwnerJson = new JSONObject();
	changeOwnerJson.put(Constant3DEXP.OWNER, user);
	changeOwnerJson.put(Constant3DEXP.ORGANIZATION, company);
	changeOwnerJson.put(Constant3DEXP.COLLABSPACE, collaborativeSpace);
	JSONArray dataArray = new JSONArray();
	JSONObject identifiedJson = new JSONObject();
	identifiedJson.put(Constant3DEXP.ID,changeId);
	identifiedJson.put(Constant3DEXP.IDENTIFIER,changeId);
	identifiedJson.put(Constant3DEXP.TYPE,Constant3DEXP.TYPE_CHANGEACTION);
	identifiedJson.put(Constant3DEXP.SOURCE,Constant3DEXP._3DSPACE_URL);
	identifiedJson.put(Constant3DEXP.RELATIVEPATH,Constant3DEXP.RELATIVEPATH_CHANGE+changeId);
	dataArray.put(identifiedJson);
	
	changeOwnerJson.put(Constant3DEXP.DATA, dataArray);
	
	return changeOwnerJson;
	}

public static JSONObject createIssueJson( String title, List<String> listAssignee,List<String> listReviewers ) {
    JSONObject json = new JSONObject();

    // Imposta i campi principali
    json.put(Constant3DEXP.DESCRIPTION, title+" - CA Mirroring Collector");
    json.put(Constant3DEXP.TITLE, title);
    json.put(Constant3DEXP.WHITAPPROVAL, Constant3DEXP.VALUE_FALSE);

    // Crea l'oggetto Constant3DEXP.MEMBERS
    JSONObject members = new JSONObject();

    members.put(Constant3DEXP.ASSIGNEES, listAssignee);
    members.put(Constant3DEXP.CONTRIBUTORS, listReviewers);

    json.put(Constant3DEXP.MEMBERS, members);

    return json;
}

public static JSONObject generateStateChangeJson(String id, String nextState) throws Exception {
    JSONObject root = new JSONObject();
    JSONArray dataArray = new JSONArray();

    JSONObject item = new JSONObject();
    item.put(Constant3DEXP.ID, id);
    item.put(Constant3DEXP.NEXTSTATE, nextState);

    dataArray.put(item);
    root.put(Constant3DEXP.DATA, dataArray);

    return root;
}

public static JSONObject composePatchJson(String cestamp, List<String> changeActionIdentifiers) {
    JSONObject root = new JSONObject();
    root.put(Constant3DEXP.CESTAMP, cestamp);

    JSONArray resolvedByArray = new JSONArray();
    for (String identifier : changeActionIdentifiers) {
        JSONObject resolvedByEntry = new JSONObject();
        resolvedByEntry.put(Constant3DEXP.SOURCE, Constant3DEXP._3DSPACE_URL);
        resolvedByEntry.put(Constant3DEXP.TYPE, Constant3DEXP.TYPE_CHANGEACTION);
        resolvedByEntry.put(Constant3DEXP.IDENTIFIER, identifier);
        resolvedByEntry.put(Constant3DEXP.RELATIVEPATH, Constant3DEXP.RELATIVEPATH_CHANGE + identifier);
        resolvedByArray.put(resolvedByEntry);
    }

    JSONObject addEntry = new JSONObject();
    addEntry.put(Constant3DEXP.RESOLVEDBY, resolvedByArray);

    JSONArray addArray = new JSONArray();
    addArray.put(addEntry);

    root.put(Constant3DEXP.ADD, addArray);

    return root;
}

    public static JSONObject createKitJson(String mfgType, String itemLevel, String itemType, String Title) throws Exception
    {
    	JSONObject json = new JSONObject();
    	JSONArray members = new JSONArray();
    	JSONObject item = new JSONObject();
    	item.put(Constant3DEXP.TYPE, mfgType);
    	JSONObject attributesJson = new JSONObject();
    	attributesJson.put(Constant3DEXP.TITLE, Title);
    	attributesJson.put(Constant3DEXP.DESCRIPTION, Constant3DEXP.VALUE_EMPTY);
    	
    	JSONObject enterpriseAttrJSON = new JSONObject();
    	enterpriseAttrJSON.put(Constant3DEXP.ATTRIBUTE_COMAU_MFG_ITEMLEVEL, itemLevel);
    	enterpriseAttrJSON.put(Constant3DEXP.ATTRIBUTE_COMAU_MFG_ITEMTYPE, itemType);
    	
    	attributesJson.put(Constant3DEXP.ENTERPRISEATTRIBUTE_MFG,enterpriseAttrJSON);
    	
    	item.put(Constant3DEXP.ATTRIBUTES, attributesJson);
    	
    	members.put(item);
    	json.put(Constant3DEXP.ITEMS, members);
    	
    	return json;
    }
    
    public static boolean isKITrequired(JSONObject engItemDTO) throws Exception {
		boolean isRequired = false;;
		JSONArray memberArray = engItemDTO.getJSONArray(Constant3DEXP.MEMBER);
		if (memberArray != null && memberArray.length()>0 ) {
		    JSONObject firstMember = memberArray.getJSONObject(0);
		    if(firstMember!=null) {
		    	JSONObject customAttribute = firstMember.getJSONObject(Constant3DEXP.ENTERPRISEATTRIBUTE_ENG);
		    	String isMBOMRequired = customAttribute.getString(Constant3DEXP.ATTRIBUTE_COMAU_ENG_ISKITREQUIRED);
		    	if(isMBOMRequired.equalsIgnoreCase(Constant3DEXP.RANGEVALUE_YES)) {
		    		isRequired = true;
		    	}
		    }
		}
		return isRequired;
	}
    
    public static String retriveMfgId(JSONObject mfnJSON) throws Exception {
		int totItems = mfnJSON.getInt(Constant3DEXP.TOTALITEMS);
		String kitId ="";
		if(totItems == 1) {
			JSONArray membersArray = mfnJSON.getJSONArray(Constant3DEXP.MEMBER);
			JSONObject resultMember = membersArray.getJSONObject(0);
			kitId = resultMember.getString(Constant3DEXP.ID);
		}
		return kitId;
	}
    
    
    public static String retriveMBOMMirror(JSONObject changePromoted) throws Exception {
    	String MBOMAction = "";
    	if(changePromoted.has(Constant3DEXP.ENTERPRISEATTRIBUTE_CA))
    	{
    		JSONObject customerAttr = changePromoted.getJSONObject(Constant3DEXP.ENTERPRISEATTRIBUTE_CA);
            if(customerAttr.has(Constant3DEXP.ATTRIBUTE_COMAU_CA_MBOMMIRRORBEHAVIOR))
            {
            	MBOMAction = customerAttr.getString(Constant3DEXP.ATTRIBUTE_COMAU_CA_MBOMMIRRORBEHAVIOR);
            }
    	}
        return MBOMAction;
    }
    
    public static String proposedManufacturingItemId (JSONObject changeActionjson, String typeRequested) throws Exception {
    	String rootMfnId = "";
    	if(changeActionjson.has(Constant3DEXP.PROPOSEDCHANGES)) {
    		JSONArray listProposedChange = changeActionjson.getJSONArray(Constant3DEXP.PROPOSEDCHANGES);
    		if(listProposedChange!=null && !listProposedChange.isEmpty() && listProposedChange.length()>0)
    		{
    			JSONObject proposedChange = listProposedChange.getJSONObject(0);
    			JSONObject where = proposedChange.getJSONObject(Constant3DEXP.WHERE);
    			String type = where.getString(Constant3DEXP.TYPE);
    			if(type.equalsIgnoreCase(typeRequested)) {
    				rootMfnId = where.getString(Constant3DEXP.IDENTIFIER);
    				
    			}
    		}
    	}
    	return rootMfnId;	
    }
    
    public static JSONObject buildJsonBodyExpand() throws Exception {
		JSONObject expandJson = new JSONObject();
		expandJson.put(Constant3DEXP.EXPANDDEPTH,-1);
		expandJson.put(Constant3DEXP.WITHPATH, true);
		return expandJson;
	}
    
    public static JSONObject buildJsonBodyExpandFirstLevel() throws Exception {
		JSONObject expandJson = new JSONObject();
		expandJson.put(Constant3DEXP.EXPANDDEPTH,1);
		expandJson.put(Constant3DEXP.WITHPATH, true);
		return expandJson;
	}
    
    public static String retriveManufacturingItemInfo(JSONObject mfnItem, String infoRequested) throws Exception {
    	JSONArray member = mfnItem.getJSONArray(Constant3DEXP.MEMBER);
		JSONObject firstMember = member.getJSONObject(0);
		String infoReturned = "";
		if(infoRequested.equals(Constant3DEXP.TYPE)) {
			String type = firstMember.getString(Constant3DEXP.TYPE);
			infoReturned = type;
		}
		if(infoRequested.equals(Constant3DEXP.STATE)) {
			String state = firstMember.getString(Constant3DEXP.STATE);
			infoReturned = state;
		}
		else if(infoRequested.equals(Constant3DEXP.CESTAMP)) {
			String cestamp = firstMember.getString(Constant3DEXP.CESTAMP);
			infoReturned = cestamp;
		}
		
		return infoReturned;
    }
    
    public static List<String> extractMBOMIds(JSONObject inputJson, String excludeId) {
        List<String> ids = new ArrayList<>();

        if (inputJson == null || !inputJson.has(Constant3DEXP.MEMBER)) {
            return ids; // ritorna lista vuota se non ci sono membri
        }

        JSONArray members = inputJson.getJSONArray(Constant3DEXP.MEMBER);
        if (members == null) {
            return ids;
        }

        for (int i = 0; i < members.length(); i++) {
            JSONObject memberObj = members.getJSONObject(i);
            if (memberObj != null && memberObj.has(Constant3DEXP.TYPE)) {
                String type = memberObj.getString(Constant3DEXP.TYPE);
                String id = memberObj.getString(Constant3DEXP.ID);

                // Filtra per type e id diverso da excludeId
                if ((type.equalsIgnoreCase(Constant3DEXP.TYPE_CREATEASSEMBLY) || type.equalsIgnoreCase(Constant3DEXP.TYPE_PROVIDE))
                        && !id.equals(excludeId)) {
                	if(!ids.contains(id)) {
                		ids.add(id);
                	}
 
                }
            }
        }

        return ids;
    }
    
public static JSONObject generateProposedChangeArray(String childid, String type) throws Exception {
		
		JSONObject firstProposed = new JSONObject();
		firstProposed.put(Constant3DEXP.TARGET, Constant3DEXP.CURRENTVERSION);
		
		JSONObject where = new JSONObject();
		where.put(Constant3DEXP.IDENTIFIER, childid);
		where.put(Constant3DEXP.RELATIVEPATH, Constant3DEXP.RELATIVEPATH_MFGITEM+childid);
		where.put(Constant3DEXP.TYPE, type);
		
		firstProposed.put(Constant3DEXP.WHERE, where);
		
		JSONArray whats = new JSONArray();
		JSONObject what = new JSONObject();
		what.put(Constant3DEXP.WHAT, Constant3DEXP.CHANGEMATURITYRELEASE);
		whats.put(what);
		firstProposed.put(Constant3DEXP.WHATS, whats);
		
		return firstProposed;
	}

	public static JSONObject addProposedChangeBody(String cestampChange, JSONArray proposedChangeArray) throws Exception {
		
		JSONObject bodyRequestProposedChange = new JSONObject();
		JSONObject proposedChange = new JSONObject();
		JSONArray addArray = new JSONArray();
		
		bodyRequestProposedChange.put(Constant3DEXP.CESTAMP, cestampChange);
		
		proposedChange.put(Constant3DEXP.PROPOSEDCHANGES, proposedChangeArray);
		addArray.put(proposedChange);
		
		bodyRequestProposedChange.put(Constant3DEXP.ADD, addArray);
		
		return bodyRequestProposedChange;
	}
	
	public static JSONObject approveChangeBody(JSONObject changeDetail) throws Exception {
		JSONObject members = changeDetail.getJSONObject(Constant3DEXP.MEMBERS);  
		JSONArray listReviewers = members.getJSONArray(Constant3DEXP.REVIEWERS);
		String cestamp= changeDetail.getString(Constant3DEXP.CESTAMP);
		
		JSONObject approveChangeJson = new JSONObject();
		
		if(!listReviewers.isEmpty()) {
			if(listReviewers.length()==1) {
				String reviewer = listReviewers.getString(0);
				if(reviewer.equalsIgnoreCase(Constant3DEXP.USERAGENT)) {
					
					approveChangeJson.put(Constant3DEXP.CESTAMP, cestamp);
					approveChangeJson.put(Constant3DEXP.COMMENT, "Change promoted by User Agent");
					
				}
			}
			
		}
		
		return approveChangeJson;
	}
    
	public static String findPreviousRevision(String acturalRevSeq) throws Exception {
		String previousRev = "";
		
		for(int i = 0; i<acturalRevSeq.length(); i++)
		{
			String tempRev = Constant3DEXP.COMAU_REVISIONSEQUENCE[i];
			if(acturalRevSeq.equalsIgnoreCase(tempRev)) {
				previousRev = Constant3DEXP.COMAU_REVISIONSEQUENCE[i - 1];
			}
		}
		return previousRev;
	}

	public static JSONObject jsonExpandRevisions(String subjectId) {
		
		JSONObject data = new JSONObject();
		JSONArray dataArray = new JSONArray();
		JSONObject subjectIdJson = new JSONObject();
		subjectIdJson.put(Constant3DEXP.ID, subjectId);
		dataArray.put(subjectIdJson);
		data.put(Constant3DEXP.DATA, dataArray);
		
		return data;
	}

	public static String retrivePreviousMFGRevision(JSONObject objectRevisions, String mfgId) {
		// TODO Auto-generated method stub
		String previousRevId = "";
		JSONArray results = objectRevisions.getJSONArray("results");
		if(results.length()>0) {
			
			JSONObject result = results.getJSONObject(0);
			JSONArray version = result.getJSONArray("versions");
			
			if(version.length()>1) {
				
				for(int i = 0; i<version.length(); i++) {
					
					JSONObject tempVersion = version.getJSONObject(i);
					String identifier = tempVersion.getString(Constant3DEXP.IDENTIFIER);
					
					if(identifier.equalsIgnoreCase(mfgId)) {
						
						if(tempVersion.has("ancestors")) {
							
							JSONArray ancestor = tempVersion.getJSONArray("ancestors");
							JSONObject tempAncestor = ancestor.getJSONObject(0);
							previousRevId = tempAncestor.getString(Constant3DEXP.IDENTIFIER);
							
						}
						
					}
				}
			}
			
		}
		
		return previousRevId;
	}

	public static String getSuccessiveRevision(JSONObject objectEngRevisions, String engId) {
		String successiveRevId = "";
		JSONArray results = objectEngRevisions.getJSONArray("results");
		if(results.length()>0) {
			
			JSONObject result = results.getJSONObject(0);
			JSONArray version = result.getJSONArray("versions");
			
			if(version.length()>1) {
				
				for(int i = 0; i<version.length(); i++) {
					
					JSONObject tempVersion = version.getJSONObject(i);
					
					if(tempVersion.has("ancestors")) {
						
						JSONArray ancestor = tempVersion.getJSONArray("ancestors");
						JSONObject singleAncestor = ancestor.getJSONObject(0);
						String ancestorId = singleAncestor.getString(Constant3DEXP.IDENTIFIER);
						
						if(ancestorId.equalsIgnoreCase(engId)) {
							successiveRevId = tempVersion.getString(Constant3DEXP.IDENTIFIER);
						}	
					}	
				}
			}			
		}		
		return successiveRevId;		
	}

	public static Map<String, Integer> retriveObjectQuantityMap(JSONObject expandOneLevel, String rootId) {
		
		Map<String, Integer> objectQuantityMap = new HashMap<>();
		
		JSONArray members = expandOneLevel.getJSONArray(Constant3DEXP.MEMBER);
		
		Set<String> vpmReferenceIds = new HashSet<>();
        for (int i = 0; i < members.length(); i++) {
        	
            JSONObject member = members.getJSONObject(i);
            if(member.has("type")) {
            	
            	String type = member.getString("type");
                if ("VPMReference".equals(type)) {
                	
                    String id = member.getString("id");
                    if (!id.isEmpty() && !rootId.equals(id)) {
                    	
                        vpmReferenceIds.add(id);
                    }
                }
            }  
        }
		
		JSONArray member = expandOneLevel.getJSONArray(Constant3DEXP.MEMBER);
		if(!member.isEmpty()) {
			
			for (String element : vpmReferenceIds) {
				Integer counter = 0;
				
				for(int j = 0; j<member.length(); j++) {
					JSONObject tempMember = member.getJSONObject(j);
					if(tempMember.has("Path")) {
						
						JSONArray path = tempMember.getJSONArray("Path");
						
						String secLevelId = path.getString(2);
						
						if(element.equals(secLevelId)) {
							
							counter = counter +1;
						}
					}
				}
				
			objectQuantityMap.put(element, counter);		
	
			}
			
		}
		return objectQuantityMap;
	}

	public static Map<String, Integer> retriveEBOMDifferences(Map<String, Integer> oldQuantities,
                                                       Map<String, Integer> newQuantities) {
        Map<String, Integer> diffs = new HashMap<>();

        Map<String, Integer> oldMap = (oldQuantities != null) ? oldQuantities : new HashMap<>();
        Map<String, Integer> newMap = (newQuantities != null) ? newQuantities : new HashMap<>();

        // Unione delle chiavi
        Set<String> allIds = new HashSet<>();
        allIds.addAll(oldMap.keySet());
        allIds.addAll(newMap.keySet());

        for (String id : allIds) {
            boolean inOld = oldMap.containsKey(id);
            boolean inNew = newMap.containsKey(id);

            if (inNew && !inOld) {
                // Solo nella Nuova mappa
                diffs.put(id, 1);
            } else if (!inNew && inOld) {
                // Solo nella Vecchia mappa
                diffs.put(id, -1);
            } else {
                // Presente in entrambe
                int oldQty = safeGet(oldMap, id);
                int newQty = safeGet(newMap, id);

                if (newQty == oldQty) {
                    diffs.put(id, 0);
                } else {
                    diffs.put(id, newQty - oldQty); // differenza effettiva
                }
            }
        }

        return diffs;
    }
	

	private static int safeGet(Map<String, Integer> map, String key) {
        Integer v = map.get(key);
        return (v == null) ? 0 : v;
    }

	public static String retrivedMfnItemId(JSONObject scopedMfnItem) {
		
		String identifier = "";
		
		if(scopedMfnItem.has(Constant3DEXP.MEMBER)) {
			
			JSONArray member = scopedMfnItem.getJSONArray(Constant3DEXP.MEMBER);
			
			JSONObject firstMember = member.getJSONObject(0);
			
			identifier = firstMember.getString(Constant3DEXP.IDENTIFIER);
			
			
		}
		
		return identifier;
	}

	public static JSONObject createMfnInstanceBody(String mfnId) {
		
		JSONObject instanceCreate = new JSONObject();
		
		JSONArray instances = new JSONArray();
		
		JSONObject object = new JSONObject();
		
		JSONObject referencedObject = new JSONObject();
		referencedObject.put(Constant3DEXP.IDENTIFIER, mfnId);
		
		object.put("referencedObject",referencedObject);
		instances.put(object);
		
		instanceCreate.put("instances", instances);
		
		return instanceCreate;
	}

	public static JSONObject buildJsonModifyMfnUnderChange(String cestamp, String removedQuantity) {
		
		JSONObject requestBody = new JSONObject();
		requestBody.put(Constant3DEXP.CESTAMP, cestamp);
		requestBody.put(Constant3DEXP.DESCRIPTION, removedQuantity);
		
		return requestBody;
	}

}
