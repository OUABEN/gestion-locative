/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        midnight: {
          DEFAULT: '#0B0B0C',
          50: '#2A2A32',
          100: '#222228',
          200: '#1C1C22',
          card: '#18181C',
          elevated: '#141417',
          border: '#26262C',
          darkest: '#0B0B0C'
        },
        lime: {
          DEFAULT: '#C6F432',
          accent: '#C6F432',
          hover: '#b5e526',
          light: '#e5fa92',
          dark: '#9bc31f'
        },
        coral: {
          DEFAULT: '#FF4D4F',
          50: '#fff1f0',
          100: '#ffccc7',
          500: '#FF4D4F',
          600: '#f5222d'
        },
        canvas: '#F6F7F9'
      },
      boxShadow: {
        'lime-glow': '0 0 25px rgba(198, 244, 50, 0.45)',
        'lime-sm': '0 0 12px rgba(198, 244, 50, 0.35)',
        'midnight-card': '0 8px 30px rgba(0, 0, 0, 0.25)',
        'card-soft': '0 4px 20px rgba(0, 0, 0, 0.04)'
      },
      borderRadius: {
        'button': '14px',
        'card': '22px',
        'hero': '28px'
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'sans-serif'],
        mono: ['JetBrains Mono', 'Fira Code', 'monospace']
      }
    },
  },
  plugins: [],
}
