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
	display: {
	  "padding-top": "30px",
	},

	default: "text",

	context: {
		text_modifier_classes: " mi-at-text--smaller"
	},

	variants: [
		{
			name: "text",
			context: {
				type: "text",
				id: "first-name",
				label: "First name",
			},
		},
		{
			name: "Password",
			context: {
				type: "password",
				label: "Password",
			},
		},
		{
			name: "Date",
			context: {
				type: "date",
				id: "birthdate",
				label: "Date of Birth",
			},
		},
		{
			name: "Email",
			context: {
				type: "email",
				label: "E-mail",
			},
		},
		{
			name: "floating-label",
			label: "Floating Label",
			context: {
				type: "text",
				label: "Text",
				required: "true",
				modifier_classes: " mi-at-input__fieldset--floating-label",
			}
		},
		{
			name: "floating-label-white",
			label: "Floating Label and White",
			display: {
				"background": "linear-gradient(90deg, #32c6b6,  #0879dd);",
			},
			context: {
				type: "text",
				label: "E-mail",
				modifier_classes: " mi-at-input__fieldset--floating-label mi-at-input__fieldset--white",
			},
			notes: "The gradient background is **not** part of the style of the component.<br />It's used here for rendering purposes only.",
		},
		{
			name: "checkbox",
			label: "Checkbox Default",
			context: {
				type: "checkbox",
				label: "I agree to the terms of services",
				modifier_classes: " mi-at-input--checkbox",
			}
		},
		{
			name: "checkbox-white",
			label: "Checkbox White",
			label_modifier_classes: " mi-at-text--white",
			context: {
				type: "checkbox",
				label: "I agree to the terms of services",
				modifier_classes: " mi-at-input--checkbox mi-at-input--checkbox-white",
			},
			display: {
				"background": "linear-gradient(90deg, #32c6b6,  #0879dd);",
			},
		}
	]
}
