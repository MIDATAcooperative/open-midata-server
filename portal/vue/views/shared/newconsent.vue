<!--
 This file is part of the Open MIDATA Server.
 
 The Open MIDATA Server is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 any later version.
 
 The Open MIDATA Server is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.
 
 You should have received a copy of the GNU General Public License
 along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
-->
<template>
	<div>
    <panel :title="getTitle()" :busy="isBusy">
    
        <div class="alert alert-info" v-if="pleaseReview && !consentId">
            <h4 class="alert-heading" v-t="'newconsent.please_review1'"></h4>
            <p v-t="'newconsent.please_review2'"></p>
        </div>

		<div class="alert alert-info d-none d-md-block" v-if="pleaseReview && consentId">
    		<h4 class="alert-heading" v-t="'editconsent.please_review1'"></h4>
    		<p v-if="consent.status=='UNCONFIRMED'" v-t="'editconsent.please_review2a'"></p>
    		<p v-else v-t="'editconsent.please_review2b'"></p>
  		</div>

        <form name="myform" ref="myform" class="css-form form-horizontal" @submit.prevent="create()" novalidate role="form">
            <error-box :error="error"></error-box>
    
            <div class="row" v-if="!consent.type">
                <div class="col-md-3 col-6 mb-3">
                    <div class="card button" @click="consent.type='CIRCLE';">
                        <img :src="getIconRole('member')" class="card-img-top">
                        <div class="card-body">                
                            <span class="card-text"  v-t="'newconsent.share_with_members'"></span>
                            <a href="javascript:" class="card-link" v-t="'newconsent.select'"></a>      
                        </div>
                    </div>
                </div>
        
                <div class="col-md-3 col-6 mb-3">
                    <div class="card"  @click="consent.type='HEALTHCARE';consent.writesBool=true;">
                        <img :src="getIconRole('provider')" class="card-img-top">
                        <div class="card-body">                
                            <span class="card-text" v-t="'newconsent.share_with_provider'"></span>  
                            <a href="javascript:" class="card-link" v-t="'newconsent.select'"></a>     
                        </div>
                    </div>
                </div>

				<div class="col-md-3 col-6 mb-3">
                    <div class="card"  @click="consent.type='REPRESENTATIVE';consent.writesBool=true;consent.query={group:'all'};">
                        <img :src="getIconRole('representative')" class="card-img-top">
                        <div class="card-body">                
                            <span class="card-text" v-t="'newconsent.share_representative'"></span>  
                            <a href="javascript:" class="card-link" v-t="'newconsent.select'"></a>     
                        </div>
                    </div>
                </div>
            </div>
			<div v-else>
				<div class="row extraspace" v-if="consent && consent.type=='REPRESENTATIVE'">
					<div class="col-12"><div class="alert alert-warning" v-t="'editconsent.type_representative'"></div></div>
				</div>

				<div class="row extraspace" v-if="consentId">  
                      
      				<div class="col-md-6">      
        				<div><small v-t="'editconsent2.status'"></small></div>
        
      
        				<span class="lead text-success" v-if="consent.status == 'ACTIVE'" v-t="'editconsent2.status_active'"></span>
        				<span class="lead text-success" v-if="consent.status == 'PRECONFIRMED'" v-t="'editconsent2.status_preconfirmed'"></span>
        				<span class="lead text-danger" v-if="consent.status == 'EXPIRED'" v-t="'editconsent2.status_expired'"></span>
        				<span class="lead text-warning" v-if="consent.status == 'UNCONFIRMED'" v-t="'editconsent2.status_unconfirmed'"></span>    
        				<span class="lead text-danger" v-if="consent.status == 'REJECTED'" v-t="'editconsent2.status_rejected'"></span>
        				<span class="lead text-danger" v-if="consent.status == 'INVALID'" v-t="'editconsent2.status_invalid'"></span>
        			</div>
              
        			<div class="col-md-6">
        				<div><small v-t="'editconsent2.duration'"></small></div>
           				<div class="lead">							       
         					{{ $filters.date(consent.dateOfCreation) }}      
       						-      
       						<span v-if="consent.validUntil"> 
       							{{ $filters.date(consent.validUntil) }}      
      						</span>      
         				</div>                
      				</div>					
     			</div>	      
    
				<div class="row" >					
					<div class="col-md-5" :class="{ 'd-none d-md-block' : (consentId && owner && owner._id == userId) }"> 
						<p><b class="text-primary" v-t="'newconsent.who_is_owner'"></b></p>
			
						<div class="" v-if="owner && owner._id != userId">
							<div class="card-body">
								<img :src="getIconRole(owner)" class="float-start consenticon">
								<div class="iconspace">
									<div>{{ $t('enum.userrole.'+owner.role)}}</div>		  	
									<address>
                                        <span v-if="owner.testUserApp" class="badge text-bg-warning me-1"><span class="fas fa-vial" title="Test User"></span></span>
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
						<div v-if="owner && owner._id == userId">
		  					<div class="card-body">
		    					<img :src="getIconRole(owner)" class="float-start consenticon">
		    					<div class="iconspace"><div>&nbsp;</div>
                                <span v-if="owner.testUserApp" class="badge text-bg-warning me-1"><span class="fas fa-vial" title="Test User"></span></span><strong v-t="'editconsent.you'"></strong></div>
		  					</div>
						</div>
						<div v-if="consent.type=='STUDYRELATED'">
		  					<div class="card-body">
		  						<img :src="getIconRole('research')" class="float-start consenticon">
          						<div class="iconspace">
		    						<div v-t="'editconsent.project'"></div>
		    						<div><strong>{{ consent.ownerName }}</strong></div>
		  						</div>
		  					</div>
						</div>
						<div class="" v-if="consent.externalOwner">
							<div class="card-body">
							<img src="/images/question.jpeg" class="float-start consenticon">
							<div class="iconspace">
								<div v-t="'editconsent.external'"></div>
								<div><strong>{{ consent.externalOwner }}</strong></div>
							</div>
						</div>
					</div>
					<div class="margin-top mb-3">
						<button v-if="!(owner || consent.externalOwner)" type="button" class="btn btn-default" @click="setOwner();" :disabled="action!=null" v-t="'newconsent.set_owner_btn'"></button>
					</div>
							
				</div>
				<div class="col-md-1 d-none d-md-block">
					<div style="margin-top:60px" class="text-center">
						<span style="font-size:40px" class="fas fa-arrow-right"></span>
					</div>
				</div>
				<div class="col-md-6">
					<p><b class="text-primary" v-t="'editconsent.people'"></b></p>
					<div  v-if="consent.type=='EXTERNALSERVICE'">
						<div class="card-body">
							<img :src="getIconRole('app')" class="consenticon float-start">
							<div class="iconspace">
								<strong v-t="'editconsent2.external'"></strong>
							</div>
						</div>
					</div>
					<div v-if="consent.type=='API'">
						<div class="card-body">
							<img :src="getIconRole('app')" class="consenticon float-start">
							<div class="iconspace">
								<strong v-t="'editconsent2.external'"></strong>
							</div>
						</div>
					</div>
					<div v-if="consent.type=='STUDYRELATED'">
						<div class="card-body">
							<img :src="getIconRole('community')" class="consenticon float-start">
							<div class="iconspace">
								<strong v-t="'editconsent2.community'"></strong>
							</div>
						</div>
					</div>
					
					<div v-if="consent.type != 'EXTERNALSERVICE' && consent.type != 'API'">
						<div v-for="person in authpersons" :key="person._id">
							<div class="card-body">
								<button type="button" @click="removePerson(person)" :disabled="action!=null" class="btn-close" aria-label="Delete" v-if="mayChangeUsers()"><span aria-hidden="true">&times;</span></button>
								<img :src="getIconRole(person)" class="float-start consenticon">
								<div class="iconspace">
									<div>{{$t('enum.userrole.'+person.role)}}</div>		
									<address>
                                         <span v-if="person.testUserApp" class="badge text-bg-warning me-1"><span class="fas fa-vial" title="Test User"></span></span>
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
					</div>
					<div v-for="usergroup in authteams" :key="usergroup._id">
						<div class="card-body">
							<button type="button" :disabled="action!=null" v-if="mayChangeUsers()" @click="removePerson(usergroup)" class="close" aria-label="Delete"><span aria-hidden="true">&times;</span></button>
							<img :src="getIconRole(usergroup.type=='ORGANIZATION' ? 'organization' : 'team')" class="float-start consenticon">
							<div class="iconspace">
								<div v-t="'enum.usergrouptype.'+usergroup.type"></div>	
								<strong>{{ usergroup.name }}</strong>
								 <address v-if="usergroup.org">                                      
		    {{ usergroup.org.address1 }}<br>
			{{ usergroup.org.address2 }}<br>
			{{ usergroup.org.zip }} {{ usergroup.org.city }}<br>
			{{ usergroup.org.country }}<br><br>			
			<span v-if="usergroup.org.phone"><span v-t="'common.user.phone'"></span>: {{ usergroup.org.phone }}</span>
		                </address>
							</div>
						</div>
					</div>
					<div v-for="person in consent.externalAuthorized" :key="person">
						<div class="card-body">
							<img :src="getIconRole('external')" class="float-start consenticon">
							<div class="iconspace">
								<div v-t="'editconsent.external'"></div>
								<strong>{{ person }}</strong>
							</div>
						</div>
					</div>
					<div v-if="consent.reshare">
						<div class="card-body">
							<img :src="getIconRole('reshare')" class="float-start consenticon">
							<div class="iconspace">
								<span v-t="'newconsent.reshare'"></span>
							</div>
						</div>
					</div>
			
					<div class="margin-top" v-if="mayAddPeople()">
						<button type="button" :disabled="action!=null" class="btn btn-default me-1" :class="{ 'btn-sm' : consent.authorized.length }" v-show="consent.owner != userId && consent.authorized.indexOf(userId)<0" @click="addYourself();" v-t="'newconsent.add_yourself_btn'"></button>
						<button type="button" :disabled="action!=null" class="btn btn-default me-1" :class="{ 'btn-sm' : consent.authorized.length }" v-show="consent.entityType!='USERGROUP' && consent.entityType!='ORGANIZATION'" @click="addPeople();" v-t="'newconsent.add_person_btn'"></button>
						<button type="button" :disabled="action!=null" class="btn btn-default me-1" :class="{ 'btn-sm' : consent.authorized.length }" v-show="consent.entityType!='USER' && consent.type!='CIRCLE' && consent.type!='REPRESENTATIVE'" @click="addOrganization();" v-t="'newconsent.add_organization_btn'"></button>
						<button type="button" :disabled="action!=null" class="btn btn-default me-1" :class="{ 'btn-sm' : consent.authorized.length }" v-show="consent.entityType!='USER' && consent.type!='CIRCLE' && consent.type!='REPRESENTATIVE'" @click="addUserGroup();" v-t="'newconsent.add_usergroup_btn'"></button>
					</div>
					<div class="extraspace"></div>
			
				</div>
			</div>
        </div>
    
        
	
        <div v-if="sharing.records || sharing.query" class="margin-top">
        
            <p><b class="text-primary" v-t="'editconsent.what_is_shared'"></b></p>
        <!-- <div v-if="groupLabels.length && groupLabels.length < 5">{{ groupLabels.join(", ") }}</div>  -->
		        <p v-if="consent.type=='REPRESENTATIVE'" v-t="'editconsent.type_representative'"></p>
				<div v-else>
					<ul v-if="groupLabels && groupLabels.length">
						<li v-for="label in groupLabels" :key="label">{{ label }}</li>
					</ul>
					<div v-if="groupExcludeLabels && groupExcludeLabels.length">
						<span v-t="'editconsent2.exclude'"></span>: {{ groupExcludeLabels.join(", ") }}
					</div>
				</div>
                <p v-if="(!sharing.records || sharing.records.length == 0) && !sharing.query.group.length" v-t="'editconsent.consent_empty'"></p>
                <p v-if="sharing.records && sharing.records.length">{{ $t('editconsent.shares_records', { count : sharing.records.length }) }}</p>
				
                <div class="extraspace"></div>
                <p><b class="text-primary" v-t="'editconsent.restrictions'"></b></p>
                <p>{{ $t('enum.writepermissiontype.'+(consent.writes || 'NONE')) }}</p>
                <p v-if="consent.createdAfter"><span v-t="'editconsent.created_after'"></span>:{{ $filters.date(consent.createdAfter) }}</p>
                <p v-if="consent.createdBefore"><span v-t="'editconsent.created_before'"></span>:{{ $filters.date(consent.createdBefore) }}</p>
                       
        </div>
        
        <div v-if="consent.basedOn">
           <div class="extraspace"></div>
           <p><b class="text-primary" v-t="'editconsent.source'"></b></p>
           <p><span v-t="'editconsent.based_on'"></span>: <router-link :to="{ path : './editconsent', query : { consentId : consent.basedOn._id }}">{{ consent.basedOn.name }}</router-link></p>
           <p><span v-t="'editconsent.base_created_at'"></span>: {{ $filters.date(consent.basedOn.dateOfCreation) }}</p>
        </div>
        
        <div v-if="consent.allowedReshares">
           <div class="extraspace"></div>
           <p><b class="text-primary" v-t="'editconsent.allowed_reshares'"></b></p>
           <ul>
           <li v-for="reshare in consent.allowedReshares">
              <span v-if="reshare.type=='SERVICES'"><span v-t="'editconsent.reshare_service'"></span>: {{ reshare.name }}</span>
              <span v-if="reshare.type=='USERGROUP'"><span v-t="'editconsent.reshare_usergroup'"></span>: {{ reshare.name }}</span>
              <span v-if="reshare.type=='USER'"><span v-t="'editconsent.reshare_user'"></span>: {{ reshare.name }}</span>
              <span v-if="reshare.type=='PROJECT'"><span v-t="'editconsent.reshare_project'"></span>: {{ reshare.name }}</span>
           </li>
           </ul>
        </div>
            
        <div v-if="options.advanced" class="margin-top">
            <div class="extraspace"></div>
            <form-group name="writes" label="newconsent.writes" class="midata-checkbox-row">
                <check-box v-model="consent.writesBool" name="writes">
                    <span v-t="'newconsent.writes2'"></span>
                </check-box>	    
            </form-group>
        
            <form-group name="reshare" label="newconsent.reshare" class="midata-checkbox-row">
                <check-box v-model="consent.reshare" name="reshare">
                    <span v-t="'newconsent.reshare2'"></span>
                </check-box>
            </form-group>
      
        
            <form-group name="passcode" label="newconsent.use_passcode" v-if="consent.type == 'HEALTHCARE' && consent.owner == userId" class="midata-checkbox-row">
                <check-box v-model="consent.usepasscode" name="usepasscode">
                    <span v-t="'newconsent.use_passcode2'"></span>
                </check-box>    
            </form-group>

            <form-group name="passcode2" label="newconsent.choose_passcode" v-if="consent.usepasscode">	      
                <input  id="passcode2" name="passcode2" type="text" class="form-control" v-validate v-model="consent.passcode" required>     
            </form-group>    
            
            <form-group name="validUntil" label="newconsent.expiration_date">        
                <input id="validUntil" type="date" class="form-control" v-validate v-date="consent.validUntil" v-model="consent.validUntil" >              
            </form-group>

            <form-group name="createdAfter" label="newconsent.created_after">	  
                <input id="createdAfter" type="date" class="form-control" v-validate v-date="consent.createdAfter" v-model="consent.createdAfter"  />              
            </form-group>

            <form-group name="createdBefore" label="newconsent.created_before">	  
                <input id="createdBefore" type="date" class="form-control" v-validate v-date="consent.createdBefore" v-model="consent.createdBefore"  />              
            </form-group>

            <form-group name="name" label="newconsent.name">
                <input id="name" name="name" type="text" class="form-control" v-validate v-model="consent.name">
            </form-group>
        </div>
            
        <div v-if="!consentId && consent.type" class="margin-top">
            <button type="button" :disabled="action!=null" @click="skip();" v-if="maySkip()" class="btn btn-default space" v-t="'common.skip_btn'"></button>
            <span v-if="!consent.query">
                <span v-if="!pleaseReview">
                    <button :disabled="action!=null" v-if="consent.authorized.length || consent.usepasscode || consent.externalAuthorized" type="submit" v-submit class="btn btn-primary" v-t="'newconsent.create_btn'"></button>
                </span>
                <span v-if="pleaseReview">
                    <button :disabled="action!=null" v-if="consent.authorized.length || consent.usepasscode || consent.externalAuthorized" type="submit" v-submit class="btn btn-primary" v-t="'newconsent.create2_btn'"></button>
                </span>
            </span>
            <span v-if="consent.query">
                <button :disabled="action!=null" v-if="consent.authorized.length || consent.usepasscode" type="submit" v-submit class="btn btn-primary" v-t="'newconsent.create2_btn'"></button>
            </span>     
            <button :disabled="action!=null" type="button" class="btn btn-link" v-if="!options.advanced" @click="options.advanced=true;" v-t="'newconsent.extended_btn'"></button>
        </div>

		<div v-if="consentId">
			<div v-if="mayChangeData()" class="extraspace mb-3">
				<router-link class="btn btn-default"  :to="{ path : './records', query : { selected : consentId, selectedType : 'circles' }}" v-t="'editconsent.view_change_selection_btn'"></router-link>
			</div>
    		<div class="d-block d-md-none">	
    			<div v-if="pleaseReview">
    				<hr>    
    				<p v-if="consent.status=='UNCONFIRMED'" v-t="'editconsent.please_review2a'"></p>
    				<p v-else v-t="'editconsent.please_review2b'"></p>
    			</div>
    
				<div class="d-grid gap-2 mt-3 mb-2">	
				<button type="button" :disabled="action!=null" @click="confirmConsent();" v-if="mayConfirm()" class="btn btn-primary btn-lg" v-t="'editconsent.confirm_btn'"></button>
				<button type="button" :disabled="action!=null" @click="rejectConsent();" v-if="mayReject()" class="btn btn-danger" v-t="'editconsent.reject_btn'"></button>
    			<button type="button" :disabled="action!=null" @click="deleteConsent();" v-if="mayDelete()" class="btn btn-danger" v-t="'editconsent.delete_btn'"></button>
				<button type="button" :disabled="action!=null" @click="deleteAllConsent();" v-if="mayDelete() && consent.type=='EXTERNALSERVICE'" class="btn btn-danger" v-t="'editconsent.delete_all_btn'"></button>
    			<button type="button" :disabled="action!=null" @click="leave()" class="btn btn-default" v-if="mayBack()" v-t="'common.back_btn'"></button>
				<button type="button" :disabled="action!=null" @click="skip();" v-if="maySkip()" class="btn btn-default" v-t="'common.skip_btn'"></button>
				</div>
    		</div>
    		<div class="d-none d-md-block">
				<button type="button" :disabled="action!=null" @click="leave()" class="btn btn-default space" v-if="mayBack()" v-t="'common.back_btn'"></button>
				<button type="button" :disabled="action!=null" @click="skip();" v-if="maySkip()" class="btn btn-default space" v-t="'common.skip_btn'"></button>
				<button type="button" :disabled="action!=null" @click="confirmConsent();" v-if="mayConfirm()" class="btn btn-primary space" v-t="'editconsent.confirm_btn'"></button>
				<button type="button" :disabled="action!=null" @click="rejectConsent();" v-if="mayReject()" class="btn btn-danger space" v-t="'editconsent.reject_btn'"></button>
    			<button type="button" :disabled="action!=null" @click="deleteConsent();" v-if="mayDelete()" class="btn btn-danger space" v-t="'editconsent.delete_btn'"></button>
				<button type="button" :disabled="action!=null" @click="deleteAllConsent();" v-if="mayDelete() && consent.type=='EXTERNALSERVICE'" class="btn btn-danger" v-t="'editconsent.delete_all_btn'"></button>
    		</div>
		</div>

          
        </form>
    </panel>
	
	<modal id="provSearch" :full-width="true" @close="setupProvidersearch=null" :open="setupProvidersearch!=null" :title="$t('providersearch.title')">
	   <provider-search :setup="setupProvidersearch" @add="addPerson"></provider-search>
	</modal>
	
	<modal id="organizationSearch" :full-width="true" @close="setupOrganizationSearch=null" :open="setupOrganizationSearch!=null" :title="$t('organizationsearch.title')">
	   <organization-search :setup="setupOrganizationSearch" @add="addPerson"></organization-search>
	</modal>
	
	<modal id="setupUser" :full-width="true" @close="setupAdduser=null" :open="setupAdduser!=null" :title="$t('dashboard.addusers')">
	  <add-users :setup="setupAdduser" @close="setupAdduser=null" @add="addPerson"></add-users>
	</modal>

	<modal id="addOwner" :full-width="true" @close="setupAddowner=null" :open="setupAddowner!=null" :title="$t('dashboard.addusers')">
	  <add-users :setup="setupAddowner" @close="setupAddowner=null" @add="setOwnerPerson"></add-users>
	</modal>

	<modal id="searchGroup" :full-width="true" @close="setupSearchGroup=null" :open="setupSearchGroup!=null" :title="$t('dashboard.usergroupsearch')">
	  <user-group-search :setup="setupSearchGroup" @close="setupSearchGroup=null" @add="addPerson"></user-group-search>
	</modal>

	</div>
</template>
<script>
import { status, CheckBox, ErrorBox, FormGroup, Modal } from 'basic-vue3-components';
import server from 'services/server';
import circles from 'services/circles';
import apps from 'services/apps';
import session from 'services/session';
import actions from 'services/actions';
import labels from 'services/labels';
import records from 'services/records';
import usergroups from 'services/usergroups';
import users from 'services/users';
import hc from 'services/hc';
import { getLocale } from 'services/lang';
import ProviderSearch from "components/tiles/ProviderSearch.vue"
import OrganizationSearch from "components/tiles/OrganizationSearch.vue"
import Panel from "components/Panel.vue"
import AddUsers from "components/tiles/AddUsers.vue"
import UserGroupSearch from "components/tiles/UserGroupSearch.vue"


export default {
	data: () => ({
        types : [
	        { value : "CIRCLE", label : "enum.consenttype.CIRCLE"},
			{ value : "REPRESENTATIVE", label : "enum.consenttype.REPRESENTATIVE"},
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
		setupOrganizationSearch : null,
		setupAdduser : null,
		setupAddowner : null,
		setupSearchGroup : null
	}),		
    
    components: { ErrorBox, CheckBox, Panel, FormGroup, ProviderSearch, OrganizationSearch, AddUsers, Modal, UserGroupSearch },

    mixins : [ status ],

    methods : {

		getTitle() {
			const { $t, $data } = this;
			if ($data.consentId && $data.consent) return $t('editconsent.title')+": "+$data.consent.name;
			return $t('newconsent.title');
		},

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
			
			me.doBusy(circles.listConsents({ "_id" : $route.query.consentId }, ["name", "type", "status", "owner", "ownerName", "authorized", "entityType", "createdBefore", "createdAfter", "validUntil", "externalOwner", "externalAuthorized", "sharingQuery", "dateOfCreation", "writes", "allowedReshares", "basedOn" ])
			.then(function(data) {
				if (!data.data || !data.data.length) {
					$data.consent = null;
					return;
				}								
				
				$data.consent = data.data[0];
				
				if ($data.consent.type == "CIRCLE") $data.isSimple = false;
								
				if ($data.consent.entityType == "USERGROUP" || $data.consent.entityType == "ORGANIZATION") {
					me.doBusy(usergroups.search({ "_id" : $data.consent.authorized }, ["name", "status", "type"]))
					.then(function(data2) {
						for (let userGroup of data2.data) {
						    userGroup.org = null;
						    $data.authteams.push(userGroup);
						    if (userGroup.type == "ORGANIZATION") {
						       me.doBusy(hc.getOrganization(userGroup._id).
						       then(function(data3) {
						          userGroup.org = data3.data; 
						       }
						       ));					        
						    } 
						}
					});
				} else {
	                var role = ($data.consent.type == "HEALTHCARE") ? "PROVIDER" : null;				
					for (let p of $data.consent.authorized) {					
						$data.authpersons.push(session.resolve(p, function() {
							var res = { "_id" : p };
							if (role) res.role = role;
							return users.getMembers(res, (role == "PROVIDER" ? users.ALLPUBLIC : users.MINIMAL )); 
						}));
					}
				}
				
				
				if ($data.consent.owner && $data.consent.type!="STUDYRELATED") {
					me.doBusy(users.getMembers({ "_id" : $data.consent.owner }, [ "firstname", "lastname", "email", "role", "testUserApp"])
					.then(function(result) { $data.owner = result.data[0]; }));
				}
				
				$data.writeProtect = ($data.consent.owner !== userId && $data.consent.status !== "UNCONFIRMED") || $data.consent.type === "EXTERNALSERVICE" || $data.consent.type === "API" || $data.consent.type === "STUDYPARTICIPATION" || $data.consent.status === "EXPIRED" || $data.consent.status === "REJECTED";
			
				me.doBusy(server.get(jsRoutes.controllers.Records.getSharingDetails($route.query.consentId).url)).
				then(function(results) {				
				    $data.sharing = results.data;
				    
				    if ($data.sharing.query) {
				    	$data.sharing.query = labels.simplifyQuery($data.sharing.query);				    	
				    	if ($data.sharing.query["group-exclude"] && !Array.isArray($data.sharing.query["group-exclude"])) { $data.sharing.query["group-exclude"] = [ $data.sharing.query["group-exclude"] ]; }
				    	if ($data.sharing.query.group && !Array.isArray($data.sharing.query.group)) { $data.sharing.query.group = [ $data.sharing.query.group ]; }
				    	me.updateSharingLabels();
				    }
				});

				
			}));
			
		} else {
			$data.isSimple = false;
			$data.consent = { type : ($route.meta.role == "provider" ? "HEALTHCARE" : null), status : "ACTIVE", authorized : [], writes : "NONE", writesBool : false };
			if ($route.meta.role == "provider" || $route.query["allow-write"]==="true") {
			  $data.consent.writesBool = true;
			  $data.consent.writes = $data.consent.writesBool ? "UPDATE_AND_CREATE" : "NONE";
			}
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
				me.doBusy(users.getMembers({ "_id" : $data.consent.owner }, [ "firstname", "lastname", "city", "address1", "address2", "country", "role", "testUserApp"])
				.then(function(result) { $data.owner = result.data[0]; }));
			} else me.ready();
			
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
				  if (r === "Patient" || r === "Group" || r === "Person" || r === "Practitioner") continue;
				  me.doBusy(labels.getContentLabel(getLocale(), r).then(function(lab) {
					  if ($data.groupLabels.indexOf(lab)<0) $data.groupLabels.push(lab); 
				  }));
				}
			}
			if (sq.group) {
				for (let r of sq.group) {				    
					  me.doBusy(labels.getGroupLabel(getLocale(), sq["group-system"], r).then(function(lab) {
						  if ($data.groupLabels.indexOf(lab)<0) $data.groupLabels.push(lab); 
					  }));
				}
			}
			
			
			if ($data.sharing.query["group-exclude"]) {
				for (let grp of $data.sharing.query["group-exclude"]) { 
					me.doBusy(labels.getGroupLabel($data.lang, $data.sharing.query["group-system"] || "v1", grp).then(function(label) { $data.groupExcludeLabels.push(label); }));
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
			   me.doAction("create", records.share(result.data._id, null, $data.consent.type, $data.sharing.query)
			   .then(function() { 
				   if (!actions.showAction($router, $route)) {
				     $router.push({ path : "./records", query : { selectedType : "circles", selected : result.data._id }});
				   }
			   }));
			} else {
			  if (!actions.showAction($router, $route)) {
				  if ($data.consent.type=='REPRESENTATIVE') {
                    $router.push({ path : "./editconsent", query : { consentId : $data.consent._id }});
				  } else {
			        $router.push({ path : "./records", query : { selectedType : "circles", selected : result.data._id }});
				  }
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
				if ($data.consent.entityType == "USERGROUP" || $data.consent.entityType == "ORGANIZATION") {
				  $data.authteams.splice($data.authteams.indexOf(person), 1);
				} else {
				  $data.authpersons.splice($data.authpersons.indexOf(person), 1);
				}
			}));
		} else {
			if ($data.consent.entityType == "USERGROUP" || $data.consent.entityType == "ORGANIZATION") {
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
		if (person.resourceType == "Organization") isTeam = true;
		$data.setupProvidersearch = null;
		$data.setupOrganizationSearch = null;
		$data.setupAdduser = null;
		$data.setupSearchGroup = null;
		$data.setupAddowner = null;

		if (isTeam) {
		    if (person.id) {
		        person.org = null;
		        person.type = "ORGANIZATION";
		        me.doBusy(hc.getOrganization(person.id).
				then(function(data3) {
					person.org = data3.data; 
				}));	
		    } else {
		        if (!person.type) person.type = "CARETEAM";
		    }
		
			$data.authteams.push(person);
			$data.consent.authorized.push(person._id || person.id);
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
			me.doAction("add", circles.addUsers($data.consentId, $data.consent.authorized, isTeam ? "USERGROUP" : "USER" ));
		}
				
	},
	
	confirmPeopleChange() {
        const { $data, $route, $router } = this, me = this;
		$data.confirmNeeded = false;
	},
	
	addPeople() {
        const { $data, $route, $router } = this, me = this;
		if ($data.consent.type != "CIRCLE" && $data.consent.type != "REPRESENTATIVE") {
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

		$data.setupAddowner = null;
	},
	
	setOwner() {	
        const { $data, $route, $router } = this, me = this;	
		$data.setupAddowner = { consent : $data.consent };		
	},
	
	addUserGroup() {
		const { $data, $route, $router } = this, me = this;	
		$data.setupSearchGroup= {};		
	},
	
	addOrganization() {
		const { $data, $route, $router } = this, me = this;	
		$data.setupOrganizationSearch = {};		
	},
	
	addYourself() {
        const { $data, $route, $router } = this, me = this;
		$data.consent.authorized.push(session.user._id);
		$data.consent.entityType = "USER";
		me.doAction("add", users.getMembers({ "_id" : session.user._id }, [ "firstname", "lastname", "city", "address1", "address2", "country", "role", "testUserApp"])
		.then(function(result) { $data.authpersons.push(result.data[0]); }));		
	},
	
	leave() {
		const { $data, $route, $router } = this, me = this;
		if (session.user.role == "MEMBER" && ($data.consent.type == "EXTERNALSERVICE" || $data.consent.type == "API")) {
			$router.push({ path : "./apps" });
		} else if (session.user.role == "MEMBER" && $data.consent.type == "STUDYPARTICIPATION") {
		    $router.push({ path : "./studies" });
		} else {
		    $router.push({ path : "./circles" });
		}
	},
	
	deleteConsent() {
        const { $data, $route, $router } = this, me = this;
		circles.unconfirmed = 0;
		me.doAction("delete", server.delete(jsRoutes.controllers.Circles["delete"]($data.consent._id).url).
		then(function() {
			me.leave();
		}));
	},
	
	deleteAllConsent() {
	    const { $data, $route, $router } = this, me = this;
		
		me.doAction("delete", apps.listUserApps([ "type", "status", "applicationId"])
		  .then(function(data) {
			let allapps = data.data;
			let applicationId = null;
			for (let ac of allapps) {
				if (ac._id == $data.consent._id) applicationId = ac.applicationId;
			}
			let all = [];
			if (applicationId) {
				for (let ac of allapps) {
					if (ac.applicationId == applicationId && ac.status=='ACTIVE') {
						all.push(server.delete(jsRoutes.controllers.Circles["delete"](ac._id).url));
					}
				}
				return Promise.all(all);
			}
			
		})
		.then(function() {
			me.leave();
		}));				   		
	},
	
	rejectConsent() {
        const { $data, $route, $router } = this, me = this;
		circles.unconfirmed = 0;
		me.doAction("reject", hc.reject($data.consent._id).then(function() { me.reinit(); }));
	},
	
	confirmConsent() {
        const { $data, $route, $router } = this, me = this;
		circles.unconfirmed = 0;
		me.doAction("confirm", hc.confirm($data.consent._id).then(function() { me.reinit(); }));	
	},
	
	mayReject() {
        const { $data, $route, $router } = this, me = this;
		if (! $data.consent) return false;
		//if ($scope.consent.owner !== $scope.userId) return false;
		return ($data.consent.status == 'UNCONFIRMED' || $data.consent.status == 'INVALID' || $data.consent.status == 'ACTIVE' || $data.consent.status == 'PRECONFIRMED') && $data.consent.type != 'STUDYPARTICIPATION';
	},
	
	mayConfirm() {
		const { $data, $route, $router } = this, me = this;
		if (! $data.consent) return false;
		if ($data.consent.owner !== $data.userId) return false;
		return $data.consent.status == 'UNCONFIRMED' || $data.consent.status == 'INVALID' || $data.consent.status == 'PRECONFIRMED';
	},
	
	mayDelete() {
        const { $data, $route, $router } = this, me = this;		
		if (! $data.consent) return false;
		if ($data.consent.owner !== $data.userId) return false;
		
		return ($data.consent.status == 'ACTIVE' || $data.consent.status == 'REJECTED' || $data.consent.status == 'EXPIRED' || $data.consent.status == 'INVALID' || $data.consent.status == 'PRECONFIRMED') && ($data.consent.type != 'STUDYPARTICIPATION' && $data.consent.type != 'HEALTHCARE');
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
		return $route.query.actions != null && $data.consent && $data.consent.status != "UNCONFIRMED";
	},
	
	mayBack() {
        const { $data, $route, $router } = this, me = this;
		return !$route.query.actions;
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
		if (item == "organization") return "/images/team.jpeg";
		if (item == "app") return "/images/app.jpg";
		if (item == "community") return "/images/community.jpeg";
		if (item == "external") return "/images/question.jpeg";
		if (item == "reshare") return "/images/community.jpeg";
		if (item == "representative") return "/images/contract.jpeg";
		if (session.user && item._id == session.user._id) return "/images/account.jpg";
		if (item=="member" || item.role == "MEMBER") return "/images/account.jpg";
		if (item=="research" || item.role == "RESEARCH") return "/images/research2.jpeg";
		if (item=="provider" || item.role == "PROVIDER") return "/images/doctor.jpeg";
		return "";
	},
        
    },

	watch:{
		$route (to, from ){
			const { $data, $route } = this, me = this;
			if (to.path.indexOf("consent")>=0) {
				session.currentUser.then(function(userId) {	
	            	me.init(userId);
	        	});
        	}
		}
	},

    created() {
        const { $data, $route } = this, me = this;
        $data.pleaseReview = ($route.query.actions != null);
        session.currentUser.then(function(userId) {	
            me.init(userId);
        });
    }
    
}
</script>