/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
      },
      colors: {
        brand: {
          50:  '#fff8ed',
          100: '#ffefd4',
          200: '#ffdba8',
          300: '#ffc070',
          400: '#fe9d37',
          500: '#fc7c0f',
          600: '#ed5f05',
          700: '#c44508',
          800: '#9c360f',
          900: '#7e2e10',
        },
      },
    },
  },
  plugins: [],
}
