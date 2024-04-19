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

import { nextTick, ref } from 'vue'
import { setI18n, addLocale, removeLocale, VueComposableDevtools } from 'vue-composable'
import ENV from 'config'

function replaceInstr(where) {
   
    for (let k in where) {
        let v = where[k];
        if (typeof v === "object") replaceInstr(v);
        else {
            v = v.replace(/\{\{/,"{");
            v = v.replace(/\}\}/,"}");
            v = v.replace(/@PLATFORM/, ENV.platform);
            v = v.replace(/@OPERATOR/, ENV.operator);
            v = v.replace(/@PRODUCT/, ENV.product);
            v = v.replace(/@SUPPORT/, ENV.support);
            v = v.replace(/@HOMEPAGE/, ENV.homepage);
            v = v.replace(/@DOMAIN/, ENV.domain);
            where[k] = v;    
        }
    }
}

let i18n;
let myLocale = ref("ch");
let targetLocale = null;
let duringUpdate = false;
let bundles = new Set();
let messages = {}; // lang to bundle

function mergeLocales(t, s) {
  for (let k in s) 
    if (typeof s[k] == "object") {
	  if (!t[k]) t[k] = {};
	  mergeLocales(t[k], s[k]); 
    } else t[k] = s[k];
}

async function loadLocaleMessages(file, locale) {  
  let msgs = undefined;
  let fallback = undefined;
  let fblocale = "en";
  if (file=="branding") {
    try {
    msgs = await import(
       /* webpackChunkName: "locale-[request]" */ `override/branding_${locale}.json`    
    )    
    if (locale!='en') fallback = await import(
       /* webpackChunkName: "locale-[request]" */ `override/branding_${fblocale}.json`
    )
    } catch (e) {}
  }
  if (!msgs) {  
    msgs = await import(
      /* webpackChunkName: "locale-[request]" */ `i18n/${file}_${locale}.json`
    )  
    if (locale!='en') fallback = await import(
      /* webpackChunkName: "locale-[request]" */ `i18n/${file}_en.json`
    )
   
  }  
  replaceInstr(msgs.default);
  if (fallback) replaceInstr(fallback.default);
  if (!messages[locale]) messages[locale] = { "common" : { "empty" : " " }};
  if (fallback) mergeLocales(messages[locale], fallback.default);
  mergeLocales(messages[locale], msgs.default);
  
} 

async function setLocaleMessages(locale) {
  //i18n.locale.value = "";
  i18n.removeLocale(locale);
  i18n.addLocale(locale, messages[locale]);
  i18n.locale.value = locale;
  //i18n.i18n.value = messages[locale];  
  return nextTick();
}

export const SUPPORT_LOCALES = ['en', 'de', 'fr', 'it', 'et']

export function setupI18n() {
  i18n = setI18n({ locale: 'ch', messages:{} });
  let lang = localStorage.language || navigator.language || navigator.userLanguage;
  let startWith = "en";
  for (let l of SUPPORT_LOCALES) if (lang.indexOf(l)>=0) startWith = l;
  bundles.add("shared");
  setLocale(startWith);
  return i18n;
}

export async function addBundle(bundlename) {
  if (bundles.has(bundlename)) return;
  console.log("add bundle: "+bundlename);
  bundles.add(bundlename);
  //messages = {};
  await setLocale(myLocale.value);  
}

export async function setLocale(locale) {  
  if (duringUpdate) {
	targetLocale = locale;
	return;
  } else {
	duringUpdate = true;
  }
    
  //if (locale != myLocale.value) messages = {};
  myLocale.value = locale;
  localStorage.language = locale;
  //i18n.locale.value = locale;
   
  for (let bundle of bundles) await loadLocaleMessages(bundle, locale);
  await setLocaleMessages(locale);  
  duringUpdate = false;
  console.log("changed locale to:"+locale);
  if (targetLocale) {
	 let tl = targetLocale;
     targetLocale = null;  
     await setLocale(tl);
  }    
}

export function getLocale() {
    return myLocale.value;
}

export function getLocaleRef() {
  return myLocale;
}

