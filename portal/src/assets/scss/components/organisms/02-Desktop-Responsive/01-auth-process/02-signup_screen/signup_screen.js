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

import BaseView from 'base-view.js';

const SHOW_OPTIONAL_FIELDS_SELECTOR = ".mi-or-signup__fields_optional_invite";
const TARGET_SELECTOR = ".mi-or-signup__content";
const TWO_COLUMNS_MODIFIER_CLASS = "mi-or-signup__content--two-columns";
const CLOSE_OPTIONAL_SELECTOR = ".mi-or-signup__fields_optional_close";

export default class SignupScreen extends BaseView {
	constructor(element) {
		super(element);
			this.optFieldsInvite = this.element.querySelector(SHOW_OPTIONAL_FIELDS_SELECTOR);
	}

	bind() {
		this.on('click', SHOW_OPTIONAL_FIELDS_SELECTOR, this.handleClickOnOptionalFieldsInvite.bind(this));
		this.on('click', CLOSE_OPTIONAL_SELECTOR, this.handleClickOnCloseOptionalFields.bind(this));
	}

	handleClickOnOptionalFieldsInvite() {
		this.addClass(TWO_COLUMNS_MODIFIER_CLASS, document.querySelector(TARGET_SELECTOR));
		document.querySelector(SHOW_OPTIONAL_FIELDS_SELECTOR).style.display = "none";
		document.querySelector(CLOSE_OPTIONAL_SELECTOR).style.display = "block";
	}

	handleClickOnCloseOptionalFields() {
		this.removeClass(TWO_COLUMNS_MODIFIER_CLASS, document.querySelector(TARGET_SELECTOR));
		document.querySelector(SHOW_OPTIONAL_FIELDS_SELECTOR).style.display = "block";
		document.querySelector(CLOSE_OPTIONAL_SELECTOR).style.display = "none";
	}
}
