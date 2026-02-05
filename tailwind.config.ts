import type { Config } from "tailwindcss";

const config: Config = {
  darkMode: "class",
  content: [
    "./pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./components/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      fontFamily: {
      },
      backgroundImage: {
      },
      colors: {
        'primary': {
          DEFAULT: '',
          light: '',
        },
        'secondary': {
          DEFAULT: '',
          light: '',
        },
        //colores para modo normal

        // Colores específicos para modo oscuro con el color correcto #00304d
        'dark': {
            //crear estilos para modo oscuro
        }
      },
      keyframes: {
        showContent: {
          to: {
            transform: "",
            filter: "",
            opacity: "",
          },
        },
        'spin-slow': {
          to: { transform: '' }
        },
        'spin-reverse': {
          to: { transform: '' }
        },
        'pulse-gentle': {
          '0%, 100%': {
            opacity: '',
            transform: '',
            boxShadow: ''
          },
          '50%': {
            opacity: '',
            transform: '',
            boxShadow: ''
          }
        },
        'float-gentle': {
          '%, %': { transform: '' },
          '%': { transform: '' }
        },
        'glow-pulse': {
          '%, %': { boxShadow: '' },
          '%': { boxShadow: '' }
        },
        'bounce-soft': {
          '%, %, %, %, %': { transform: '' },
          '%, %': { transform: '' },
          '%': { transform: '' },
          '90%': { transform: '' }
        },
        'fade-in-up': {
          from: {
            opacity: '',
            transform: ''
          },
          to: {
            opacity: '',
            transform: ''
          }
        }
      },
      animation: {
        "show-Content": "showContent 0.5s 0.7s ease-in-out 1 forwards",
        'spin-slow': 'spin-slow 8s linear infinite',
        'spin-reverse': 'spin-reverse 6s linear infinite',
        'pulse-gentle': 'pulse-gentle 3s ease-in-out infinite',
        'float-gentle': 'float-gentle 4s ease-in-out infinite',
        'glow-pulse': 'glow-pulse 2s ease-in-out infinite',
        'bounce-soft': 'bounce-soft 2s ease-in-out infinite',
        'fade-in-up': 'fade-in-up 0.6s ease-out'
      },
      screens: {
        'xs': '360px',
        'sm': '640px',
        'md': '768px',
        'lg': '1024px',
        'xl': '1280px',
        '2xl': '1536px'
      }
    },
  },
  plugins: [],
};

export default config;