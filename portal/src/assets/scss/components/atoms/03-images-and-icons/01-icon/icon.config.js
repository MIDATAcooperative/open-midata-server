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

module.exports = {
	default: "default",
	notes: "You can have a look at the full list of available icons <a href='/docs/icons'>in the docs</a>.",
	variants: [
		{
			name: "default",
			label: "Default",
			context: {
				icon: "timeline",
			}
		},
		{
			name: "close",
			label: "White Close Icon",
			context: {
				icon: "hamburger-close",
				modifier_classes: " mi-at-icon--white",
				alt: "close icon",
			},
			display: {
				"background": "linear-gradient(90deg, #32c6b6,  #0879dd)",
				"padding": "15px"
			},
			notes: "The gradient background and paddings are **not** part of the style of the component.<br />They are used here for rendering purposes only.",
		},
		{
			name: "with-badge",
			label: "Icon with badge",
			context: {
				icon: "notifications",
				alt: "icon with badge",
				badge: 9,
			}
		},
	]
};
