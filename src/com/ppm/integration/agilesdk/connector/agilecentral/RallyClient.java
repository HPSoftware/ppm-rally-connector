package com.ppm.integration.agilesdk.connector.agilecentral;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.hp.ppm.integration.provider.Providers;
import com.hp.ppm.integration.provider.UserProvider;
import com.ppm.integration.agilesdk.connector.agilecentral.Config;
import com.ppm.integration.agilesdk.connector.agilecentral.RallyIntegrationConnector;
import com.ppm.integration.agilesdk.connector.agilecentral.RestHelper;
import com.ppm.integration.agilesdk.connector.agilecentral.model.HierarchicalRequirement;
import com.ppm.integration.agilesdk.connector.agilecentral.model.Iteration;
import com.ppm.integration.agilesdk.connector.agilecentral.model.Project;
import com.ppm.integration.agilesdk.connector.agilecentral.model.Release;
import com.ppm.integration.agilesdk.connector.agilecentral.model.Revision;
import com.ppm.integration.agilesdk.connector.agilecentral.model.RevisionHistory;
import com.ppm.integration.agilesdk.connector.agilecentral.model.Subscription;
import com.ppm.integration.agilesdk.connector.agilecentral.model.User;
import com.ppm.integration.agilesdk.connector.agilecentral.model.Workspace;

public class RallyClient {

	private final RestHelper helper;

	public RallyClient(String endpoint, Config config) {
		this.helper = new RestHelper(endpoint, config);
	}

	public Subscription getSubscription() {
		String subscriptionURI = "/slm/webservice/v2.0/subscription";
		return new Subscription(helper.get(subscriptionURI).getJSONObject("Subscription"));
	}

	public List<Workspace> getWorkspaces(Subscription subscription){
		return getWorkspaces(subscription.getId());
	}

	public List<Workspace> getWorkspaces(String subscriptionId){
		JSONArray jsonArray = helper.getAll("/slm/webservice/v2.0/Subscription/"+subscriptionId+"/Workspaces");
		List<Workspace> workspaces = new ArrayList<Workspace>(jsonArray.size());
		for (int i = 0; i < jsonArray.size(); i++) {
			workspaces.add(new Workspace(jsonArray.getJSONObject(i)));
		}
		return workspaces;
	}

	public List<Project> getProjects(Workspace workspace) {
		return getProjects(workspace.getId());
	}

	public List<Project> getProjects(String workspaceId) {
		JSONArray jsonArray = helper.getAll("/slm/webservice/v2.0/Workspace/"+workspaceId+"/Projects");
		List<Project> projects = new ArrayList<Project>(jsonArray.size());
		for (int i = 0; i < jsonArray.size(); i++) {
			projects.add(new Project(jsonArray.getJSONObject(i)));
		}
		return projects;
	}

	public List<Iteration> getIterations(String projectId) {
		String iterationsURI = "/slm/webservice/v2.0/project/?/Iterations";
		iterationsURI = iterationsURI.replace("?", projectId);
		JSONArray jsonArray = helper.getAll(iterationsURI);
		List<Iteration> iterations = new ArrayList<Iteration>(jsonArray.size());
		for (int i = 0; i < jsonArray.size(); i++) {
			iterations.add(new Iteration(jsonArray.getJSONObject(i)));
		}
		List<HierarchicalRequirement> hierarchicalRequirements = getHierarchicalRequirements();
		fillHierarchicalRequirement(iterations, hierarchicalRequirements);
		List<User> users = getUsers();
		fillUser(hierarchicalRequirements, users);
		return iterations;
	}
	

	public List<HierarchicalRequirement> getHierarchicalRequirements() {
		String hierarchicalrequirementURI = "/slm/webservice/v2.0/hierarchicalrequirement";
		JSONArray jsonArray = helper.query(hierarchicalrequirementURI, "", true, "", 1, 20);
		List<HierarchicalRequirement> hierarchicalRequirements = new ArrayList<HierarchicalRequirement>();

		UserProvider userProvider = Providers.getUserProvider(RallyIntegrationConnector.class);
		for (int i = 0; i < jsonArray.size(); i++) {
		
			hierarchicalRequirements.add(new HierarchicalRequirement(jsonArray.getJSONObject(i),userProvider));
		}
		return hierarchicalRequirements;
	}

	private List<User> getUsers() {
		String userURI = "/slm/webservice/v2.0/user";
		JSONArray jsonArray = helper.query(userURI, "", true, "", 1, 20);
		List<User> users = new ArrayList<User>();
		for (int i = 0; i < jsonArray.size(); i++) {
			users.add(new User(jsonArray.getJSONObject(i)));
		}
		return users;
	}

	private void fillHierarchicalRequirement(List<Iteration> iterations, List<HierarchicalRequirement> hierarchicalRequirements) {
		for (HierarchicalRequirement hierarchicalRequirement : hierarchicalRequirements) {
			for (Iteration iteration : iterations) {
				iteration.addHierarchicalRequirement(hierarchicalRequirement);
			}
		}
	}

	private void fillUser(List<HierarchicalRequirement> hierarchicalRequirements, List<User> users) {
		for (User user : users) {
			for (HierarchicalRequirement hierarchicalRequirement : hierarchicalRequirements) {
				user.addHierarchicalRequirement(hierarchicalRequirement);
			}
		}
	}
	public List<Release> getReleases(String projectId) {
		String releasesURI = "/slm/webservice/v2.0/project/?/Releases";
		releasesURI = releasesURI.replace("?", projectId);
		JSONArray jsonArray = helper.getAll(releasesURI);
		List<Release> releases = new ArrayList<Release>(jsonArray.size());
		for (int i = 0; i < jsonArray.size(); i++) {
			releases.add(new Release(jsonArray.getJSONObject(i)));
		}
		return releases;
	}
	public List<Iteration> getIterationsByRelease(String projectId, String releaseId) {
		String iterationsURI = "/slm/webservice/v2.0/project/?/Iterations";
		iterationsURI = iterationsURI.replace("?", projectId);
		JSONArray jsonArray = helper.getAll(iterationsURI);		
		List<Iteration> iterations = new ArrayList<Iteration>();
		List<HierarchicalRequirement> hierarchicalRequirements = getHierarchicalRequirements();
		List<User> users = getUsers();
		
		String releaseURI = "/slm/webservice/v2.0/release/" + releaseId;
		JSONObject releaseObject = helper.get(releaseURI).getJSONObject("Release");
		if(releaseObject.isNullObject()){
			for (int i = 0; i < jsonArray.size(); i++) {
				iterations.add(new Iteration(jsonArray.getJSONObject(i)));
			}
		}else{			
			Release release = new Release(releaseObject);
			Date releaseStart = release.getScheduleStart();
			Date releaseEnd = release.getScheduleFinish();		
			
			for (int i = 0; i < jsonArray.size(); i++) {
				Iteration iteration = new Iteration(jsonArray.getJSONObject(i));
				Date iterationStart = iteration.getScheduleStart();							
				if(iterationStart.getTime() > releaseStart.getTime() && iterationStart.getTime() < releaseEnd.getTime()){				
					iterations.add(iteration);
				}
			}
		}
				
		fillHierarchicalRequirement(iterations, hierarchicalRequirements);
		fillUser(hierarchicalRequirements, users);
		return iterations;
	}

	public RevisionHistory getRevisionHistory(String getRevisionHistoryRef) {
		int len = getRevisionHistoryRef.split("/").length;
		String revisionHistoryURI = "/slm/webservice/v2.0/revisionhistory/?";
		revisionHistoryURI = revisionHistoryURI.replace("?", getRevisionHistoryRef.split("/")[len - 1]);
		JSONObject historyObject = helper.get(revisionHistoryURI).getJSONObject("RevisionHistory");
		RevisionHistory revisionHistory = new RevisionHistory(historyObject);
		return revisionHistory;
	}
	
	public List<Revision> getRevisions(String getRevisionHistoryId) {
		String revisionhistoryURI = "/slm/webservice/v2.0/revisionhistory/?/revisions";
		revisionhistoryURI = revisionhistoryURI.replace("?", getRevisionHistoryId);
		JSONArray jsonArray = helper.getAll(revisionhistoryURI);
		List<Revision> revisions = new ArrayList<Revision>(jsonArray.size());
		for (int i = 0; i < jsonArray.size(); i++) {
			revisions.add(new Revision(jsonArray.getJSONObject(i)));
		}
		return revisions;
	}

}