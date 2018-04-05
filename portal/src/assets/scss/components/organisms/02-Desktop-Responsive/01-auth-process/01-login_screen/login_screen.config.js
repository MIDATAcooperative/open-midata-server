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
