/*
 * This file is part of the Open MIDATA Platform.
 *
 * The Open MIDATA platform is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Platform is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA platform.  If not, see <http://www.gnu.org/licenses/>.
 */

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
