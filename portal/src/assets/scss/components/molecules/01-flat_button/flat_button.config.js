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

module.exports = {
	default: "edit",

	variants: [
		{
			name: "edit",
			context: {
				button_icon: {
					icon: "edit",
					alt: "edit",
				},
				button_text: {
					text: "Edit profile",
				}
			},
		},
		{
			name: "sign out",
			context: {
				button_icon: {
					icon: "sign-out",
					alt: "sign out",
				},
				button_text: {
					text: "Sign Out",
				}
			},
		},
		{
			name: "submit",
			context: {
				button_text: {
					text: "confirm changes",
				},
				type: "submit",
				modifier_class: " mi-mo-flat_button--submit",
			},
		},
		{
			name: "rounded",
			label: "Rounded Corners",
			context: {
				button_text: {
					text: "login",
				},
				type: "submit",
				modifier_class: " mi-mo-flat_button--rounded",
			},
			display: {
				"background": "linear-gradient(90deg, #32c6b6,  #0879dd);",
				"padding": "30px",
			},
			notes: "The gradient background and text paddings are **not** part of the style of the component.<br />They are used here for rendering purposes only.",
		},
	]
}
