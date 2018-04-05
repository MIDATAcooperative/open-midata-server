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
