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
	status: "wip",

	context: {
		fieldsets: [
			{
				type: "text",
				id: "first-name",
				label: "First Name",
			},
			{
				type: "text",
				id: "last-name",
				label: "Last Name",
			},
			{
				type: "date",
				id: "birthdate",
				label: "Date of Birth",
			},
			{
				type: "text",
				id: "gender",
				label: "gender",
			},
			{
				type: "email",
				label: "Email",
			},
			{
				type: "password",
				label: "Password",
			},
			{
				type: "text",
				id: "country",
				label: "Country"
			}
		]
	}
}
