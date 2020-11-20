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
		items: [
			{
				item_icon: {
					icon: "notifications",
					alt: "notifications",
					badge: "9",
				},
				item_text: {
					text: "Notifications",
				}
			},
			{
				item_icon: {
					icon: "timeline",
					alt: "timeline",
				},
				item_text: {
					text: "Timeline",
					modifier_classes: " mi-at-text--highlighted"
				}
			},
			{
				item_icon: {
					icon: "studies",
					alt: "studies",
				},
				item_text: {
					text: "Studies",
				}
			},
			{
				item_icon: {
					icon: "apps",
					alt: "Apps",
				},
				item_text: {
					text: "Apps",
				}
			},
			{
				item_icon: {
					icon: "suggestions",
					alt: "suggestions",
				},
				item_text: {
					text: "Suggestions",
				}
			},
		]
	}
}
