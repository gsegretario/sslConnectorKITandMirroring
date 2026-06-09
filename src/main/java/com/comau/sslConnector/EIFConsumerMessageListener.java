package com.comau.sslConnector;

import java.net.http.HttpClient;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ClientImplementation.Client3DXImpl;
import ClientImplementation.Client3dExperienceFactory;
import ClientImplementation.Constant3DEXP;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TextMessage;
import jsonHelper.jsonBuilder;
import session.SessionManager;

public class EIFConsumerMessageListener implements MessageListener {

	
	// ======================================================
//  Executor dedicato ai retry asincroni (thread parallelo)
// ======================================================
	private static final ScheduledExecutorService scheduler =
			Executors.newScheduledThreadPool(1);
	private static final Logger logger = LoggerFactory.getLogger(EIFConsumerMessageListener.class);
	private final HttpClient httpClient = HttpClient.newHttpClient();
	private static volatile long lastMessageTime = System.currentTimeMillis();
	private final Runnable onMessageCallback = () -> {
	};
	private String role = "VPLMProjectLeader";
	

	public void onMessage(Message message) {
		lastMessageTime = System.currentTimeMillis();
		try {
	        onMessageCallback.run();

	        if (message instanceof TextMessage) {
	            String body = ((TextMessage) message).getText();
	            logger.info("📩 Messaggio ricevuto:\n{}", body);

	            ObjectMapper mapper = new ObjectMapper();
	            JsonNode json = mapper.readTree(body);

	            String eventType = json.at("/data/eventType").asText();
	            String subjectType = json.at("/data/subject/type").asText();
	            String subjectId = json.at("/data/subject/identifier").asText();
	            String nextState = json.at("/data/object/Value").asText();
	            String eventClass = json.at("/data/eventClass").asText();
	            // Esempio di evento creazione task
	            if ("created".equals(eventType) && "Manufacturing Item".equals(eventClass)) {
	                String securityContext = json.at("/data/authorization").asText();
	                String[] secContext = securityContext.split("\\.");
	                String company = secContext[1];
	                String collaborativeSpace = secContext[2];
	                String user = json.at("/data/user").asText();
	                securityContext = role + "." + secContext[1] + "." + secContext[2];

	                logger.info("✅ - Object Detail: id=" + subjectId);
	                logger.info("✅ - Object Detail: event type=" + eventType);
	                logger.info("✅ - Object Detail: Type=" + subjectType);
	                logger.info("✅ - Object Detail: Security Context= " + securityContext);

	                try {
	                    HashMap<String, String> loginMap = new HashMap<>();
	                    loginMap.put("securityContext", securityContext);
	                    Client3DXImpl _3dsClient = SessionManager.getClient(Constant3DEXP.USERAGENT, loginMap);
	                    /*Client3DXImpl _3dsClient = Client3dExperienceFactory.newInstance();
	                    _3dsClient.init(loginMap);*/
	                    logger.info("✅ - Successful log in");
	                    try {
	                    	mfnRootManagementConRetry(subjectId, _3dsClient, company, collaborativeSpace, user);
	                    	//mfnRootManagement(subjectId, _3dsClient, company, collaborativeSpace, user);
	                    }catch (Exception e) {
		                    logger.error("❌ Errore durante la gestione del Manufacturing item "+subjectId+":", e);
		                }
	                  
	                } catch (Exception e) {
	                    logger.error("❌ Errore durante il log in:", e);
	                }
	            }/* else if ("statusChanged".equals(eventType) && Constant3DEXP.TYPE_CHANGEACTION.equals(subjectType)
	            		&& nextState.equals("In Work")) {
	                String securityContext = json.at("/data/authorization").asText();
	                String[] secContext = securityContext.split("\\.");
	                securityContext = role + "." + secContext[1] + "." + secContext[2];

	                logger.info("✅ - Object Detail: id=" + subjectId);
	                logger.info("✅ - Object Detail: event type=" + eventType);
	                logger.info("✅ - Object Detail: Type=" + subjectType);
	                logger.info("✅ - Object Detail: Security Context= " + securityContext);

	                try {
	                    HashMap<String, String> loginMap = new HashMap<>();
	                    loginMap.put("securityContext", securityContext);
	                    Client3DXImpl _3dsClient = Client3dExperienceFactory.newInstance();
	                    _3dsClient.init(loginMap);
	                    logger.info("✅ - Successful log in");
	                    try {
	                    	
	                    	JSONObject changePromoted = _3dsClient.getChangeActionById(subjectId);
	                        String MBOMAction = jsonBuilder.retriveMBOMMirror(changePromoted);
	                        
	                        if (MBOMAction.equals(Constant3DEXP.RANGEVALUE_CREATEMBOM)) {
	                        	int res = fillProposedChange(_3dsClient, changePromoted, subjectId);
	                        }
	                        else if (MBOMAction.equals(Constant3DEXP.RANGEVALUE_CREATEANDRELEASEMBOM)) {
	                        	int res = fillProposedChange(_3dsClient, changePromoted, subjectId);
	                        	if(res == 0) {
	                        		promoteChange(subjectId, _3dsClient);
		        					approveChangeAction(_3dsClient,subjectId);
	                        	}
	                        }
	                        else {
	                            logger.info("❌ - MBOM Action not needed");
	                        }
	                        
	                        String KitAction = "";
	                        KitAction = jsonBuilder.retriveChangeInfo(changePromoted, KitAction,Constant3DEXP.ATTRIBUTE_COMAU_CABS_KITMANAGEMENT);
	                        			                        
	                        if (KitAction.equals(Constant3DEXP.RANGEVALUE_CREATEKIT)) {
	                        	int res = 0;
	                        	res = findDifferences(_3dsClient, changePromoted, subjectId);
	                        }
	                        else if (KitAction.equals(Constant3DEXP.RANGEVALUE_CREATEANDRELEASEKIT)) {
	                        	int res = 0;
	                        	res = findDifferences(_3dsClient, changePromoted, subjectId);
	                        	if(res==0) {
	                        		promoteChange(subjectId, _3dsClient);
		        					approveChangeAction(_3dsClient,subjectId);
	                        	}
	                        }
	                        else {
	                            logger.info("❌ - KIT Action not needed");
	                        }	                        
	                        
	                    }catch (Exception e) {
	                        logger.error("❌ Errore durante il recupero della Change Action:", e);
	                    }
	                    
	                    
	                } catch (Exception e) {
	                    logger.error("❌ Errore durante la log in:", e);
	                }
	            }
	            else if ("versioned".equals(eventType) && "Manufacturing Item".equals(eventClass)) {
	            	String securityContext = json.at("/data/authorization").asText();
	                String[] secContext = securityContext.split("\\.");
	                securityContext = role + "." + secContext[1] + "." + secContext[2];

	                logger.info("✅ - Object Detail: id=" + subjectId);
	                logger.info("✅ - Object Detail: event type=" + eventType);
	                logger.info("✅ - Object Detail: Type=" + subjectType);
	                logger.info("✅ - Object Detail: Security Context= " + securityContext);
	                
	            	try {
	                    HashMap<String, String> loginMap = new HashMap<>();
	                    loginMap.put("securityContext", securityContext);
	                    Client3DXImpl _3dsClient = Client3dExperienceFactory.newInstance();
	                    _3dsClient.init(loginMap);
	                    logger.info("✅ - Successful log in");
	                    
	                    String company = secContext[1];
		                String collaborativeSpace = secContext[2];
		                String user = json.at("/data/user").asText();
		                
	                    mbomAndKitManagementOnRevision(subjectId, _3dsClient, company, collaborativeSpace, user);
	                    
	            	}catch (Exception e) {
                        logger.error("❌ Errore durante il recupero della Change Action:", e);
                    }
	            }*/
	            //ams purpose START
	            /*else if ("statusChanged".equals(eventType) && "Issue".equals(subjectType)
	            		&& nextState.equals("In Work")) {
	            	
	            	try {
                    	mfnRootManagement(subjectId, _3dsClient, company, collaborativeSpace, user);
                    }catch (Exception e) {
	                    logger.error("❌ Errore durante la gestione del Manufacturing item "+subjectId+":", e);
	                }
	            	
	            }*/
	            //END
	            message.acknowledge();
	        }
	    } catch (Exception e) {
	        logger.error("❌ Errore durante la gestione del messaggio:", e);
	        /*try {
	           message.acknowledge();
	        } catch (JMSException jmsEx) {
	            logger.error("❌ Errore nell'acknowledge del messaggio:", jmsEx);
	        }*/
	    }
	}


	private int findDifferences(Client3DXImpl _3dsClient, JSONObject changePromoted, String subjectId) throws Exception {
		int res = 0;
		String cestampChange = changePromoted.getString(Constant3DEXP.CESTAMP);
		
		String mfnKitId = jsonBuilder.proposedManufacturingItemId(changePromoted, Constant3DEXP.TYPE_CREATEKIT);
		if(!mfnKitId.equals("") && mfnKitId != null)
		{
			String attributeKITids = "";
			attributeKITids = jsonBuilder.retriveChangeInfo(changePromoted, attributeKITids, Constant3DEXP.ATTRIBUTE_COMAU_CABS_RELATEDENGID);
			if(attributeKITids.contains("-")) {
				String[] engIdsRevision = attributeKITids.split("-");
				
				String firstRev = engIdsRevision[0] ;
				String secondRev = engIdsRevision[1] ;
				
				if(!firstRev.isEmpty() && !secondRev.isEmpty()) {
										
					JSONObject expandBodyRequest = jsonBuilder.buildJsonBodyExpandFirstLevel();
					
					JSONObject expandFirstRev = _3dsClient.expandEngineeringItem(firstRev, expandBodyRequest);
					JSONObject expandSecondRev = _3dsClient.expandEngineeringItem(secondRev, expandBodyRequest);
					
					Map<String, Integer> objectQuantityFirst = new HashMap<>();
					Map<String, Integer> objectQuantitySecond = new HashMap<>();
					
					objectQuantityFirst = jsonBuilder.retriveObjectQuantityMap(expandFirstRev,firstRev);
					objectQuantitySecond = jsonBuilder.retriveObjectQuantityMap(expandSecondRev,secondRev);
					
					Map<String, Integer> ebomDifferences = jsonBuilder.retriveEBOMDifferences(objectQuantityFirst, objectQuantitySecond);
					
					String removedQuantity = "";
					
					for (Map.Entry<String, Integer> entry : ebomDifferences.entrySet()) {
						String engId = entry.getKey();
						Integer value = entry.getValue();
						
						if(value>0) {
							
							//generate relationship under change
							List<String> engIds = new ArrayList<String>();
							engIds.add("\""+engId+"\"");
									
							JSONObject scopedMfnItem = _3dsClient.getScopedManufacturingItem(engIds);
							String mfnId = jsonBuilder.retrivedMfnItemId(scopedMfnItem);
							
							JSONObject createMfnInstance = jsonBuilder.createMfnInstanceBody(mfnId);
							for (int i = 0; i<value; i++) {
								
								JSONObject newInstances =  _3dsClient.createNewManufacturingItemInstanceUnderChange(mfnKitId, createMfnInstance, subjectId);
							
							}
								
						}else if(value<0) {
							//generare Stringa e modificare il KIT
							JSONObject engItem = _3dsClient.getEngineeringItemById(engId);
							String engTitle = "";
							engTitle = jsonBuilder.retriveEngItemInfo(engItem, Constant3DEXP.TITLE, engTitle);
							removedQuantity = removedQuantity + "Removed "+value+" instances of "+engTitle+". \r\n";
							logger.info("✅ quantity {} of object {} removed",value,engId +" - "+engTitle);
							
						}else if(value==0) {
							//do nothing
						}
					}
					
					//patchMFNItemUnderChange
					String cestamp = "";
					JSONObject kitJson = _3dsClient.getManufacturingItem(mfnKitId);
					
					String cestampKit = jsonBuilder.retriveManufacturingItemInfo(kitJson, Constant3DEXP.CESTAMP);
					JSONObject requestBody = jsonBuilder.buildJsonModifyMfnUnderChange(cestampKit, removedQuantity);					
					JSONObject kitModified = _3dsClient.patchModifyManufacturingUnderChange(mfnKitId, requestBody, subjectId);
					

				}
				else {
					res = 1;
				}
			}
			else {
				res = 1;
			}
			
			
		}
		else {
			res = 1;
		}
		return res;
	}


	/**
	 * @param subjectId
	 * @param _3dsClient
	 * @param company
	 * @param collaborativeSpace
	 * @param user
	 * @throws Exception
	 * @throws JSONException
	 */
	private void mbomAndKitManagementOnRevision(String subjectId, Client3DXImpl _3dsClient, String company,
			String collaborativeSpace, String user) throws Exception, JSONException {
		
		JSONObject revisionsPath = jsonBuilder.jsonExpandRevisions(subjectId);
		JSONObject objectRevisions = _3dsClient.getRevisionsPath(revisionsPath);
		logger.info("✅ mbomAndKitManagementOnRevision - object revisions {}",objectRevisions.toString());
		
		String previousRevisionId = jsonBuilder.retrivePreviousMFGRevision(objectRevisions, subjectId);
		logger.info("✅ mbomAndKitManagementOnRevision - previous Revision Id {}",previousRevisionId.toString());
		
		JSONObject scopedEngItem = _3dsClient.getScopedEngItem(previousRevisionId);
		logger.info("✅ mbomAndKitManagementOnRevision - scopedEngItem {}",scopedEngItem.toString());
		
		String engId = jsonBuilder.retriveEngId(scopedEngItem);
		if(engId!=null && !engId.equals("")) {
			
			logger.info("✅ mbomAndKitManagementOnRevision - Previous Revision Eng Id ={}",engId);
			
			JSONObject revisionsEngPath = jsonBuilder.jsonExpandRevisions(engId);
		    JSONObject objectEngRevisions = _3dsClient.getRevisionsPath(revisionsEngPath);
		    
		    String revisedEngId = jsonBuilder.getSuccessiveRevision(objectEngRevisions,engId);
		    
		    String compareIds = engId + "-" +revisedEngId;
		    logger.info("✅ mbomAndKitManagementOnRevision - Previous Revision Eng Id {} Last revision eng id {}",engId,revisedEngId);
			
		    JSONObject engItemDetail = _3dsClient.getEngineeringItemById(revisedEngId);
		    String engItemTitle = "";
			engItemTitle = jsonBuilder.retriveEngItemInfo(engItemDetail,Constant3DEXP.TITLE,engItemTitle );
			logger.info("✅ mbomAndKitManagementOnRevision - engItemTitle ={}",engItemTitle);
			
		    
		    boolean isMBOMNeeded = jsonBuilder.isMBOMrequired(engItemDetail);
			boolean kitNeeded = jsonBuilder.isKITrequired(engItemDetail);
			logger.info("✅ mbomAndKitManagementOnRevision - isMBOMNeeded ="+isMBOMNeeded);
			logger.info("✅ mbomAndKitManagementOnRevision- kitNeeded ="+kitNeeded);
			String changeId = "";
			String changeMfnId = "";
			String changeKitId = "";
			List<String> approvers = new ArrayList<>();
			List<String> assignee = new ArrayList<>();
			if(isMBOMNeeded==true || kitNeeded== true) {
				String changeTitle ="";
				String changeName ="";
				String changeMirrorBehavior ="";
				String changeKITBehavior = "";
				JSONObject changeImpact = _3dsClient.getChangeImpact(revisedEngId);
				changeId = jsonBuilder.retriveChangeId(changeImpact);
				
				JSONObject changeDetail = _3dsClient.getChangeActionById(changeId);
				changeTitle = jsonBuilder.retriveChangeInfo(changeDetail,changeTitle,Constant3DEXP.TITLE);
				changeName = jsonBuilder.retriveChangeInfo(changeDetail,changeTitle,Constant3DEXP.NAME);
				
				changeMirrorBehavior = jsonBuilder.retriveChangeInfo(changeDetail,changeMirrorBehavior,Constant3DEXP.ATTRIBUTE_COMAU_CA_MBOMMIRRORBEHAVIOR);
				changeKITBehavior = jsonBuilder.retriveChangeInfo(changeDetail,changeKITBehavior,Constant3DEXP.ATTRIBUTE_COMAU_CABS_KITMANAGEMENT);
				logger.info("✅ mbomAndKitManagementOnRevision - Engineering Change Action:id {},name {},title {},mirrorBheavior {}, changeKITBehavior {}",changeId,changeName,changeTitle,changeMirrorBehavior,changeKITBehavior);
				
				if(isMBOMNeeded == true) {
					
					//subject id is already inside a change
					
					JSONObject createChange = jsonBuilder.buildChangeBody(changeTitle, engItemTitle,"MBOM Mirroring");
					JSONObject changeMfn = _3dsClient.createChangeAction(createChange);
					logger.info("✅ mbomAndKitManagementOnRevision - Manufacturing Change Action changeMfn ={}",changeMfn);
					
					changeMfnId = changeMfn.getString(Constant3DEXP.ID);
					String mfnCestamp = changeMfn.getString(Constant3DEXP.CESTAMP);
					logger.info("✅ mbomAndKitManagementOnRevision - Manufacturing Change Action id={}",changeMfnId);
					
					jsonBuilder.retriveMembersChange(changeDetail,approvers,assignee);
					logger.info("✅ mbomAndKitManagementOnRevision - MBOM approvers = {} KIT reviewers = {}",approvers,assignee);
					/*JSONObject jsonPatchModifyChange = jsonBuilder.updateChangeActionJson(mfnCestamp, assignee,approvers,subjectId,changeMirrorBehavior,
							changeName, changeId, Constant3DEXP.RANGEVALUE_DONOTHING, Constant3DEXP.VALUE_EMPTY, Constant3DEXP.TYPE_CREATEASSEMBLY);
					
					JSONObject addMembersAndItem = _3dsClient.patchChange(changeMfnId, jsonPatchModifyChange);	
					logger.info("✅ mbomAndKitManagementOnRevision - Output modify Change Action : {}",addMembersAndItem.toString());*/
					
					JSONObject jsonPatchModifyChangeCustAttr = jsonBuilder.updateChangeActionJson(mfnCestamp, assignee,approvers,subjectId,changeMirrorBehavior, changeName, changeId, 
							Constant3DEXP.RANGEVALUE_DONOTHING,Constant3DEXP.VALUE_EMPTY, Constant3DEXP.TYPE_CREATEASSEMBLY,"CUSTOMATTR");
					JSONObject addCustomAttr = _3dsClient.patchChange(changeMfnId, jsonPatchModifyChangeCustAttr);
					logger.info("✅ mbomAndKitManagementOnRevision - patch change addCustomAttr ={}",addCustomAttr);
					
					mfnCestamp = addCustomAttr.getString(Constant3DEXP.CESTAMP);
					JSONObject jsonPatchModifyChangeMember = jsonBuilder.updateChangeActionJson(mfnCestamp, assignee,approvers,subjectId,changeMirrorBehavior, changeName, changeId, Constant3DEXP.RANGEVALUE_DONOTHING,Constant3DEXP.VALUE_EMPTY, Constant3DEXP.TYPE_CREATEASSEMBLY,"USER");
					JSONObject addMember = _3dsClient.patchChange(changeMfnId, jsonPatchModifyChangeMember);		
					logger.info("✅ mbomAndKitManagementOnRevision - patch change addMember {}",addMember.toString());
					
					mfnCestamp = addMember.getString(Constant3DEXP.CESTAMP);
					JSONObject jsonPatchModifyChangeProposed = jsonBuilder.updateChangeActionJson(mfnCestamp, assignee,approvers,subjectId,
															changeMirrorBehavior, changeName, changeId, Constant3DEXP.RANGEVALUE_DONOTHING,
															Constant3DEXP.VALUE_EMPTY, Constant3DEXP.TYPE_CREATEASSEMBLY,"PROPOSEDCHANGE");
					JSONObject addProposedChange = _3dsClient.patchChange(changeMfnId, jsonPatchModifyChangeProposed);
					//JSONObject addMembersAndItem = _3dsClient.patchChange(changeMfnId, jsonPatchModifyChange);	
					logger.info("✅ mbomAndKitManagementOnRevision - patch change addProposedChange {}",addProposedChange.toString());
					
					
					JSONObject classifiedEngChange = _3dsClient.getWhereClassified(changeId);
					List<String> whereChangeClassifiedId = jsonBuilder.listclassId(classifiedEngChange);
					
					if(whereChangeClassifiedId.size()>0) {
						logger.info("✅ mbomAndKitManagementOnRevision - Engineering Change Action classification Ids ="+whereChangeClassifiedId.toString());
						for(int i = 0; i<whereChangeClassifiedId.size(); i++) {
							JSONObject classifiedChange = jsonBuilder.buildClassifyJson(whereChangeClassifiedId.get(i),Constant3DEXP.TYPE_CHANGEACTION,changeMfnId,Constant3DEXP.RELATIVEPATH_CHANGE);
							_3dsClient.classifyObject(classifiedChange);
							logger.info("✅ mbomAndKitManagementOnRevision - Manufacturing Change Action classified in "+ whereChangeClassifiedId.get(i));
						}
					}
					
					JSONObject changeOwnerJson = jsonBuilder.buildJsonChangeOwner(company, collaborativeSpace, user,changeMfnId);
					JSONObject newOwner = _3dsClient.changeOwner(changeOwnerJson);
					logger.info("✅ mbomAndKitManagementOnRevision - newOwner = {}",newOwner);
				}
				/*if(kitNeeded == true) {
					
					JSONObject createChange = jsonBuilder.buildChangeBody(changeTitle, engItemTitle,"KIT Management");
					JSONObject changeMfn = _3dsClient.createChangeAction(createChange);
					logger.info("✅ mbomAndKitManagementOnRevision - KIT created new changeMfn ={}",changeMfn.toString());
					changeKitId = changeMfn.getString(Constant3DEXP.ID);
					String mfnCestamp = changeMfn.getString(Constant3DEXP.CESTAMP);
					logger.info("✅ mbomAndKitManagementOnRevision - KIT Manufacturing Change Action od= {}",changeKitId);
					
					JSONObject kitCreateJson = jsonBuilder.createKitJson(Constant3DEXP.TYPE_CREATEKIT, "Manufacturing Kit","W - Special Component","KIT - "+engItemTitle);
					JSONObject kit = _3dsClient.createNewManufacturingItem(kitCreateJson);
					logger.info("✅ mbomAndKitManagementOnRevision - KIT created new kit ={}",kit.toString());
					
					String createdKitId = jsonBuilder.retriveMfgId(kit);
					
					JSONObject classified = _3dsClient.getWhereClassified(revisedEngId);
					List<String> whereClassifiedId = jsonBuilder.listclassId(classified);
					
					String mfnType = Constant3DEXP.TYPE_CREATEKIT;
					
					if(whereClassifiedId.size()>0) {
						logger.info("✅ mbomAndKitManagementOnRevision - KIT Revised Engineering Item classification Ids ={}",whereClassifiedId.toString());
						for(int i = 0; i<whereClassifiedId.size(); i++) {
							try {
								
								JSONObject classifiedMfn = jsonBuilder.buildClassifyJson(whereClassifiedId.get(i),mfnType,createdKitId,Constant3DEXP.RELATIVEPATH_MFGITEM);
								_3dsClient.classifyObject(classifiedMfn);
								logger.info("✅ mbomAndKitManagementOnRevision - KIT classifeid in : "+whereClassifiedId.get(i));
							
							}catch  (Exception e) {
								logger.error("❌ mbomAndKitManagementOnRevision - KIT Errore durante la classificazione del KIT = "+subjectId+" nella classe = "+whereClassifiedId.get(i), e);
							}
						}
					}
					
					jsonBuilder.retriveMembersChange(changeDetail,approvers,assignee);
					logger.info("✅ mbomAndKitManagementOnRevision - KIT approvers = {} KIT reviewers = {}",approvers,assignee);
					
					JSONObject jsonPatchModifyChangeCustAttr = jsonBuilder.updateChangeActionJson(mfnCestamp, assignee,approvers,subjectId,changeMirrorBehavior, changeName, changeId, 
							Constant3DEXP.RANGEVALUE_DONOTHING,Constant3DEXP.VALUE_EMPTY, Constant3DEXP.TYPE_CREATEASSEMBLY,"CUSTOMATTR");
					JSONObject addCustomAttr = _3dsClient.patchChange(changeMfnId, jsonPatchModifyChangeCustAttr);
					logger.info("✅ mbomAndKitManagementOnRevision - patch change addCustomAttr ={}",addCustomAttr);
					
					mfnCestamp = addCustomAttr.getString(Constant3DEXP.CESTAMP);
					JSONObject jsonPatchModifyChangeMember = jsonBuilder.updateChangeActionJson(mfnCestamp, assignee,approvers,subjectId,changeMirrorBehavior, changeName, changeId, Constant3DEXP.RANGEVALUE_DONOTHING,Constant3DEXP.VALUE_EMPTY, Constant3DEXP.TYPE_CREATEASSEMBLY,"USER");
					JSONObject addMember = _3dsClient.patchChange(changeMfnId, jsonPatchModifyChangeMember);		
					logger.info("✅ mbomAndKitManagementOnRevision - patch change addMember {}",addMember.toString());
					
					mfnCestamp = addMember.getString(Constant3DEXP.CESTAMP);
					JSONObject jsonPatchModifyChangeProposed = jsonBuilder.updateChangeActionJson(mfnCestamp, assignee,approvers,subjectId,
															changeMirrorBehavior, changeName, changeId, Constant3DEXP.RANGEVALUE_DONOTHING,
															Constant3DEXP.VALUE_EMPTY, Constant3DEXP.TYPE_CREATEASSEMBLY,"PROPOSEDCHANGE");
					JSONObject addProposedChange = _3dsClient.patchChange(changeMfnId, jsonPatchModifyChangeProposed);
					//JSONObject addMembersAndItem = _3dsClient.patchChange(changeMfnId, jsonPatchModifyChange);	
					logger.info("✅ mbomAndKitManagementOnRevision - patch change addProposedChange {}",addProposedChange.toString());/*
					
					
					/*JSONObject jsonPatchModifyChange = jsonBuilder.updateChangeActionJson(mfnCestamp, assignee,approvers,subjectId,changeMirrorBehavior,
							changeName, changeId, Constant3DEXP.RANGEVALUE_DONOTHING, Constant3DEXP.VALUE_EMPTY, Constant3DEXP.TYPE_CREATEASSEMBLY);
					
					JSONObject addMembersAndItem = _3dsClient.patchChange(changeMfnId, jsonPatchModifyChange);	
					logger.info("✅ mbomAndKitManagementOnRevision - Output modify Change Action : {}",addMembersAndItem.toString());*/
					
					
					/*
					JSONObject jsonPatchModifyChange = jsonBuilder.updateChangeActionJson(mfnCestamp, assignee,approvers,
							createdKitId,Constant3DEXP.VALUE_EMPTY, changeName, changeId, changeKITBehavior,compareIds, Constant3DEXP.TYPE_CREATEKIT);
					
					JSONObject addMembersAndItem = _3dsClient.patchChange(changeKitId, jsonPatchModifyChange);	
					logger.info("✅ mbomAndKitManagementOnRevision - KIT Output modify Change Action : {}",addMembersAndItem.toString());*/
					
					/*JSONObject classifiedEngChange = _3dsClient.getWhereClassified(changeId);
					List<String> whereChangeClassifiedId = jsonBuilder.listclassId(classifiedEngChange);
					
					if(whereChangeClassifiedId.size()>0) {
						logger.info("✅ mbomAndKitManagementOnRevision - KIT Engineering Change Action classification Ids ={}",whereChangeClassifiedId.toString());
						for(int i = 0; i<whereChangeClassifiedId.size(); i++) {
							JSONObject classifiedChange = jsonBuilder.buildClassifyJson(whereChangeClassifiedId.get(i),Constant3DEXP.TYPE_CHANGEACTION,changeKitId,Constant3DEXP.RELATIVEPATH_CHANGE);
							_3dsClient.classifyObject(classifiedChange);
							logger.info("✅ mbomAndKitManagementOnRevision - KIT Manufacturing Change Action classified in: {}", whereChangeClassifiedId.get(i));
						}
					}
					
					JSONObject changeOwnerJson = jsonBuilder.buildJsonChangeOwner(company, collaborativeSpace, user,changeKitId);
					JSONObject newOwner = _3dsClient.changeOwner(changeOwnerJson);
					logger.info("✅ mbomAndKitManagementOnRevision - KIT new change owner ={}",newOwner.toString());
				}*/
				
				JSONObject createIssue = jsonBuilder.createIssueJson(engItemTitle,approvers,assignee);
				JSONObject issueJSON = _3dsClient.createIssue(createIssue);
				String issueId = issueJSON.getString(Constant3DEXP.ID);
				
				logger.info("✅ mbomAndKitManagementOnRevision - Issue ="+issueId);
				
				JSONObject changeIssueStatusJSON = jsonBuilder.generateStateChangeJson(issueId, "Assign");
				_3dsClient.promoteObject(changeIssueStatusJSON);
				JSONObject issueDetail = _3dsClient.getIssueById(issueId);
				 
				String cestamp = issueDetail.getString(Constant3DEXP.CESTAMP);
				
				List<String> listChangeId = new ArrayList<>();
				if(!changeId.isEmpty()) {
					listChangeId.add(changeId);
				}
				if(!changeMfnId.isEmpty()) {
					listChangeId.add(changeMfnId);
				}
				if(!changeKitId.isEmpty()) {
					listChangeId.add(changeKitId);
				}
				
				JSONObject addResolvedByItemJSON = jsonBuilder.composePatchJson(cestamp,listChangeId);
				JSONObject IssueUpdated = _3dsClient.patchIssue(issueId, addResolvedByItemJSON);
				logger.info("✅ mbomAndKitManagementOnRevision - Issue Updated ={}",IssueUpdated.toString());
			}
			
		}
	}

	private void mfnRootManagement(String subjectId, Client3DXImpl _3dsClient, String company, String collaborativeSpace, String user) throws Exception {
		JSONObject scopedEng = _3dsClient.getScopedEngItem(subjectId);
		String engId = jsonBuilder.retriveEngId(scopedEng);
		logger.info("✅ mfnRootManagement - Engineering Scoped Item ="+engId);
		if(engId!=null && !engId.equals("")) {
			JSONObject engItemDetail = _3dsClient.getEngineeringItemById(engId);
			String engItemTitle = "";
			engItemTitle = jsonBuilder.retriveEngItemInfo(engItemDetail,Constant3DEXP.TITLE,engItemTitle );
			try {
				logger.info("✅ mfnRootManagement - engItemTitle ="+engItemTitle);
				
				JSONObject classified = _3dsClient.getWhereClassified(engId);
				List<String> whereClassifiedId = jsonBuilder.listclassId(classified);
				
				JSONObject mfnItem = _3dsClient.getManufacturingItem(subjectId);
				String mfnType = jsonBuilder.retriveMfnType(mfnItem);
				
				if(whereClassifiedId.size()>0) {
					logger.info("✅ mfnRootManagement - Engineering Item classification Ids ="+whereClassifiedId.toString());
					for(int i = 0; i<whereClassifiedId.size(); i++) {
						try {
							
							JSONObject classifiedMfn = jsonBuilder.buildClassifyJson(whereClassifiedId.get(i),mfnType,subjectId,Constant3DEXP.RELATIVEPATH_MFGITEM);
							_3dsClient.classifyObject(classifiedMfn);
							logger.info("✅ mfnRootManagement - Manufacturing Item classifeid in : "+whereClassifiedId.get(i));
						
						}catch  (Exception e) {
							logger.error("❌ mfnRootManagement Errore durante la classificazione dell'Mfn = "+subjectId+" nella classe = "+whereClassifiedId.get(i), e);
						}
					}
				}
			}catch (Exception ex) {
	            logger.error("❌ mfnRootManagement Errore durante il recupero della classe:", ex);
	        }
			
			/*boolean isMBOMNeeded = jsonBuilder.isMBOMrequired(engItemDetail);
			boolean kitNeeded = jsonBuilder.isKITrequired(engItemDetail);
			logger.info("✅ mfnRootManagement - isMBOMNeeded ="+isMBOMNeeded);
			logger.info("✅ mfnRootManagement - kitNeeded ="+kitNeeded);
			String changeId = "";
			String changeMfnId = "";
			String changeKitId = "";
			List<String> approvers = new ArrayList<>();
			List<String> assignee = new ArrayList<>();
			if(isMBOMNeeded==true || kitNeeded== true) {
				String changeTitle ="";
				String changeName ="";
				String changeMirrorBehavior ="";
				
				JSONObject changeImpact = _3dsClient.getChangeImpact(engId);
				changeId = jsonBuilder.retriveChangeId(changeImpact);
				
				JSONObject changeDetail = _3dsClient.getChangeActionById(changeId);
				changeTitle = jsonBuilder.retriveChangeInfo(changeDetail,changeTitle,Constant3DEXP.TITLE);
				changeName = jsonBuilder.retriveChangeInfo(changeDetail,changeTitle,Constant3DEXP.NAME);
				changeMirrorBehavior = jsonBuilder.retriveChangeInfo(changeDetail,changeTitle,Constant3DEXP.ATTRIBUTE_COMAU_CA_MBOMMIRRORBEHAVIOR);
				logger.info("✅ mfnRootManagement - Engineering Change Action:id= {},name= {},title= {},mirrorBheavior= {}",changeId,changeName,changeTitle,changeMirrorBehavior);
				if(isMBOMNeeded == true) {
					
					JSONObject createChange = jsonBuilder.buildChangeBody(changeTitle, engItemTitle,"MBOM Mirroring");
					JSONObject changeMfn = _3dsClient.createChangeAction(createChange);
					
					changeMfnId = changeMfn.getString(Constant3DEXP.ID);
					String mfnCestamp = changeMfn.getString(Constant3DEXP.CESTAMP);
					logger.info("✅ mfnRootManagement - Manufacturing Change Action ="+changeMfnId);
					
					jsonBuilder.retriveMembersChange(changeDetail,approvers,assignee);
					logger.info("✅ mfnRootManagement - approvers ={},"+approvers.toString());
					logger.info("✅ mfnRootManagement - assignee ={},"+assignee.toString());
					JSONObject jsonPatchModifyChange = jsonBuilder.updateChangeActionJson(mfnCestamp, assignee,approvers,subjectId,changeMirrorBehavior, changeName, changeId, Constant3DEXP.RANGEVALUE_DONOTHING,Constant3DEXP.VALUE_EMPTY, Constant3DEXP.TYPE_CREATEASSEMBLY);
					JSONObject addMembersAndItem = _3dsClient.patchChange(changeMfnId, jsonPatchModifyChange);	
					logger.info("✅ mfnRootManagement - Output modify Change Action : "+addMembersAndItem.toString());
					
					JSONObject classifiedEngChange = _3dsClient.getWhereClassified(changeId);
					List<String> whereChangeClassifiedId = jsonBuilder.listclassId(classifiedEngChange);
					
					if(whereChangeClassifiedId.size()>0) {
						logger.info("✅ mfnRootManagement - Engineering Change Action classification Ids ="+whereChangeClassifiedId.toString());
						for(int i = 0; i<whereChangeClassifiedId.size(); i++) {
							JSONObject classifiedChange = jsonBuilder.buildClassifyJson(whereChangeClassifiedId.get(i),Constant3DEXP.TYPE_CHANGEACTION,changeMfnId,Constant3DEXP.RELATIVEPATH_CHANGE);
							_3dsClient.classifyObject(classifiedChange);
							logger.info("✅ mfnRootManagement - Manufacturing Change Action classified in "+ whereChangeClassifiedId.get(i));
						}
					}
					
					JSONObject changeOwnerJson = jsonBuilder.buildJsonChangeOwner(company, collaborativeSpace, user,changeMfnId);
					_3dsClient.changeOwner(changeOwnerJson);
				}
				
				JSONObject createIssue = jsonBuilder.createIssueJson(engItemTitle,approvers,assignee);
				JSONObject issueJSON = _3dsClient.createIssue(createIssue);
				String issueId = issueJSON.getString(Constant3DEXP.ID);
				
				logger.info("✅ mfnRootManagement - Issue ="+issueId);
				
				JSONObject changeIssueStatusJSON = jsonBuilder.generateStateChangeJson(issueId, "Assign");
				_3dsClient.promoteObject(changeIssueStatusJSON);
				JSONObject issueDetail = _3dsClient.getIssueById(issueId);
				 
				String cestamp = issueDetail.getString(Constant3DEXP.CESTAMP);
				
				List<String> listChangeId = new ArrayList<>();
				if(!changeId.isEmpty()) {
					listChangeId.add(changeId);
				}
				if(!changeMfnId.isEmpty()) {
					listChangeId.add(changeMfnId);
				}
				if(!changeKitId.isEmpty()) {
					listChangeId.add(changeKitId);
				}
				JSONObject addResolvedByItemJSON = jsonBuilder.composePatchJson(cestamp,listChangeId);
				JSONObject issueUpdated = _3dsClient.patchIssue(issueId, addResolvedByItemJSON);
				logger.info("✅ mfnRootManagement - Issue Updated ={}",issueUpdated);
				
			}*/
			
			
		}
		else {
			logger.info("❌ mfnRootManagement scoped Item non trovato per mfn id {}", subjectId);
		}
	}
	
private void mfnRootManagementConRetry(String subjectId, Client3DXImpl _3dsClient, String company,String collaborativeSpace,String user) {

    try {
        try {
        	
        	JSONObject scopedEng = _3dsClient.getScopedEngItem(subjectId);
            String engId = jsonBuilder.retriveEngId(scopedEng);

            logger.info("✅ mfnRootManagement - Engineering Scoped Item = {}", engId);

            // ------------------------------------------------------
            // Se non trovo scopedEng → avvio retry dopo 15 minuti
            // ------------------------------------------------------
            if (engId == null || engId.isBlank()) {
                logger.warn("⚠️ scopedEngItem non trovato per subjectId {} — retry tra 15 minuti", subjectId);
                //_3dsClient
                scheduleRetry(subjectId, company, collaborativeSpace, user);
                return;
            }

            // ------------------------------------------------------
            // Recupero info Engineering Item
            // ------------------------------------------------------
            JSONObject engItemDetail = _3dsClient.getEngineeringItemById(engId);
            String engItemTitle = jsonBuilder.retriveEngItemInfo(engItemDetail, Constant3DEXP.TITLE, "");

            logger.info("📄 mfnRootManagement - engItemTitle = {}", engItemTitle);

            // ------------------------------------------------------
            // Recupero classificazioni
            // ------------------------------------------------------
            JSONObject classified = _3dsClient.getWhereClassified(engId);
            List<String> whereClassifiedId = jsonBuilder.listclassId(classified);

            JSONObject mfnItem = _3dsClient.getManufacturingItem(subjectId);
            String mfnType = jsonBuilder.retriveMfnType(mfnItem);

            // ------------------------------------------------------
            // Classifico MFN nelle classi rilevate
            // ------------------------------------------------------
            if (!whereClassifiedId.isEmpty()) {
                logger.info("📌 Classificazioni trovate: {}", whereClassifiedId);

                for (String classId : whereClassifiedId) {
                    try {
                        JSONObject classifiedMfn = jsonBuilder.buildClassifyJson(
                                classId, mfnType, subjectId, Constant3DEXP.RELATIVEPATH_MFGITEM
                        );

                        _3dsClient.classifyObject(classifiedMfn);

                        logger.info("✅ MFN {} classificato nella classe {}", subjectId, classId);

                    } catch (Exception e) {
                        logger.error("❌ Errore classificando MFN {} nella classe {}", subjectId, classId, e);
                    }
                }
            }

            /*boolean isMBOMNeeded = jsonBuilder.isMBOMrequired(engItemDetail);
			boolean kitNeeded = jsonBuilder.isKITrequired(engItemDetail);
			logger.info("✅ mfnRootManagement - isMBOMNeeded ="+isMBOMNeeded);
			logger.info("✅ mfnRootManagement - kitNeeded ="+kitNeeded);
			String changeId = "";
			String changeMfnId = "";
			String changeKitId = "";
			List<String> approvers = new ArrayList<>();
			List<String> assignee = new ArrayList<>();
			if(isMBOMNeeded==true || kitNeeded== true) {
				String changeTitle ="";
				String changeName ="";
				String changeMirrorBehavior ="";
				
				JSONObject changeImpact = _3dsClient.getChangeImpact(engId);
				changeId = jsonBuilder.retriveChangeId(changeImpact);
				
				JSONObject changeDetail = _3dsClient.getChangeActionById(changeId);
				changeTitle = jsonBuilder.retriveChangeInfo(changeDetail,changeTitle,Constant3DEXP.TITLE);
				changeName = jsonBuilder.retriveChangeInfo(changeDetail,changeTitle,Constant3DEXP.NAME);
				changeMirrorBehavior = jsonBuilder.retriveChangeInfo(changeDetail,changeTitle,Constant3DEXP.ATTRIBUTE_COMAU_CA_MBOMMIRRORBEHAVIOR);
				logger.info("✅ mfnRootManagement - Engineering Change Action:id= {},name= {},title= {},mirrorBheavior= {}",changeId,changeName,changeTitle,changeMirrorBehavior);
				if(isMBOMNeeded == true) {
					
					JSONObject createChange = jsonBuilder.buildChangeBody(changeTitle, engItemTitle,"MBOM Mirroring");
					JSONObject changeMfn = _3dsClient.createChangeAction(createChange);
					
					changeMfnId = changeMfn.getString(Constant3DEXP.ID);
					String mfnCestamp = changeMfn.getString(Constant3DEXP.CESTAMP);
					logger.info("✅ mfnRootManagement - Manufacturing Change Action ="+changeMfnId);
					
					jsonBuilder.retriveMembersChange(changeDetail,approvers,assignee);
					logger.info("✅ mfnRootManagement - approvers ={}",approvers.toString());
					logger.info("✅ mfnRootManagement - assignee ={}",assignee.toString());
					//JSONObject jsonPatchModifyChange = jsonBuilder.updateChangeActionJson(mfnCestamp, assignee,approvers,subjectId,changeMirrorBehavior, changeName, changeId, Constant3DEXP.RANGEVALUE_DONOTHING,Constant3DEXP.VALUE_EMPTY, Constant3DEXP.TYPE_CREATEASSEMBLY);
					//CUSTOMATTR, USER, PROPOSEDCHANGE
					JSONObject jsonPatchModifyChangeCustAttr = jsonBuilder.updateChangeActionJson(mfnCestamp, assignee,approvers,subjectId,changeMirrorBehavior, changeName, changeId, Constant3DEXP.RANGEVALUE_DONOTHING,Constant3DEXP.VALUE_EMPTY, Constant3DEXP.TYPE_CREATEASSEMBLY,"CUSTOMATTR");
					JSONObject addCustomAttr = _3dsClient.patchChange(changeMfnId, jsonPatchModifyChangeCustAttr);
					logger.info("✅ mfnRootManagement - patch change addCustomAttr ={}",addCustomAttr);
					
					mfnCestamp = addCustomAttr.getString(Constant3DEXP.CESTAMP);
					JSONObject jsonPatchModifyChangeMember = jsonBuilder.updateChangeActionJson(mfnCestamp, assignee,approvers,subjectId,changeMirrorBehavior, changeName, changeId, Constant3DEXP.RANGEVALUE_DONOTHING,Constant3DEXP.VALUE_EMPTY, Constant3DEXP.TYPE_CREATEASSEMBLY,"USER");
					JSONObject addMember = _3dsClient.patchChange(changeMfnId, jsonPatchModifyChangeMember);		
					logger.info("✅ mfnRootManagement - patch change addMember {}",addMember.toString());
					
					mfnCestamp = addMember.getString(Constant3DEXP.CESTAMP);
					JSONObject jsonPatchModifyChangeProposed = jsonBuilder.updateChangeActionJson(mfnCestamp, assignee,approvers,subjectId,changeMirrorBehavior, changeName, changeId, Constant3DEXP.RANGEVALUE_DONOTHING,Constant3DEXP.VALUE_EMPTY, Constant3DEXP.TYPE_CREATEASSEMBLY,"PROPOSEDCHANGE");
					JSONObject addProposedChange = _3dsClient.patchChange(changeMfnId, jsonPatchModifyChangeProposed);
					//JSONObject addMembersAndItem = _3dsClient.patchChange(changeMfnId, jsonPatchModifyChange);	
					logger.info("✅ mfnRootManagement - patch change addProposedChange {}",addProposedChange.toString());
					
					JSONObject classifiedEngChange = _3dsClient.getWhereClassified(changeId);
					List<String> whereChangeClassifiedId = jsonBuilder.listclassId(classifiedEngChange);
					
					if(whereChangeClassifiedId.size()>0) {
						logger.info("✅ mfnRootManagement - Engineering Change Action classification Ids ="+whereChangeClassifiedId.toString());
						for(int i = 0; i<whereChangeClassifiedId.size(); i++) {
							JSONObject classifiedChange = jsonBuilder.buildClassifyJson(whereChangeClassifiedId.get(i),Constant3DEXP.TYPE_CHANGEACTION,changeMfnId,Constant3DEXP.RELATIVEPATH_CHANGE);
							_3dsClient.classifyObject(classifiedChange);
							logger.info("✅ mfnRootManagement - Manufacturing Change Action classified in "+ whereChangeClassifiedId.get(i));
						}
					}
					
					JSONObject changeOwnerJson = jsonBuilder.buildJsonChangeOwner(company, collaborativeSpace, user,changeMfnId);
					_3dsClient.changeOwner(changeOwnerJson);
				}
				
				JSONObject createIssue = jsonBuilder.createIssueJson(engItemTitle,approvers,assignee);
				JSONObject issueJSON = _3dsClient.createIssue(createIssue);
				String issueId = issueJSON.getString(Constant3DEXP.ID);
				
				logger.info("✅ mfnRootManagement - Issue ="+issueId);
				
				JSONObject changeIssueStatusJSON = jsonBuilder.generateStateChangeJson(issueId, "Assign");
				_3dsClient.promoteObject(changeIssueStatusJSON);
				JSONObject issueDetail = _3dsClient.getIssueById(issueId);
				 
				String cestamp = issueDetail.getString(Constant3DEXP.CESTAMP);
				
				List<String> listChangeId = new ArrayList<>();
				if(!changeId.isEmpty()) {
					listChangeId.add(changeId);
				}
				if(!changeMfnId.isEmpty()) {
					listChangeId.add(changeMfnId);
				}
				if(!changeKitId.isEmpty()) {
					listChangeId.add(changeKitId);
				}
				JSONObject addResolvedByItemJSON = jsonBuilder.composePatchJson(cestamp,listChangeId);
				JSONObject issueUpdated = _3dsClient.patchIssue(issueId, addResolvedByItemJSON);
				logger.info("✅ mfnRootManagement - Issue Updated ={}",issueUpdated);
				
			}
			*/
        } catch (Exception ex) {
            logger.error("❌ Errore generale in mfnRootManagement per {}", subjectId, ex);
        }
    } catch (Exception e) {
        logger.error("❌ Errore durante il log in:", e);
    }
        
}


// ======================================================
// Retry asincrono: ritenta dopo 15 minuti su thread separato
// ======================================================
	private void scheduleRetry(String subjectId,
                           //Client3DXImpl _3dsClient,
                           String company,
                           String collaborativeSpace,
                           String user) throws Exception {

		
        
		scheduler.schedule(() -> {
			HashMap<String, String> loginMap = new HashMap<>();
	    	String securityContext = role + "." + company + "." + collaborativeSpace;
	    	loginMap.put("securityContext", securityContext);	    	
	        logger.info("✅ - Successful log in");
			logger.info("🔁 Retry mfnRootManagement per subjectId {} dopo 15 minuti", subjectId);
			try {
				Client3DXImpl _3dsClient = SessionManager.getClient(user, loginMap);
				_3dsClient.setSecurityContext(securityContext);
				/*Client3DXImpl _3dsClient = Client3dExperienceFactory.newInstance();
		        _3dsClient.init(loginMap);*/
				mfnRootManagement(subjectId, _3dsClient, company, collaborativeSpace, user);
			} catch (Exception e) {
				logger.error("ERROR ON CLASSIFICATION RETRY");
			}
		}, 15, TimeUnit.MINUTES);
	}



// ======================================================
// Metodo da chiamare allo shutdown dell'app (es. @PreDestroy)
// ======================================================
	public void shutdownRetryScheduler() {
		logger.info("🛑 Shutdown scheduler dei retry...");
		scheduler.shutdown();

		try {
			if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
				logger.warn("⚠️ Forzo shutdown immediato dello scheduler.");
				scheduler.shutdownNow();
			}
		} catch (InterruptedException e) {
			scheduler.shutdownNow();
		}

		logger.info("✅ Scheduler arrestato correttamente.");
	}
	
	private int fillProposedChange(Client3DXImpl _3dsClient, JSONObject changePromoted, String subjectId) throws Exception {
		int res = 0;
		String cestampChange = changePromoted.getString(Constant3DEXP.CESTAMP);
		
		String mfnRootId = jsonBuilder.proposedManufacturingItemId(changePromoted, Constant3DEXP.TYPE_CREATEASSEMBLY);
		logger.info("✅ fillProposedChange - mfnRootId ={} inside change action ={}",mfnRootId,changePromoted);
		if(!mfnRootId.equals("") && mfnRootId != null)
		{
			JSONObject jsonBodyExpand = jsonBuilder.buildJsonBodyExpand();
			
			JSONObject expandedObject = _3dsClient.expandManufacturingItem(mfnRootId, jsonBodyExpand);
			List<String> listMBOMid = jsonBuilder.extractMBOMIds(expandedObject,mfnRootId);
			if(listMBOMid.size()>0) {
				
				logger.info("✅ fillProposedChange - Manufacturing Item Childs {}",listMBOMid.toString());
				JSONArray proposedChangeArray = new JSONArray();
				
				for(int i = 0; i<listMBOMid.size();i++) {
					
					String childid = listMBOMid.get(i);
					JSONObject mfnChild = _3dsClient.getManufacturingItem(childid);
					String current = jsonBuilder.retriveManufacturingItemInfo(mfnChild,Constant3DEXP.STATE);
					String sType = jsonBuilder.retriveManufacturingItemInfo(mfnChild,Constant3DEXP.TYPE);
					if(!current.equals(Constant3DEXP.STATE_RELEASED)) {
						JSONObject proposedChangeJson = jsonBuilder.generateProposedChangeArray(childid, sType);
						proposedChangeArray.put(proposedChangeJson);
					}
					
					
				}
				JSONObject bodyRequestProposedChange = jsonBuilder.addProposedChangeBody(cestampChange, proposedChangeArray);
				logger.info("✅ fillProposedChange - Adding proposed change bodyRequestProposedChange {}",proposedChangeArray.toString());
				JSONObject responsePatchChange = _3dsClient.patchChange(subjectId, bodyRequestProposedChange);
				
				logger.info("✅ fillProposedChange - Change Action {} modified result:{} ",subjectId,responsePatchChange.toString());
				
			}
			else {
				res =1;
			}
		}
		else {
			res =1;
		}
		return res;
	}
	
	private void promoteChange(String subjectId, Client3DXImpl _3dsClient) throws Exception {
		
		logger.info("✅ promoteChange - subjectId ={}",subjectId );
		JSONObject changePromoteJson = jsonBuilder.generateStateChangeJson(subjectId, "In Approval");
		JSONObject resultPromote = _3dsClient.promoteObject(changePromoteJson);
		logger.info("✅ promoteChange - resultPromote subjectId ={}",resultPromote );
	}
	
	private void approveChangeAction(Client3DXImpl _3dsClient, String subjectId ) throws Exception {
		
		JSONObject changeDetail = _3dsClient.getChangeActionById(subjectId);
		
		JSONObject approveChangeJson = jsonBuilder.approveChangeBody(changeDetail);
		logger.info("✅ approveChangeAction - approveChangeJson ={}",approveChangeJson.toString());
		
		JSONObject resultApprovedChange = _3dsClient.approveChangeActionById(subjectId, approveChangeJson);
		
		logger.info("✅ approveChangeAction - resultApprovedChange {} ",resultApprovedChange.toString());
		
	}
	
	public static boolean isStale(long maxSilenceMs) { 
		return System.currentTimeMillis() - lastMessageTime > maxSilenceMs; 
		}
}


