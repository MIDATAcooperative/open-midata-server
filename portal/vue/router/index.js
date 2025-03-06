/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

import { createRouter, createWebHashHistory } from 'vue-router';

import PatientSearch from 'views/provider/patientsearch.vue';
import SA from 'views/developer/appsubscriptions.vue';

const postRegister = () => import(/* webpackChunkName: "oauth" */ 'views/shared/public/postregister.vue');
const PublicNav = () => import(/* webpackChunkName: "public" */ 'views/nav/public.vue');
const domain = document.location.hostname;

const baseRoutes = [  
  {
    path : '/portal',
    name: 'oauth',
    component: () => import(/* webpackChunkName: "oauth" */ 'views/nav/oauthnav.vue'),
    meta : { role : "member" }
  },
  {
    path : '/public',
    name: 'public',
    component: PublicNav,
    meta : { role : "member" }
  },
  {
    path : '/public_research',
    name: 'public_research',
    component: PublicNav,
    meta : { role : "research" }
  },
  {
    path : '/public_provider',
    name: 'public_provider',
    component: PublicNav,
    meta : { role : "provider", termsRole : "provider" }
  },
  {
    path : '/public_developer',
    name: 'public_developer',
    component: PublicNav,
    meta : { role : "developer" }
  },
  {
    path : '/member',
    name: 'member',
    component: () => import(/* webpackChunkName: "member" */ 'views/nav/member.vue'),
    meta : { role : "member", keep : true }
  },
  {
    path : '/provider',
    name: 'provider',
    component: () => import(/* webpackChunkName: "provider" */ 'views/nav/provider.vue'),
    meta : { role : "provider", termsRole : "provider", keep : true }
  },
  {
    path : '/research',
    name: 'research',
    component: () => import(/* webpackChunkName: "research" */ 'views/nav/research.vue'),
    meta : { role : "research", keep : true }
  },
  {
    path : '/developer',
    name: 'developer',
    component: () => import(/* webpackChunkName: "developer" */ 'views/nav/developer.vue'),
    meta : { role : "developer", keep : true }
  },
  {
    path : '/admin',
    name: 'admin',
    component: () => import(/* webpackChunkName: "admin" */ 'views/nav/admin.vue'),
    meta : { role : "admin", keep : true }
  },
  {
    path : '/portal/confirm/:token',
    redirect : to => ({ path : "/portal/confirm", query : { token : to.params.token }})
  },
  {
    path : '/portal/reject/:token',
    redirect : to => ({ path : "/portal/reject", query : { token : to.params.token }})
  },
  {
    path : '/portal/confirm/:token/:lang',
    redirect : to => ({ path : "/portal/confirm", query : { token : to.params.token, language : to.params.lang }})
  },
  {
    path : '/portal/reject/:token/:lang',
    redirect : to => ({ path : "/portal/reject", query : { token : to.params.token, language : to.params.lang  }})
  },
  {
    path : '/',
    redirect : ((domain.indexOf("ch.midata.coop")>=0) ? '/public/login' : '/public/info')
  }
];

const routes = [
  {
    base : ['oauth'],
    path : 'oauth2',
    component: () => import(/* webpackChunkName: "oauth" */ 'views/shared/public/oauth2.vue')
  },
  {
    base : ['public', 'oauth', "developer", "admin", "public_research", "public_developer", "public_provider"],
    path : 'registration',
    name: 'registration',
    component: () => import(/* webpackChunkName: "oauth" */ 'views/member/public/registration.vue')
  },      
  {
    base : ['oauth'],
    path : 'oauthconfirm',    
    component: () => import(/* webpackChunkName: "oauth" */ 'views/shared/public/confirm.vue')
  },
  {
    base : ["public", "oauth", "public_provider", "public_research", "public_developer"],
    path : 'lostpw',
    component: () => import(/* webpackChunkName: "password" */ 'views/shared/public/lostpw.vue')
  },
  {
    base : ["public", "oauth", "public_provider", "public_research", "public_developer"],
    path : 'setpw',
    component: () => import(/* webpackChunkName: "password" */ 'views/shared/public/setpw.vue')
  },
  {
    base : ['public'],
    path : 'info',
    component: () => import(/* webpackChunkName: "public" */ 'views/shared/public/info.vue')
  },       
  {
    base : ['public', "public_provider", "public_research", "public_developer", "member", "provider", "research", "developer", "admin"],
    path : 'terms',
    component: () => import(/* webpackChunkName: "public" */ 'views/shared/public/termsPage.vue')
  },
  {
    base : ["oauth", "public", "public_provider", "public_research", "public_developer","member", "research", "developer", "provider", "admin"],
    path : "postregister",
    meta : { keep : true },
    component: postRegister
  },
  {
    base : ["public", "oauth"],
    path : "confirm",
    name : "confirm",
    meta : { mode : "VALIDATED", keep : true },
    component: postRegister
  },
  {
    base : ["public", "oauth"],
    path : "reject",
    name : "reject",
    meta : { mode : "REJECTED", keep : true },
    component: postRegister
  },
  {
    base : ["member", "research", "developer", "provider", "admin"],
    path : "upgrade",
    component: postRegister
  },
  { 
    base : ["oauth", "public", "public_provider", "public_research", "public_developer"],
    path : "failure",
    meta : { keep : true },
    component: postRegister
  },
  { 
    base : ["oauth"],
    path : "appendoflife",
    component: () => import(/* webpackChunkName: "shared" */ 'views/shared/public/appexpired.vue')
  },
  {
    base : ["member", "developer", "admin", "research", "provider", "project"],
    path : "spaces",
    component: () => import(/* webpackChunkName: "shared" */ 'views/shared/space.vue')
  },
  {
    base : ["member", "developer", "admin", "research", "provider", "project"],
    path : "importrecords",
    component: () => import(/* webpackChunkName: "shared" */ 'views/shared/importrecords.vue')
  },
  {
    base : ["oauth", "public", "public_research", "public_provider", "public_developer"],
    path : "login",
    component: () => import(/* webpackChunkName: "public" */ 'views/shared/public/login.vue')
  },
  {
    base : ["member", "developer", "admin", "research", "provider"],
    path : "overview",
    component: () => import(/* webpackChunkName: "shared" */ 'views/shared/timeline.vue')
  },  
  {
    base : ["member", "developer", "admin", "research", "provider"],
    path : "dashboard",
    component: () => import(/* webpackChunkName: "shared" */ 'views/shared/dashboard.vue')
  },  
  {
    base : ["member", "developer", "admin", "research", "provider"],
    path : "sandbox",
    component: () => import(/* webpackChunkName: "shared" */ 'views/shared/dashboard.vue'),
    meta : {
      dashId : "sandbox"
    }
  },  
  {
    base : ["member", "developer", "admin", "research", "provider"],
    path : "apps",
    component: () => import(/* webpackChunkName: "shared" */ 'views/shared/apps.vue')
  },
  {
    base : ["member", "developer", "admin", "research", "provider"],
    path : "user",
    component: () => import(/* webpackChunkName: "shared" */ 'views/shared/user.vue')
  },
  {
    base : ["member", "developer", "admin", "research", "provider"],
    path : "user2",
    component: () => import(/* webpackChunkName: "shared" */ 'views/shared/user2.vue'),
    meta : { locked : true }
  },
  {
    base : ["member", "developer", "admin", "research", "provider"],
    path : "accountwipe",
    component: () => import(/* webpackChunkName: "shared" */ 'views/shared/accountwipe.vue')
  },
  {
    base : ["member", "developer", "admin", "research", "provider"],
    path : "changeaddress",
    component: () => import(/* webpackChunkName: "shared" */ 'views/shared/changeaddress.vue')
  },
  {
    base : ["member", "developer", "admin", "research", "provider"],
    path : "changepassword",
    component: () => import(/* webpackChunkName: "shared" */ 'views/shared/changepassword.vue')
  },
  {
    base : ["member", "developer", "admin", "research", "provider"],
    path : "changeemail",
    component: () => import(/* webpackChunkName: "shared" */ 'views/shared/changeemail.vue')
  },
  {
    base : ["member", "developer", "admin", "research", "provider"],
    path : "market",
    component: () => import(/* webpackChunkName: "shared" */ 'views/shared/market.vue')
  },
  {
    base : ["member", "developer", "admin", "research", "provider"],
    path : "visualization",
    component: () => import(/* webpackChunkName: "shared" */ 'views/shared/visualization.vue')
  },
  {
    base : ["member", "developer", "admin", "research", "provider"],
    path : "circles",
    component: () => import(/* webpackChunkName: "shared" */ 'views/shared/consents.vue'),
    member : {
      meta : {
        types : ['CIRCLE','HEALTHCARE', 'REPRESENTATIVE']
      }
    }
  },
  {
    base : ["member", "developer", "admin", "research", "provider"],
    path : "revconsents",
    component: () => import(/* webpackChunkName: "shared" */ 'views/shared/revconsents.vue')
  },
  {
    base : ["member", "developer", "admin", "research", "provider"],
    path : "newconsent",
    component: () => import(/* webpackChunkName: "shared" */ 'views/shared/newconsent.vue')
  },
  {
    base : ["member", "developer", "admin", "research", "provider"],
    path : "editconsent",
    component: () => import(/* webpackChunkName: "shared" */ 'views/shared/newconsent.vue')
  },
  {
    base : ["public"],
    path : "service",
    component: () => import(/* webpackChunkName: "shared" */ 'views/shared/public/service.vue')
  },
  {
    base : ["public", "oauth"],
    path : "unsubscribe",
    component: () => import(/* webpackChunkName: "shared" */ 'views/shared/public/unsubscribe.vue')
  },
  {
    base : ["public", "oauth"],
    path : "account",
    component: () => import(/* webpackChunkName: "shared" */ 'views/shared/public/service.vue'),
    meta : {
      account : true
    }
  },
  {
    base : ["public"],
    path : "/public/apps/:pluginName",
    name : "goplugin",
    component: () => import(/* webpackChunkName: "shared" */ 'views/shared/public/service.vue')
  },
  {
    base : ["member", "provider", "research", "developer", "admin"],
    path : "serviceleave",
    component: () => import(/* webpackChunkName: "shared" */ 'views/shared/public/serviceleave.vue'),
    meta : { lock : true }
  },
  {
    base : ["member", "provider", "research", "developer", "admin"],
    path : "auditlog",
    component: () => import(/* webpackChunkName: "shared" */ 'views/shared/audit.vue')
  },
  {
    base : ["member", "provider", "research", "developer", "admin"],
    path : "records",
    component: () => import(/* webpackChunkName: "shared" */ 'views/shared/records.vue')
  },
  {
    base : ["member", "provider", "research", "developer", "admin"],
    path : "records-delete",
    meta : {
      allowDelete : true  
    },
    admin : {
      meta : {
        allowDelete : true,
        allowDeletePublic : true
      }
    },
    component: () => import(/* webpackChunkName: "shared" */ 'views/shared/records.vue')
  },
  {
    base : ["member", "provider", "research", "developer", "admin"],
    path : "recorddetail",
    component: () => import(/* webpackChunkName: "shared" */ 'views/shared/recorddetail.vue')
  },
  {
    base : ["member", "provider", "research", "developer", "admin"],
    path : "servicekeys",
    component: () => import(/* webpackChunkName: "research" */ 'views/shared/apikeys.vue')
  },
  {
    base : ["developer", "admin"],
    path : "query",
    component: () => import(/* webpackChunkName: "research" */ 'views/shared/queryeditor.vue'),
    meta : {
       mode : "app"
    }
  },
  {
    base : ["research", "developer", "admin"],
    path : "study.query",
    component: () => import(/* webpackChunkName: "research" */ 'views/shared/queryeditor.vue'),
    meta : {
      mode : "study"
    }
  },
  {
    base : ["member"],
    path : "studies",
    component: () => import(/* webpackChunkName: "shared" */ 'views/member/smallstudies.vue')
  },
  {
    base : ["research", "developer", "admin"],
    path : "studies",
    component: () => import(/* webpackChunkName: "research" */ 'views/research/studies.vue')
  },
  {
    base : ["research"],
    path : "organization",
    component: () => import(/* webpackChunkName: "research" */ 'views/research/organization.vue')
  },
  {
    base : ["provider"],
    path : "organization",
    component: () => import(/* webpackChunkName: "provider" */ 'views/provider/organization.vue')
  },
  {
    base : ["research"],
    path : "addresearcher",
    component: () => import(/* webpackChunkName: "research" */ 'views/shared/registerother.vue'),
    meta : {
      mode : "researcher"
    }
  },
  {
    base : ["provider"],
    path : "addprovider",
    component: () => import(/* webpackChunkName: "provider" */ 'views/shared/registerother.vue'),
    meta : {
      mode : "provider"
    }
  },
  {
    base : ["provider", "admin"],
    path : "addorganization",
    component: () => import(/* webpackChunkName: "provider" */ 'views/provider/editorg.vue'),   
  },
  {
    base : ["provider", "admin"],
    path : "updateorganization",
    component: () => import(/* webpackChunkName: "provider" */ 'views/provider/editorg.vue'),   
  },
  {
    base : ["provider", "admin"],
    path : "requestaccess",
    component: () => import(/* webpackChunkName: "provider" */ 'views/provider/requestaccess.vue'),   
  },
  {
    base : ["admin"],
    path : "adminregistration",
    component: () => import(/* webpackChunkName: "research" */ 'views/shared/registerother.vue'),
    meta : {
      mode : "admin"
    }
  },
  {
    base : ["provider"],
    path : "usergroups",
    component: () => import(/* webpackChunkName: "provider" */ 'views/provider/usergroups.vue')    
  },
  {
    base : ["provider", "admin"],
    path : "editusergroup",
    component: () => import(/* webpackChunkName: "provider" */ 'views/provider/editusergroup.vue')    
  },
  {
    base : ["provider"],
    path : "newusergroup",
    component: () => import(/* webpackChunkName: "provider" */ 'views/provider/editusergroup.vue')    
  },
  {
    base : ["provider"],
    path : "memberdetails",
    component: () => import(/* webpackChunkName: "provider" */ 'views/provider/memberdetails.vue')    
  },
  {
    base : ["research"],
    path : "study.addparticipant",
    component: () => import(/* webpackChunkName: "research" */ 'views/shared/registerother.vue'),
    meta : {
      mode : "participant"
    }
  },
  {
    base : ["research", "developer", "admin"],
    path : "createstudy",
    component: () => import(/* webpackChunkName: "research" */ 'views/research/createstudy.vue')
  },
  {
    base : ["research", "developer", "admin"],
    path : "description",
    component: () => import(/* webpackChunkName: "research" */ 'views/research/createstudy.vue')
  },
  {
    base : ["research", "developer", "admin"],
    path : "study.overview",
    component: () => import(/* webpackChunkName: "research" */ 'views/research/studyoverview.vue')
  },
  {
    base : ["research", "developer", "admin"],
    path : "study.info",
    component: () => import(/* webpackChunkName: "research" */ 'views/research/studyinfo.vue')
  },
  {
    base : ["research", "developer", "admin"],
    path : "study.team",
    component: () => import(/* webpackChunkName: "research" */ 'views/research/studyteam.vue')
  },
  {
    base : ["research", "developer", "admin"],
    path : "study.subprojects",
    component: () => import(/* webpackChunkName: "research" */ 'views/research/studysubprojects.vue')
  },
  {
    base : ["research", "developer", "admin"],
    path : "study.fields",
    component: () => import(/* webpackChunkName: "research" */ 'views/research/studyfields.vue')
  },
  {
    base : ["research", "developer", "admin"],
    path : "study.rules",
    component: () => import(/* webpackChunkName: "research" */ 'views/research/studyrules.vue')
  },
  {
      base : ["research", "developer", "admin"],
      path : "study.messages",
      component: () => import(/* webpackChunkName: "research" */ 'views/research/studymessages.vue')
  },
  {
    base : ["research"],
    path : "study.participants",
    component: () => import(/* webpackChunkName: "research" */ 'views/research/studyparticipants.vue')
  },
  {
    base : ["research"],
    path : "study.participant",
    component: () => import(/* webpackChunkName: "research" */ 'views/research/studyparticipant.vue')
  },
  {
    base : ["research"],
    path : "study.records",
    component: () => import(/* webpackChunkName: "research" */ 'views/research/studyrecords.vue')
  },
  {
    base : ["research", "developer", "admin"],
    path : "study.codes",
    component: () => import(/* webpackChunkName: "research" */ 'views/research/codes.vue')
  },
  {
    base : ["research", "developer", "admin"],
    path : "study.sharing",
    component: () => import(/* webpackChunkName: "research" */ 'views/research/sharing.vue')
  },
  {
    base : ["research", "developer", "admin"],
    path : "study.actions",
    component: () => import(/* webpackChunkName: "research" */ 'views/research/studyactions.vue'),
    research : {
      meta : {
        allowPersonalApps : true
      }
    }
  },
  {
    base : ["member", "provider"],
    path : "studydetails",
    component: () => import(/* webpackChunkName: "shared" */ 'views/shared/studydetails.vue')
  },
  {
    base : ["provider"],
    path : "patientsearch",
    component: PatientSearch // 
  },
  {
    base : ["provider"],
    path : "patients",
    component: () => import(/* webpackChunkName: "provider" */ 'views/provider/patients.vue')
  },
  {
    base : ["admin"],
    path : "stats",
    component: () => import(/* webpackChunkName: "admin" */ 'views/admin/stats.vue')
  },
  {
    base : ["admin"],
    path : "usagestats",
    component: () => import(/* webpackChunkName: "admin" */ 'views/admin/usagestats.vue')
  },
  {
    base : ["admin"],
    path : "members",
    component: () => import(/* webpackChunkName: "admin" */ 'views/admin/members.vue')
  },
  {
    base : ["admin"],
    path : "organizations",
    component: () => import(/* webpackChunkName: "admin" */ 'views/admin/organizations.vue')
  },
  {
    base : ["admin"],
    path : "address",
    component: () => import(/* webpackChunkName: "admin" */ 'views/admin/address.vue')
  },
  {
    base : ["admin"],
    path : "pwrecover",
    component: () => import(/* webpackChunkName: "admin" */ 'views/admin/pwrecover.vue')
  },
  {
    base : ["admin"],
    path : "yourapps",
    component: () => import(/* webpackChunkName: "admin" */ 'views/admin/plugins.vue')
  },
  {
    base : ["admin"],
    path : "updplugins",
    component: () => import(/* webpackChunkName: "admin" */ 'views/developer/updplugins.vue')
  },
  {
    base : ["admin"],
    path : "yourapps2",
    component: () => import(/* webpackChunkName: "developer" */ 'views/developer/yourapps.vue')
  },
  {
    base : ["admin", "developer"],
    path : "workspace",
    component: () => import(/* webpackChunkName: "developer" */ 'views/developer/workspace.vue')
  },
  {
    base : ["admin"],
    path : "defineplugin",
    component: () => import(/* webpackChunkName: "admin" */ 'views/admin/defineplugin.vue')
  },
  {
    base : ["admin"],
    path : "definestudy",
    component: () => import(/* webpackChunkName: "admin" */ 'views/admin/definestudy.vue')
  },
  {
    base : ["admin"],
    path : "licenses",
    component: () => import(/* webpackChunkName: "admin" */ 'views/admin/licenses.vue')
  },
  {
    base : ["admin"],
    path : "mails",
    component: () => import(/* webpackChunkName: "admin" */ 'views/admin/mails.vue')
  },
  {
    base : ["admin"],
    path : "news",
    component: () => import(/* webpackChunkName: "admin" */ 'views/admin/news.vue')
  },
  {
    base : ["admin"],
    path : "astudies",
    component: () => import(/* webpackChunkName: "admin" */ 'views/admin/studies.vue')
  },
  {
    base : ["admin"],
    path : "astudy",
    component: () => import(/* webpackChunkName: "admin" */ 'views/admin/study.vue')
  },
  {
    base : ["admin"],
    path : "viewterms",
    component: () => import(/* webpackChunkName: "admin" */ 'views/admin/terms.vue')
  },
  {
    base : ["admin"],
    path : "content",
    component: () => import(/* webpackChunkName: "admin" */ 'views/admin/content.vue')
  },
  {
    base : ["admin"],
    path : "importcontent",
    component: () => import(/* webpackChunkName: "admin" */ 'views/admin/importcontent.vue')
  },
  {
    base : ["admin"],
    path : "newterms",
    component: () => import(/* webpackChunkName: "admin" */ 'views/admin/manageterms.vue')
  },
  {
    base : ["admin"],
    path : "newmail",
    component: () => import(/* webpackChunkName: "admin" */ 'views/admin/managemails.vue'),
    meta : {
      allowDelete : false
    }
  },
  {
    base : ["admin"],
    path : "managemails",
    component: () => import(/* webpackChunkName: "admin" */ 'views/admin/managemails.vue'),
    meta : {
      allowDelete : true
    }
  },
  {
    base : ["admin"],
    path : "newnews",
    component: () => import(/* webpackChunkName: "admin" */ 'views/admin/managenews.vue'),
    meta : {
      allowDelete : false
    }
  },
  {
    base : ["admin"],
    path : "managenews",
    component: () => import(/* webpackChunkName: "admin" */ 'views/admin/managenews.vue'),
    meta : {
      allowDelete : true
    }
  },
  {
    base : ["admin"],
    path : "newlicence",
    component: () => import(/* webpackChunkName: "admin" */ 'views/admin/addlicence.vue'),
    meta : {
      allowDelete : false
    }
  },
  {
    base : ["admin"],
    path : "addlicence",
    component: () => import(/* webpackChunkName: "developer" */ 'views/admin/addlicence.vue'),
    meta : {
      allowDelete : true
    }
  },
  {
    base : ["developer"],
    path : "yourapps",
    component: () => import(/* webpackChunkName: "developer" */ 'views/developer/yourapps.vue')
  },
  {
    base : ["admin", "developer"],
    path : "appsubscriptions",
    component: SA //() => import(/* webpackChunkName: "developer" */ 'views/developer/appsubscriptions.vue')
  },
  {
    base : ["admin", "developer"],
    path : "applink",
    component: () => import(/* webpackChunkName: "developer" */ 'views/developer/applink.vue')
  },
  {
    base : ["admin", "developer"],
    path : "appicon",
    component: () => import(/* webpackChunkName: "developer" */ 'views/developer/appicon.vue')
  },
  {
    base : ["admin", "developer"],
    path : "applogin",
    component: () => import(/* webpackChunkName: "developer" */ 'views/developer/applogin.vue')
  },
  {
    base : ["admin", "developer"],
    path : "appmessages",
    component: () => import(/* webpackChunkName: "developer" */ 'views/developer/appmessages.vue')
  },
  {
    base : ["admin", "developer"],
    path : "applicence",
    component: () => import(/* webpackChunkName: "developer" */ 'views/developer/applicence.vue')
  },
  {
      base : ["admin", "developer"],
      path : "appsmtp",
      component: () => import(/* webpackChunkName: "developer" */ 'views/developer/appsmtp.vue')
  },
  {
    base : ["admin", "developer"],
    path : "repository",
    component: () => import(/* webpackChunkName: "developer" */ 'views/developer/repository.vue')
  },
  {
    base : ["admin", "developer"],
    path : "appstats",
    component: () => import(/* webpackChunkName: "developer" */ 'views/developer/appstats.vue')
  },
  {
    base : ["admin", "developer"],
    path : "appdebug",
    component: () => import(/* webpackChunkName: "developer" */ 'views/developer/appdebug.vue')
  },
  {
    base : ["admin", "developer"],
    path : "services",
    component: () => import(/* webpackChunkName: "developer" */ 'views/developer/services.vue')
  },
  {
    base : ["admin", "developer"],
    path : "autoimport",
    component: () => import(/* webpackChunkName: "developer" */ 'views/developer/autoimport.vue')
  },
  {
    base : ["admin", "developer"],
    path : "testusers",
    component: () => import(/* webpackChunkName: "developer" */ 'views/developer/testusers.vue')
  },
  {
    base : ["admin", "developer"],
    path : "appreviews",
    component: () => import(/* webpackChunkName: "developer" */ 'views/developer/appreviews.vue'),
    developer : {
      meta : {
        allowReview : false        
      }
    },
    admin : {
      meta : {
        allowReview : true
      }
    }
  },
  {
    base : ["admin", "developer"],
    path : "manageapp",
    component: () => import(/* webpackChunkName: "developer" */ 'views/developer/overview.vue'),
    developer : {
      meta : {
        allowDelete : false,
        allowStudyConfig : false,
        allowExport : false
      }
    },
    admin : {
      meta : {
        allowDelete : true,
        allowStudyConfig : true,
        allowExport : true
      }
    }
  },
  {
    base : ["admin", "developer"],
    path : "editapp",
    component: () => import(/* webpackChunkName: "developer" */ 'views/developer/manageapp.vue'),
    developer : {
      meta : {
        allowDelete : false,
        allowStudyConfig : false,
        allowExport : false
      }
    },
    admin : {
      meta : {
        allowDelete : true,
        allowStudyConfig : true,
        allowExport : true
      }
    }
  },
  {
    base : ["admin", "developer"],
    path : "registerapp",
    component: () => import(/* webpackChunkName: "developer" */ 'views/developer/manageapp.vue'),
    developer : {
      meta : {
        allowDelete : false,
        allowStudyConfig : false,
        allowExport : false
      }
    },
    admin : {
      meta : {
        allowDelete : false,
        allowStudyConfig : false,
        allowExport : false
      }
    }
  }
]

function processRoutes(baseRoutes, routes) {
   let categories = {};
   for (let route of baseRoutes) {      
      let name = route.name;
      if (name) {
        route.children = categories[name] = [];
      }
   }
   for (let route of routes) {
      if (!route.base) console.log("Missing base in router:"+route.path);
      for (let target of route.base) {
        if (categories[target]) {
          let r = {
            path : route.path,
            name : target+"."+(route.name||route.path),
            component : route.component
          }
          if (route.props) r.props = route.props;
          if (route.meta) r.meta = route.meta;
          if (route[target]) {
            if (route[target].props) r.props = route[target].props;
            if (route[target].meta) r.meta = route[target].meta;
          }
          categories[target].push(r);
        }
      }
   }
   return baseRoutes;
}

const router = createRouter({
  history: createWebHashHistory(),
  routes: processRoutes(baseRoutes, routes)
})

export default router
