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
