module.exports = {
  parserOptions: {
    parser: 'babel-eslint'
  },
  extends: [
    'eslint:recommended',
    'plugin:vue/vue3-recommended',
  ],
  plugins: [
    'vue'
  ],
  rules : {
    'vue/max-attributes-per-line' : 'off'
  }
}
