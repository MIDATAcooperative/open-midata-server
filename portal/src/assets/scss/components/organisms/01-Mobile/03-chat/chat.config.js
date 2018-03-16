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
