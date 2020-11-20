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
	context: {
		description: {
			text: "Please fill in the required information below (fields marked with an asterisk * are mandatory)",
		},
		fields: {
			first_group: [
				{
					input: {
						type: "text",
						id: "email",
						label: "email",
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
				{
					input: {
						type: "password",
						id: "repeat-password",
						label: "Repeat password",
						required: true,
					},
				},
				{
					input: {
						type: "text",
						id: "first-name",
						label: "First Name",
						required: true,
					},
				},
				{
					input: {
						type: "text",
						id: "last-name",
						label: "Last Name",
						required: true,
					},
				},
			],
			second_group: [
				{
					input: {
						type: "date",
						id: "date-of-birth",
						label: "Date of Birth",
						placeholder: "mm/dd/yyyy",
						required: true,
					},
				},
			],
		},
		selects: {
			first: {
				options: [
					{
						option: "Gender *",
						attributes: "selected disabled",
					},
					{
						option: "Male",
					},
					{
						option: "Female",
					},
					{
						option: "Other",
					}
				],
			},
			second: {
				options: [
					{
						option: "Preferred language *",
						attributes: "selected disabled",
					},
					{
						option: "Français",
					},
					{
						option: "Deutsch",
					},
					{
						option: "Italiano",
					},
					{
						option: "English",
					},
				],
			},
			third: {
				options: [
					{
						option: "Country *",
						attributes: "selected disabled",
					},
					{
						option: "Switzerland",
					},
					{
						option: "Austria",
					},
					{
						option: "France",
					},
					{
						option: "Germany",
					},
					{
						option: "Italy",
					}
				],
			}
		},
		optional: {
			title: {
				text: "Have a few more seconds to spare? We'd be happy if you could give us a bit more details about you ☺️ ",
				modifier_classes: " mi-at-text--smallest mi-at-text--white"
			},
			link: {
				text: "I'm feeling generous today, show me the fields!",
				modifier_classes: " mi-at-text--smallest mi-at-text--white"
			},
			close: {
				text: "Hide optional fields",
			},
			fields: [
				{
					input: {
						type: "address",
						label: "Address line 1",
						id: "address-1",
					},
				},
				{
					input: {
						type: "address",
						label: "Address line 2",
						id: "address-2",
					},
				},
				{
					input: {
						type: "text",
						label: "City",
						id: "city",
					},
				},
				{
					input: {
						type: "text",
						label: "PO code / NPP",
						id: "PO-NPP",
					},
				},
				{
					input: {
						type: "phone",
						label: "Landline phone number",
						id: "landline-phone",
					},
				},
				{
					input: {
						type: "text",
						label: "Mobile phone number",
						id: "mobile-phone",
					},
				},
			],
		},
		checkboxes: [
			{
				input: {
					label: "I agree to MIDATA's terms of use",
				},
			},
			{
				input: {
					label: "I agree to MIDATA's privacy terms"
				}
			}
		],
		signup_button: {
			button_text : {
				text: "create account",
			},
			type: "submit",
			modifier_class: " mi-mo-flat_button--rounded",
		},
		icon: {
			url: "/img/ico/hamburger-close.svg",
			alt: "close",
		},
		link: {
			text: "I already have an account",
			modifier_classes: " mi-at-text--smallest mi-at-text--white",
			href: "/components/preview/login_screen",
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
