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
import { setI18n } from 'vue-composable'

function replaceInstr(where) {
    for (let k in where) {
        let v = where[k];
        if (typeof v === "object") replaceInstr(v);
        else {
            v = v.replace(/\{\{/,"{");
            v = v.replace(/\}\}/,"}");
            v = v.replace(/@PLATFORM/, "Midata");
            v = v.replace(/@OPERATOR/, "Midata");
            where[k] = v;
        }
    }
}

let i18n;
let myLocale = ref("en");
let bundles = new Set();
let messages = {};

function mergeLocales(t, s) {
  for (let k in s) 
    if (t[k] && typeof t[k] == "object") mergeLocales(t[k], s[k]); else t[k] = s[k];
}

async function loadLocaleMessages(file, locale) {
  console.log("load: "+file+" "+locale);
  // load locale messages with dynami import
  const msgs = await import(
    /* webpackChunkName: "locale-[request]" */ `./../../src/i18n/${file}_${locale}.json`
  )  
  replaceInstr(msgs.default);
  mergeLocales(messages, msgs.default);
    
} 

async function setLocaleMessages(locale) {
  
  i18n.i18n.value = messages;
  //i18n.removeLocale(locale);
  //console.log(i18n.i18n);
  //i18n.addLocale(locale, messages);
  return nextTick();
}

export const SUPPORT_LOCALES = ['en', 'de', 'fr', 'it']

export function setupI18n(options = { locale: 'en', messages:{} }) {
  i18n = setI18n(options)
  bundles.add("shared");
  setLocale(options.locale)
  return i18n;
}

export async function addBundle(bundlename) {
  if (bundles.has(bundlename)) return;
  bundles.add(bundlename);
  messages = {};
  setLocale(myLocale.value);  
}

export async function setLocale(locale) {
  if (locale != myLocale.value) messages = {};
  myLocale.value = locale;

  i18n.locale.value = locale;
  /*if (i18n.mode === 'legacy') {
    i18n.global.locale = locale
  } else {
    i18n.global.locale.value = locale
  }*/

  for (let bundle of bundles) await loadLocaleMessages(bundle, locale);

  await setLocaleMessages(locale);
 
}

export function getLocale() {
    return myLocale.value;
}

export function getLocaleRef() {
  return myLocale;
}

