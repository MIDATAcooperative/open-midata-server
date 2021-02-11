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

import Newconsent from 'views/shared/newconsent.vue';
import Studydetails from 'views/shared/studydetails.vue';

const Timeline = () => import(/* webpackChunkName: "shared" */ 'views/shared/timeline.vue');
const postRegister = () => import(/* webpackChunkName: "oauth" */ 'views/shared/public/postregister.vue');
const PublicNav = () => import(/* webpackChunkName: "public" */ 'views/nav/public.vue');

const baseRoutes = [  
  {
    path : '/portal',
    name: 'oauth',
    component: () => import(/* webpackChunkName: "oauth" */ 'views/nav/oauthnav.vue')
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
    meta : { role : "provider" }
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
    path : '/',
    redirect : '/public/info'
  }
];

const routes = [
  {
    base : ['oauth'],
    path : 'oauth2',
    component: () => import(/* webpackChunkName: "oauth" */ 'views/shared/public/oauth2.vue')
  },
  {
    base : ['public', 'oauth', "public_research", "public_developer", "public_provider"],
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
    base : ['public', 'oauth'],
    path : 'lostpw',
    component: () => import(/* webpackChunkName: "password" */ 'views/shared/public/lostpw.vue'),
    props : { "role" : "MEMBER" }
  },
  {
    base : ['public', 'oauth'],
    path : 'setpw',
    component: () => import(/* webpackChunkName: "password" */ 'views/shared/public/setpw.vue')
  },
  {
    base : ['public'],
    path : 'info',
    component: () => import(/* webpackChunkName: "public" */ 'views/shared/public/info.vue')
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
    base : ["oauth", "public", "public_provider", "public_research", "public_developer"],
    path : "postregister",
    meta : { keep : true },
    component: postRegister
  },
  {
    base : ["public"],
    path : "confirm/:token",
    name : "confirm",
    meta : { mode : "VALIDATED" },
    component: postRegister
  },
  {
    base : ["public"],
    path : "reject/:token",
    name : "reject",
    meta : { mode : "REJECTED" },
    component: postRegister
  },
  {
    base : ["member", "research", "developer", "provider"],
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
    component: Timeline
  },
  {
    base : ["member", "developer", "admin", "research", "provider"],
    path : "records",
    component: Timeline
  },
  {
    base : ["member", "developer", "admin", "research", "provider"],
    path : "circles",
    component: Timeline
  },

  {
    base : ["member", "developer", "admin", "research", "provider"],
    path : "studies",
    component: Timeline
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
        types : ['CIRCLE','HEALTHCARE']
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
    component: Newconsent // () => import(/* webpackChunkName: "shared" */ 'views/shared/newconsent.vue')
  },
  {
    base : ["public"],
    path : "service",
    component: () => import(/* webpackChunkName: "shared" */ 'views/shared/public/service.vue')
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
    component: () => import(/* webpackChunkName: "shared" */ 'views/shared/public/serviceleave.vue')
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
    base : ["member"],
    path : "studies",
    component: () => import(/* webpackChunkName: "shared" */ 'views/member/smallstudies.vue')
  },
  {
    base : ["member", "provider"],
    path : "studydetails",
    component: () => import(/* webpackChunkName: "shared" */ 'views/shared/studydetails.vue')
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
