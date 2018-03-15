import BaseView from 'base-view.js';

const AVATAR_SELECTOR = ".mi-at-avatar";
const POPOVER_SELECTOR = ".mi-or-responsive_menu__popover";
const ICON_CLOSE_SELECTOR = ".mi-at-icon-close";

const VISIBLE_PLACEHOLDER_CLASS = "mi-x-is_visible";
const INVERT_AVATAR_CLASS = "mi-at-avatar--invert";

export default class ResponsiveMenu extends BaseView {
	constructor(element) {
		super(element);
		this.popover = document.querySelector(POPOVER_SELECTOR);
		this.avatar = document.querySelector(AVATAR_SELECTOR);
	}

	bind() {
		this.on('click', AVATAR_SELECTOR, this.togglePopOverVisibility.bind(this));
		this.on('click', ICON_CLOSE_SELECTOR, this.togglePopOverVisibility.bind(this));
	}

	invertAvatar() {
		this.avatar.classList.toggle(INVERT_AVATAR_CLASS);
	}

	togglePopOverVisibility() {
		this.popover.classList.toggle(VISIBLE_PLACEHOLDER_CLASS);
		this.invertAvatar();
	}
}
