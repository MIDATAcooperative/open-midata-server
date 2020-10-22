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
	context: {
		app: {
			message: {
				text: "requests the permission to access your personal data.",
				modifier_classes: " mi-at-text--smaller mi-at-text--white"
			},
			name: {
				text: "Ally Science",
				modifier_classes: " mi-at-text--smaller mi-at-text--white mi-at-text--highlighted"
			},
			image: {
				url: "/img/placeholder/ally_logo.png",
			}
		},
		fields: [
			{
				input: {
					type: "text",
					id: "username",
					label: "username",
					required: true,
				},
			},
			{
				input: {
					type: "password",
					id: "password",
					label: "password",
					required: true,
				},
			},
		],
		button: {
			button_text : {
				text: "login",
			},
			type: "submit",
			modifier_class: " mi-mo-flat_button--rounded",
		},
		icon: {
			url: "/img/ico/hamburger-close.svg",
			alt: "close",
		},
		link: {
			text: "I don't have an account",
			modifier_classes: " mi-at-text--smallest mi-at-text--white",
			href: "/components/preview/signup_screen"
		},
		legal: {
			text: "(c) 2018 ETH Zürich",
			modifier_classes: " mi-at-text--smallest mi-at-text--white"
		},
		device: {
			text: "Device: #678handy",
			modifier_classes: " mi-at-text--smallest mi-at-text--white"
		}
	},
}
