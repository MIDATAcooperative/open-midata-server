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
	default: "midata_message",

	context: {
		message: "Your browser suggest that you are an English speaker, but you might prefer to use MIDATA in another language ?",
	},

	variants: [
		{
			name: "midata_message",
			context: {
				modifier_classes: " mi-at-chat_bubble--midata",
			}
		},
		{
			name: "User message",
			context: {
				text: "Timeline",
				modifier_classes: " mi-at-chat_bubble--user",
			}
		},
		{
			name: "Typing",
			context: {
				modifier_classes: " mi-at-chat_bubble--midata",
				typing: true,
			}
		},
		{
			name: "Choices",
			context: {
				message: "",
				choices: [
					{
						text: "I'm good with English",
					},
					{
						text: "Ja, bitte Deustch sprechen",
					},
					{
						text: "En français s'il-vous-plaît !",
					},
				]
			}
		}
	],
}
