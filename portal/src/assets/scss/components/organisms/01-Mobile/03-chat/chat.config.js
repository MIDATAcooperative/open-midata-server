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
	default: "multiple-choices",

	variants: [
		{
			name: "multiple-choices",
			label: "Example 1: Ask a Question with Multiple Choices",
			context: {
				messages: [
					{
						message: "Welcome to MIDATA!",
						modifier_classes: " mi-at-chat_bubble--midata mi-x-is_first_midata"
					},
					{
						message: "Here you can keep all your health data safely.",
						modifier_classes: " mi-at-chat_bubble--midata",
					},
					{
						message: "Your browser suggest that you are an English speaker, but you might prefer to use MIDATA in another language ?",
						modifier_classes: " mi-at-chat_bubble--midata mi-x-is_last_midata",
						// typing: " true"
					},
					{
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
				]
			},
		},
		{
			name: "midata-typing",
			label: "Example 2: User replies and waits while MIDATA is typing",
			context: {
				messages: [
					{
						message: "Welcome to MIDATA!",
						modifier_classes: " mi-at-chat_bubble--midata mi-x-is_first_midata"
					},
					{
						message: "Here you can keep all your health data safely.",
						modifier_classes: " mi-at-chat_bubble--midata",
					},
					{
						message: "Your browser suggest that you are an English speaker, but you might prefer to use MIDATA in another language ?",
						modifier_classes: " mi-at-chat_bubble--midata mi-x-is_last_midata",
						// typing: " true"
					},
					{
						message: "I'm good with English",
						modifier_classes: " mi-at-chat_bubble--user mi-x-is_first_user"
					},
					{
						message: "Thanks",
						modifier_classes: " mi-at-chat_bubble--user"
					},
					{
						message: "So what do you want to know about me?",
						modifier_classes: " mi-at-chat_bubble--user mi-x-is_last_user"
					},
					{
						message: "Let's see how MIDATA can improve your health ;-)",
						modifier_classes: " mi-at-chat_bubble--midata",
						typing: true,
					}
				]
			},
		},
		{
			name: "is-scrolled",
			label: "Example 3: Chat Window Is Scrolled",
			context: {
				modifier_classes: " mi-x-is_scrolled",
				messages: [
					{
						message: "Welcome to MIDATA!",
						modifier_classes: " mi-at-chat_bubble--midata mi-x-is_first_midata"
					},
					{
						message: "Here you can keep all your health data safely.",
						modifier_classes: " mi-at-chat_bubble--midata",
					},
					{
						message: "Your browser suggest that you are an English speaker, but you might prefer to use MIDATA in another language ?",
						modifier_classes: " mi-at-chat_bubble--midata mi-x-is_last_midata",
						// typing: " true"
					},
					{
						message: "I'm good with English",
						modifier_classes: " mi-at-chat_bubble--user mi-x-is_first_user"
					},
					{
						message: "Thanks",
						modifier_classes: " mi-at-chat_bubble--user"
					},
					{
						message: "So what do you want to know about me?",
						modifier_classes: " mi-at-chat_bubble--user mi-x-is_last_user"
					},
					{
						message: "Let's see how MIDATA can improve your health ;-)",
						modifier_classes: " mi-at-chat_bubble--midata mi-x-is_last_midata",
						typing: true,
					}
				]
			},
		}
	]
}
