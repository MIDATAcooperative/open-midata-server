<template>
	<div>
    <panel :title="$t('newconsent.title')" >
    
        <div class="alert alert-info" v-if="pleaseReview">
            <h4 class="alert-heading" v-t="'newconsent.please_review1'"></h4>
            <p v-t="'newconsent.please_review2'"></p>
        </div>
        <form name="myform" ref="myform" class="css-form form-horizontal" @submit.prevent="create()" novalidate role="form">
            <error-box :error="error"></error-box>
    
            <div class="row" v-if="!consent.type">
                <div class="col-sm-3">
                    <div class="card button" @click="consent.type='CIRCLE';">
                        <img :src="getIconRole('member')" class="card-img-top">
                        <div class="card-body">                
                            <span class="card-text"  v-t="'newconsent.share_with_members'"></span>
                            <a href="javascript:" class="card-link" v-t="'newconsent.select'"></a>      
                        </div>
                    </div>
                </div>
        
                <div class="col-sm-3">
                    <div class="card"  @click="consent.type='HEALTHCARE';consent.writesBool=true;">
                        <img :src="getIconRole('provider')" class="card-img-top">
                        <div class="card-body">                
                            <span class="card-text" v-t="'newconsent.share_with_provider'"></span>  
                            <a href="javascript:" class="card-link" v-t="'newconsent.select'"></a>     
                        </div>
                    </div>
                </div>
            </div>
    
            <div class="row" v-else >
                <div class="col-sm-5"> 
                    <p><b v-t="'newconsent.who_is_owner'"></b></p>
          
                    <div class="" v-if="owner">
                        <div class="card-body">
                            <img :src="getIconRole(owner)" class="float-left consenticon">
                            <div class="iconspace">
                                <div>{{ $t('enum.userrole.'+owner.role)}}</div>		  	
		                        <address>
			                        <strong>{{ owner.firstname }} {{ owner.lastname }}</strong>
			                        <div v-if="owner.email">{{ owner.email }}<br></div>
			                        <span v-if="owner.address1 || owner.city || owner.country"><br>{{ owner.address1 }}<br>
			                            {{ owner.address2 }}<br>
			                            {{ owner.country }} {{ owner.zip }} {{ owner.city }}			
			                        </span>			
		                        </address>
		                    </div>
		                </div>		 		   				  
		            </div>
		            <div class="" v-if="consent.externalOwner">
		                <div class="card-body">
		                <img src="/images/question.jpeg" class="float-left consenticon">
                        <div class="iconspace">
		                    <div v-t="editconsent.external"></div>
		                    <div><strong>{{ consent.externalOwner }}</strong></div>
		                </div>
		            </div>
		        </div>
		        <div class="margin-top">
		            <button v-if="!(owner || consent.externalOwner)" type="button" class="btn btn-default" @click="setOwner();" v-t="'newconsent.set_owner_btn'"></button>
		        </div>
                        
            </div>
            <div class="col-sm-1">
                <div style="margin-top:60px" class="text-center">
                    <span style="font-size:40px" class="fas fa-arrow-right"></span>
                </div>
            </div>
            <div class="col-sm-6">
                <p><b v-t="'editconsent.people'"></b></p>
                <div  v-if="consent.type=='EXTERNALSERVICE'">
                    <div class="card-body">
                        <img :src="getIconRole('app')" class="consenticon float-left">
                        <div class="iconspace">
                            <strong v-t="'editconsent2.external'"></strong>
                        </div>
                    </div>
		        </div>
		        <div v-if="consent.type=='API'">
                    <div class="card-body">
                        <img :src="getIconRole('app')" class="consenticon float-left">
                        <div class="iconspace">
                            <strong v-t="'editconsent2.external'"></strong>
                        </div>
                    </div>
                </div>
                <div v-if="consent.type=='STUDYRELATED'">
                    <div class="card-body">
                        <img :src="getIconRole('community')" class="consenticon float-left">
                        <div class="iconspace">
                            <strong v-t="'editconsent2.community'"></strong>
                        </div>
                    </div>
                </div>
                <div v-for="person in authpersons" :key="person._id">
                    <div class="card-body">
		                <button type="button" @click="removePerson(person)" class="close" aria-label="Delete" v-if="mayChangeUsers()"><span aria-hidden="true">&times;</span></button>
		                <img :src="getIconRole(person)" class="float-left consenticon">
		                <div class="iconspace">
		                    <div>{{$t('enum.userrole.'+person.role)}}</div>		
		                    <address>
			                    <strong>{{ person.firstname }} {{ person.lastname }}</strong>
			                    <div v-if="person.email">{{ person.email }}<br></div>
			                    <span v-if="person.address1 || person.city || person.country"><br>
			                        {{ person.address1 }}<br>
			                        {{ person.address2 }}<br>
			                        {{ person.country }} {{ person.zip }} {{ person.city }}	
			                    </span>			
		                    </address>
		                </div>
		            </div>
		        </div>
		        <div v-for="usergroup in authteams" :key="usergroup._id">
		            <div class="card-body">
		                <button type="button" v-if="mayChangeUsers()" @click="removePerson(usergroup)" class="close" aria-label="Delete"><span aria-hidden="true">&times;</span></button>
		                <img :src="getIconRole('team')" class="float-left consenticon">
		                <div class="iconspace">
		                    <div v-t="'editconsent2.team'"></div>	
		                    <strong>{{ usergroup.name }}</strong>
		                </div>
		            </div>
		        </div>
		        <div v-for="person in consent.externalAuthorized" :key="person">
		            <div class="card-body">
		                <img :src="getIconRole('external')" class="float-left consenticon">
		                <div class="iconspace">
		                    <div v-t="'editconsent.external'"></div>
		                    <strong>{{ person }}</strong>
		                </div>
		            </div>
		        </div>
		        <div v-if="consent.reshare">
		            <div class="card-body">
		                <img :src="getIconRole('reshare')" class="float-left consenticon">
		                <div class="iconspace">
		                    <span v-t="'newconsent.reshare'"></span>
		                </div>
		            </div>
		        </div>
		
		        <div class="margin-top">
		            <button type="button" class="btn btn-default" :class="{ 'btn-sm' : consent.authorized.length }" v-show="consent.owner != userId && consent.authorized.indexOf(userId)<0" @click="addYourself();" v-t="'newconsent.add_yourself_btn'"></button>
		            <button type="button" class="btn btn-default" :class="{ 'btn-sm' : consent.authorized.length }" v-show="consent.entityType!='USERGROUP'" @click="addPeople();" v-t="'newconsent.add_person_btn'"></button>
		            <button type="button" class="btn btn-default" :class="{ 'btn-sm' : consent.authorized.length }" v-show="consent.entityType!='USER' && consent.type!='CIRCLE'" @click="addUserGroup();" v-t="'newconsent.add_usergroup_btn'"></button>
	            </div>
	            <div class="extraspace"></div>
		
            </div>
        </div>
    
        
	
        <div v-if="sharing.records || sharing.query" class="margin-top">
        
            <p><b v-t="'editconsent.what_is_shared'"></b></p>
        <!-- <div v-if="groupLabels.length && groupLabels.length < 5">{{ groupLabels.join(", ") }}</div>  -->
                <ul v-if="groupLabels.length">
                    <li v-for="label in groupLabels" :key="label">{{ label }}</li>
                </ul>
                <div v-if="groupExcludeLabels.length">
                    <span v-t="'editconsent2.exclude'"></span>: {{ groupExcludeLabels.join(", ") }}
                </div>
        
                <p v-if="sharing.records.length == 0 && !sharing.query.group.length" v-t="'editconsent.consent_empty'"></p>
                <p v-if="sharing.records.length">{{ $t('editconsent.shares_records', { count : sharing.records.length }) }}</p>
                <div class="extraspace"></div>
                <p><b v-t="'editconsent.restrictions'"></b></p>
                <p>{{ $t('enum.writepermissiontype.'+(consent.writes || 'NONE')) }}</p>
                <p v-if="consent.createdBefore"><span v-t="'editconsent.created_before'"></span>:{{ $filters.date(consent.createdBefore) }} 
            </p>
        
        </div>
            
        <div v-if="options.advanced" class="margin-top">
            <div class="extraspace"></div>
            <form-group name="writes" label="newconsent.writes">
                <check-box v-model="consent.writesBool" name="writes">
                    <span v-t="'newconsent.writes2'"></span>
                </check-box>	    
            </form-group>
        
            <form-group name="reshare" label="newconsent.reshare">
                <check-box v-model="consent.reshare" name="reshare">
                    <span v-t="'newconsent.reshare2'"></span>
                </check-box>
            </form-group>
      
        
            <form-group name="passcode" label="newconsent.use_passcode" v-if="consent.type == 'HEALTHCARE' && consent.owner == userId">
                <check-box v-model="consent.usepasscode" name="usepasscode">
                    <span v-t="'newconsent.use_passcode2'"></span>
                </check-box>    
            </form-group>

            <form-group name="passcode2" label="newconsent.choose_passcode" ng-if="consent.usepasscode">	      
                <input  id="passcode2" name="passcode2" type="text" class="form-control" v-validate v-model="consent.passcode" required>     
            </form-group>    
            
            <form-group name="validUntil" label="newconsent.expiration_date">        
                <input id="validUntil" type="date" class="form-control" v-validate v-model="consent.validUntil" >              
            </form-group>

            <form-group name="createdBefore" label="newconsent.created_before">	  
                <input id="createdBefore" type="date" class="form-control" v-validate v-model="consent.createdBefore"  />              
            </form-group>

            <form-group name="name" label="newconsent.name">
                <input id="name" name="name" type="text" class="form-control" v-validate v-model="consent.name">
            </form-group>
        </div>
            
        <div v-if="consent.type" class="margin-top">
            <button type="button" @click="skip();" v-if="maySkip()" class="btn btn-default" v-t="'common.skip_btn'"></button>
            <span v-if="!consent.query">
                <span v-if="!pleaseReview">
                    <button v-if="consent.authorized.length || consent.usepasscode || consent.externalAuthorized" type="submit" v-submit class="btn btn-primary" v-t="'newconsent.create_btn'"></button>
                </span>
                <span v-if="pleaseReview">
                    <button v-if="consent.authorized.length || consent.usepasscode || consent.externalAuthorized" type="submit" v-submit class="btn btn-primary" v-t="'newconsent.create2_btn'"></button>
                </span>
            </span>
            <span v-if="consent.query">
                <button v-if="consent.authorized.length || consent.usepasscode" type="submit" v-submit class="btn btn-primary" v-t="'newconsent.create2_btn'"></button>
            </span>     
            <button type="button" class="btn btn-link" v-if="!options.advanced" @click="options.advanced=true;" v-t="'newconsent.extended_btn'"></button>
        </div>
          
        </form>
    </panel>
	
	<modal @close="setupProvidersearch=null" v-if="setupProvidersearch" :title="$t('providersearch.title')">
	   <provider-search :setup="setupProvidersearch" @add="addPerson"></provider-search>
	</modal>
	
	<modal @close="setupAdduser=null" v-if="setupAdduser" :title="$t('addusers.title')">
	  <add-users :setup="setupAdduser" @close="setupAdduser=null" @add="addPerson"></add-users>
	</modal>

	<modal @close="setupAddowner=null" v-if="setupAddowner" :title="$t('addusers.title')">
	  <add-users :setup="setupAddowner" @close="setupAddowner=null" @add="setOwnerPerson"></add-users>
	</modal>

	<modal @close="setupSearchGroup=null" v-if="setupSearchGroup" :title="$t('usergroupsearch.title')">
	  <user-group-search :setup="setupSearchGroup" @close="setupSearchGroup=null" @add="addPerson"></user-group-search>
	</modal>

	</div>
</template>
<script>
import CheckBox from 'components/CheckBox.vue'
import Panel from 'components/Panel.vue'
import ErrorBox from 'components/ErrorBox.vue'
import FormGroup from 'components/FormGroup.vue'
import Modal from 'components/Modal.vue'
import status from 'mixins/status'
import server from 'services/server';
import circles from 'services/circles';
import session from 'services/session';
import actions from 'services/actions';
import users from 'services/users';
import hc from 'services/hc';
import { getLocale } from 'services/lang';
import ProviderSearch from "components/tiles/ProviderSearch.vue"
import AddUsers from "components/tiles/AddUsers.vue"
import UserGroupSearch from "components/tiles/UserGroupSearch.vue"


export default {
	data: () => ({
        types : [
	        { value : "CIRCLE", label : "enum.consenttype.CIRCLE"},
	        { value : "HEALTHCARE", label : "enum.consenttype.HEALTHCARE" },
	        { value : "STUDYPARTICIPATION", label : "enum.consenttype.STUDYPARTICIPATION" },
			{ value : "EXTERNALSERVICE", label : "enum.consenttype.EXTERNALSERVICE" },
			{ value : "API", label : "enum.consenttype.API" },
	    ],
		stati : [
	        { value : "ACTIVE", label : "enum.consent.ACTIVE" },
	        { value : "UNCONFIRMED", label : "enum.consent.UNCONFIRMED" },
	        { value : "EXPIRED", label : "enum.consent.EXPIRED" }
	    ],
        lang : getLocale(),
	    authpersons : [],
	    authteams : [],
        options : {},
	    writeProtect : true,
	    isSimple : true,
        pleaseReview : false,
        userId : null,
        consent : { name : null },
        consentId : null,
        sharing : {},
		owner : null,
		setupProvidersearch : null,
		setupAdduser : null,
		setupAddowner : null,
		setupSearchGroup : null
	}),		
    
    components: { ErrorBox, CheckBox, Panel, FormGroup, ProviderSearch, AddUsers, Modal, UserGroupSearch },

    mixins : [ status ],

    methods : {

        getName(obj) {		
		    var dt = this.$filters.date(new Date());
		    if (obj.name) return obj.name+" "+dt;
		    if (obj.lastname) return obj.firstname+" "+obj.lastname+" "+dt;
		    return dt;		
        },
        
	    init(userId) {
            const { $data, $route, $router } = this, me = this;
		    $data.userId = userId;
		    $data.authpersons = [];
		    $data.authteams = [];
		
		if ($route.query.consentId) {
			$data.isSimple = true;
			$data.consentId = $route.query.consentId;
			
			me.doBusy(circles.listConsents({ "_id" : $route.query.consentId }, ["name", "type", "status", "owner", "ownerName", "authorized", "entityType", "createdBefore", "validUntil", "externalOwner", "externalAuthorized", "sharingQuery", "dateOfCreation", "writes" ]))
			.then(function(data) {
				if (!data.data || !data.data.length) {
					$data.consent = null;
					return;
				}								
				
				$data.consent = data.data[0];
				
				if ($data.consent.type === "CIRCLE") $data.isSimple = false;
				
				if ($data.consent.status === "ACTIVE" || $data.consent.owner === $data.userId) {
				  //views.setView("records_shared", { aps : $route.query.consentId, properties : { } , fields : [ "ownerName", "created", "id", "name" ], allowRemove : false, allowAdd : false, type : "circles" });
				} else {
				  //views.disableView("records_shared");
				}

				if ($data.consent.entityType == "USERGROUP") {
					me.doBusy(usergroups.search({ "_id" : $data.consent.authorized }, ["name"]))
					.then(function(data2) {
						for (let userGroup of data2.data) {
							$data.authteams.push(userGroup);
						}
					});
				} else {
	                var role = ($data.consent.type === "HEALTHCARE") ? "PROVIDER" : null;				
					for (let p of $data.consent.authorized) {					
						$data.authpersons.push(session.resolve(p, function() {
							var res = { "_id" : p };
							if (role) res.role = role;
							return users.getMembers(res, (role == "PROVIDER" ? users.ALLPUBLIC : users.MINIMAL )); 
						}));
					}
				}
								
				
				if ($data.consent.owner && $data.content.type!="STUDYRELATED") {
					users.getMembers({ "_id" : $data.consent.owner }, [ "firstname", "lastname", "email", "role"])
					.then(function(result) { console.log(result);$data.owner = result.data[0]; });
				}
				
				$data.writeProtect = ($data.consent.owner !== userId && $data.consent.status !== "UNCONFIRMED") || $data.consent.type === "EXTERNALSERVICE" || $data.consent.type === "API" || $data.consent.type === "STUDYPARTICIPATION" || $data.consent.status === "EXPIRED" || $data.consent.status === "REJECTED";
			
				me.doBusy(server.get(jsRoutes.controllers.Records.getSharingDetails($route.query.consentId).url)).
				then(function(results) {				
				    $data.sharing = results.data;
				    
				    if ($data.sharing.query) {
				    	$data.sharing.query = labels.simplifyQuery($data.sharing.query);				    	
				    	if ($data.sharing.query["group-exclude"] && !Array.isArray($data.sharing.query["group-exclude"])) { $data.sharing.query["group-exclude"] = [ $data.sharing.query["group-exclude"] ]; }
				    	if ($data.sharing.query.group && !Array.isArray($data.sharing.query.group)) { $data.sharing.query.group = [ $data.sharing.query.group ]; }
				    	$data.updateSharingLabels();
				    }
				});
			});
			
		} else {
			$data.isSimple = false;
			$data.consent = { type : ($route.meta.role == "provider" ? "HEALTHCARE" : null), status : "ACTIVE", authorized : [], writes : "NONE" };
			if ($route.meta.role == "provider") $data.consent.writesBool = true;
			//views.disableView("records_shared");
			
			if ($route.query.owner != null) {
				$data.consent.owner = $route.query.owner;
			} else if ($route.query.extowner) {
				$data.consent.externalOwner = $route.query.extowner;
				$data.owner = null;
			} else if ($route.query.request) {
				me.addYourself();
				$data.owner = null;
			} else { $data.consent.owner = userId; }
			
			if ($route.query.share != null) {
				$data.sharing = { query : JSON.parse($route.query.share) };
				if ($data.sharing.query["group-exclude"] && !Array.isArray($data.sharing.query["group-exclude"])) { $data.sharing.query["group-exclude"] = [ $data.sharing.query["group-exclude"] ]; }
				if ($data.sharing.query.content) {
					if (!Array.isArray($data.sharing.query.content)) $data.sharing.query.content = [ $data.sharing.query.content ];
                    $data.sharing.query.group = [];
                    for (let c of $data.sharing.query.content) {
                        $data.sharing.query.group.push("cnt:"+c); 
                    }
				}
		    	if ($data.sharing.query.group && !Array.isArray($data.sharing.query.group)) { $data.sharing.query.group = [ $data.sharing.query.group ]; }
				me.updateSharingLabels();
			}
			
			if ($data.consent.owner) {				
				users.getMembers({ "_id" : $data.consent.owner }, [ "firstname", "lastname", "city", "address1", "address2", "country", "role"])
				.then(function(result) { $data.owner = result.data[0]; });
			}
			
			$data.writeProtect = false;
		}
		
		if ($route.query.authorize != null) {
			$data.consent.type = "HEALTHCARE";			
			$data.consent.authorized = [ $route.query.authorize ];
			
			hc.search({ "_id" :  $route.query.authorize }, [ "firstname", "lastname", "city", "address1", "address2", "country", "role"])
			.then(function(data) {
				$data.authpersons = data.data;
				if (data.data.length > 0) {
				  $data.consent.name = me.getName($data.authpersons[0]);
				}
			});
			
			me.doBusy(usergroups.search({ "_id" : $route.query.authorize }, ["name"]))
			.then(function(data2) {
				for (let userGroup of data2.data) {
					$data.consent.entityType = "USERGROUP"
					$data.authteams.push(userGroup);
					$data.consent.name = me.getName($data.authteams[0]);
				}
			});
		}
		
		
				
	},
	updateSharingLabels() {
        const { $data, $route, $router } = this, me = this;
		$data.groupLabels = [];
		$data.groupExcludeLabels = [];
		if ($data.sharing && $data.sharing.query) {
			var sq = $data.sharing.query;
		
		 
			
			if (sq.content) {
				for (let r of sq.content) {
				  if (r === "Patient" || r === "Group" || r === "Person" || r === "Practitioner") return;
				  labels.getContentLabel(getLocale(), r).then(function(lab) {
					  if ($data.groupLabels.indexOf(lab)<0) $data.groupLabels.push(lab); 
				  });
				}
			}
			if (sq.group) {
				for (let r of sq.group) {
					  labels.getGroupLabel(getLocale(), sq["group-system"], r).then(function(lab) {
						  if ($data.groupLabels.indexOf(lab)<0) $data.groupLabels.push(lab); 
					  });
				}
			}
			
			
			if ($data.sharing.query["group-exclude"]) {
				for (let grp of $data.sharing.query["group-exclude"]) { 
					labels.getGroupLabel($data.lang, $data.sharing.query["group-system"] || "v1", grp).then(function(label) { $data.groupExcludeLabels.push(label); });
				}
			}
		}
	},
	create() {	
		const { $data, $route, $router } = this, me = this;
		if (!$data.consent.name) $data.consent.name = me.getName({});
									
		$data.consent.writes = $data.consent.writesBool ? "UPDATE_AND_CREATE" : "NONE";		
				
		me.doAction("create", circles.createNew($data.consent))		
		.then(function(result) {
			$data.consent = result.data;
			if ($data.sharing && $data.sharing.query) {
			   records.share(result.data._id, null, $data.consent.type, $data.sharing.query)
			   .then(function() { 
				   if (!actions.showAction($router, $route)) {
				     $router.push({ path : "./records", query : { selectedType : "circles", selected : result.data._id }});
				   }
			   });
			} else {
			  if (!actions.showAction($router, $route)) {
			    $router.push({ path : "./records", query : { selectedType : "circles", selected : result.data._id }});
			  }
			}
		});
					
	},
	
	removePerson(person) {
        const { $data, $route, $router } = this, me = this;
		if ($data.consentId) {
		    me.doAction("delete", server.delete(jsRoutes.controllers.Circles.removeMember($data.consent._id, person._id).url).
			then(function() {
				$data.consent.authorized.splice($data.consent.authorized.indexOf(person._id), 1);
				if ($data.consent.entityType == "USERGROUP") {
				  $data.authteams.splice($data.authteams.indexOf(person), 1);
				} else {
				  $data.authpersons.splice($data.authpersons.indexOf(person), 1);
				}
			}));
		} else {
			if ($data.consent.entityType == "USERGROUP") {
				  $data.authteams.splice($data.authteams.indexOf(person), 1);
				  $data.consent.authorized.splice($data.consent.authorized.indexOf(person._id), 1);
			} else {
				  $data.authpersons.splice($data.authpersons.indexOf(person), 1);
				  $data.consent.authorized.splice($data.consent.authorized.indexOf(person._id), 1);
				  
			}	
			if ($data.consent.authorized.length==0) $data.consent.entityType = undefined;
		}
	},
	
	addPerson(person, isTeam) {	
		const { $data, $route, $router } = this, me = this;
		console.log(person);
		if (person.members) isTeam = true;
		$data.setupProvidersearch = null;
		$data.setupAdduser = null;
		$data.setupSearchGroup = null;
		$data.setupAddowner = null;

		if (isTeam) {
			$data.authteams.push(person);
			$data.consent.authorized.push(person._id);
			$data.consent.entityType = "USERGROUP";
			if (!$data.consent.name) $data.consent.name = me.getName(person);
		} else {
			$data.consent.entityType = "USER";
			if (typeof person == "string") {
				if ($data.consent.authorized && $data.consent.authorized.length) return;
				if (!$data.consent.externalAuthorized) $data.consent.externalAuthorized = [];
				$data.consent.externalAuthorized.push(person);
			} else if (person.length) {
				for (let p of person) { 
					if (p.role) {
					  $data.authpersons.push(p); 
					  $data.consent.authorized.push(p._id);
					  if (!$data.consent.name) $data.consent.name = me.getName(p);
					}
				}
		    } else if (person.role) {
			    $data.authpersons.push(person);
			    $data.consent.authorized.push(person._id);
			    if (!$data.consent.name) $data.consent.name = me.getName(person);
		    }
		}
		if ($data.consentId) {
			circles.addUsers($data.consentId, $data.consent.authorized, isTeam ? "USERGROUP" : "USER" );
		}
				
	},
	
	confirmPeopleChange() {
        const { $data, $route, $router } = this, me = this;
		$data.confirmNeeded = false;
	},
	
	addPeople() {
        const { $data, $route, $router } = this, me = this;
		if ($data.consent.type != "CIRCLE") {
		  $data.setupProvidersearch = {}; //views.setView("providersearch", { callback : addPerson });	
		} else {
		  $data.setupAdduser = { consent : $data.consent };
		}		
	},
	
    setOwnerPerson(person, isTeam) {	
		const { $data, $route, $router } = this, me = this;
		if (isTeam) return;			
		if (Array.isArray(person)) person = person[0];
			
		if (person._id) {
		  $data.owner = person;
		  $data.consent.owner = person._id;
		} else if (person != null) {
			$data.consent.externalOwner = person;
		}
	},
	
	setOwner() {	
        const { $data, $route, $router } = this, me = this;	
		//views.setView("addusers", { consent : $data.consent, callback : setOwnerPerson });			
	},
	
	addUserGroup() {
		const { $data, $route, $router } = this, me = this;	
		$data.setupSearchGroup= {};		
	},
	
	
	
	addYourself() {
        const { $data, $route, $router } = this, me = this;
		$data.consent.authorized.push(session.user._id);
		$data.consent.entityType = "USER";
		users.getMembers({ "_id" : session.user._id }, [ "firstname", "lastname", "city", "address1", "address2", "country", "role"])
		.then(function(result) { $data.authpersons.push(result.data[0]); });		
	},
	
	deleteConsent() {
        const { $data, $route, $router } = this, me = this;
		circles.unconfirmed = 0;
		server.delete(jsRoutes.controllers.Circles["delete"]($data.consent._id).url).
		then(function() {
			if (session.user.role == "MEMBER" && ($data.consent.type == "EXTERNALSERVICE" || $data.consent.type == "API")) {
				$router.push({ path : "./apps" });
			} else if (session.user.role == "MEMBER" && $data.consent.type == "STUDYPARTICIPATION") {
			    $router.push({ path : "./studies" });
			} else {
			    $router.push({ path : "./circles" });
			}
		});
	},
	
	rejectConsent() {
        const { $data, $route, $router } = this, me = this;
		circles.unconfirmed = 0;
		hc.reject($data.consent._id).then(function() { me.reinit(); });
	},
	
	confirmConsent() {
        const { $data, $route, $router } = this, me = this;
		circles.unconfirmed = 0;
		hc.confirm($data.consent._id).then(function() { $data.reinit(); });	
	},
	
	mayReject() {
        const { $data, $route, $router } = this, me = this;
		if (! $data.consent) return false;
		//if ($scope.consent.owner !== $scope.userId) return false;
		return ($data.consent.status == 'UNCONFIRMED' || $data.consent.status == 'ACTIVE') && $data.consent.type != 'STUDYPARTICIPATION';
	},
	
	mayConfirm() {
		if (! $data.consent) return false;
		if ($data.consent.owner !== $data.userId) return false;
		return $data.consent.status == 'UNCONFIRMED';
	},
	
	mayDelete() {
        const { $data, $route, $router } = this, me = this;		
		if (! $data.consent) return false;
		if ($data.consent.owner !== $data.userId) return false;
		
		return ($data.consent.status == 'ACTIVE' || $data.consent.status == 'REJECTED') && ($data.consent.type != 'STUDYPARTICIPATION' && $data.consent.type != 'HEALTHCARE');
	},
	
	mayChangeUsers() {
        const { $data, $route, $router } = this, me = this;
		if (! $data.consent) return false;
		if ($data.writeProtect) return false;
		if ($data.consent.status == 'ACTIVE' && $data.consent.authorized.length==1) return false;
		if ($data.isSimple) return false;
		return true;
	},
	
	mayAddPeople() {
        const { $data, $route, $router } = this, me = this;
		if (! $data.consent) return false;	
		if ($data.consent.type == "EXTERNALSERVICE") return false;
		if ($data.consent.type == "API") return false;
		if ($data.consent.type == "STUDYRELATED") return false;
		if ($data.consent.type == "IMPLICIT") return false;
		if ($data.isSimple) return false;
		return true;
	},
	
	mayChangeData() {
        const { $data, $route, $router } = this, me = this;
		if (! $data.consent) return false;
		if ($data.writeProtect) return false;
		if ($data.isSimple) return false;
		return true;
	},
	
	maySkip() {
        const { $data, $route, $router } = this, me = this;
		return $route.query.action != null && $data.consent && $data.consent.status != "UNCONFIRMED";
	},
	
	mayBack() {
        const { $data, $route, $router } = this, me = this;
		return !$route.query.action;
	},
	
	skip() {
        const { $data, $route, $router } = this, me = this;
		if (!actions.showAction($router, $route)) {
		      me.reload();
		}
	},
	
	showStudyDetails() {
        const { $data, $route, $router } = this, me = this;
		$router.push({ path : './studydetails', query : { studyId : $data.consent._id } });		
	},
	
	showPasscode() {
        const { $data, $route, $router } = this, me = this;
		me.doAction('passcode', circles.listConsents({ "_id" : $route.query.consentId }, ["type", "passcode" ]))
		.then(function(data) {
			$data.consent.passcode = data.data[0].passcode;
		});
	},
	
	goBack() {
        const { $data, $route, $router } = this, me = this;
		$router.go(-1);
	},
		
	reinit() {	
        const { $data, $route, $router } = this, me = this;	
		if (!actions.showAction($router, $route)) me.init($data.userId);		
	},
	
	
		
	
	getIconRole(item) {
		if (!item) return "/images/account.jpg";
		if (item == "team") return "/images/team.jpeg";
		if (item == "app") return "/images/app.jpg";
		if (item == "community") return "/images/community.jpeg";
		if (item == "external") return "/images/question.jpeg";
		if (item == "reshare") return "/images/community.jpeg";
		if (session.user && item._id == session.user._id) return "/images/account.jpg";
		if (item=="member" || item.role == "MEMBER") return "/images/account.jpg";
		if (item=="research" || item.role == "RESEARCH") return "/images/research2.jpeg";
		if (item=="provider" || item.role == "PROVIDER") return "/images/doctor.jpeg";
		return "";
	},
        
    },

    created() {
        const { $data, $route } = this, me = this;
        $data.pleaseReview = ($route.query.action != null);
        session.currentUser.then(function(userId) {	
            me.init(userId);
        });
    }
    
}
</script>