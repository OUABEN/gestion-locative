/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        navy: {
          50: '#F0F5FA',
          100: '#E1EBF5',
          200: '#C3D7EB',
          300: '#95B8DC',
          400: '#5F92CA',
          500: '#3872B5',
          600: '#255A97',
          700: '#1F4E79', // Couleur maîtresse demandée
          800: '#1A4064',
          900: '#163653',
          950: '#0E2236',
        },
        surface: '#F8FAFC',
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'sans-serif'],
      }
    },
  },
  plugins: [],
}
