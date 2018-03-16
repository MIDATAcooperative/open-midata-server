module.exports = {
	default: "edit",

	variants: [
		{
			name: "edit",
			context: {
				button_icon: {
					icon: "edit",
					alt: "edit",
				},
				button_text: {
					text: "Edit profile",
				}
			},
		},
		{
			name: "sign out",
			context: {
				button_icon: {
					icon: "sign-out",
					alt: "sign out",
				},
				button_text: {
					text: "Sign Out",
				}
			},
		},
		{
			name: "submit",
			context: {
				button_text: {
					text: "confirm changes",
				},
				type: "submit",
				modifier_class: " mi-mo-flat_button--submit",
			},
		},
		{
			name: "rounded",
			label: "Rounded Corners",
			context: {
				button_text: {
					text: "login",
				},
				type: "submit",
				modifier_class: " mi-mo-flat_button--rounded",
			},
			display: {
				"background": "linear-gradient(90deg, #32c6b6,  #0879dd);",
				"padding": "30px",
			},
			notes: "The gradient background and text paddings are **not** part of the style of the component.<br />They are used here for rendering purposes only.",
		},
	]
}
