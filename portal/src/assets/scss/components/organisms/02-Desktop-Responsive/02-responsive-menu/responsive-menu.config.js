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
	name: "Responsive Menu",
	context: {
		tabs: [
			{
				active: true,
				title: {
					text: "Timeline",
					modifier_classes: " mi-at-text--white mi-at-text--responsive",
				},
				description: {
					text: "Hello Sebastian, here are your weekly stats",
					modifier_classes: " mi-at-text--white mi-at-text--big-title mi-at-text--big-title-responsive"
				}
			},
			{
				title: {
					text: "Studies",
					modifier_classes: " mi-at-text--white mi-at-text--responsive",
				},
				description: {
					text: "You have been enrolled in these studies",
					modifier_classes: " mi-at-text--white mi-at-text--big-title mi-at-text--big-title-responsive"
				}
			},
			{
				title: {
					text: "Apps",
					modifier_classes: " mi-at-text--white mi-at-text--responsive",
				},
				description: {
					text: "Here are the apps that are connected to your MIDATA profile",
					modifier_classes: " mi-at-text--white mi-at-text--big-title mi-at-text--big-title-responsive"
				}
			},
			{
				title: {
					text: "Suggestions",
					modifier_classes: " mi-at-text--white mi-at-text--responsive",
				},
				description: {
					text: "Any suggestions? We'd love to hear from you!",
					modifier_classes: " mi-at-text--white mi-at-text--big-title mi-at-text--big-title-responsive"
				}
			},
		],
		user: {
			avatar: {
				username: "Sebastian",
				initial: "s",
			}
		},
		logo: {
			src: '/img/logo-midata.svg',
			alt: 'MIDATA'
		},
		notifications: {
			icon: "notifications-white",
			alt: "icon with badge",
			badge: 3,
		},
		description: {
			text: "Hello Sebastian, here are your weekly stats",
			modifier_classes: " mi-at-text--white mi-at-text--big-title mi-at-text--big-title-responsive"
		},
		menu: {
			icon: "hamburger-white",
			alt: "menu",
		}
	}
}
