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
		app: {
			name: {
				text: "Ally Science",
			},
			description: {
				text: "This is a small text describing the related app.",
			},
		},
		intro_message: {
			text: "needs access to your data.",
		},
		required_data: {
			title: {
				text: "This app is asking access to:"
			},
			data: [
				{
					data: {
						text: "Format: fhir/Patient",
					},
				},
				{
					data: {
						text: "Format: fhir/Observation",
					},
				},
				{
					data: {
						text: "Format: fhir/Data",
					},
				},
				{
					data: {
						text: "Format: fhir/Data",
					},
				},
			],
		},
		study: {
			input: {
				label: "Enroll in the following study",
				label_modifier_classes: " mi-at-text--white",
			},
			title: {
				text: "Hamburger forever",
			},
			description: {
				text: "We are looking into whether eating more than 3 hamburgers a day impacts your LDL cholesterol blood level."
			}
		},
		button: {
			button_text : {
				text: "Confirm Access",
			},
			type: "submit",
			modifier_class: " mi-mo-flat_button--rounded",
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
