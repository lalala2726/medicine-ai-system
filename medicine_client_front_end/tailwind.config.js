/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx,js,jsx}'],
  theme: {
    extend: {
      colors: {
        primary: 'var(--nutui-color-primary)'
      }
    }
  },
  plugins: []
}
