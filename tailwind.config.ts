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
            transform: "translateY(0)",
            filter: "blur(0)",
            opacity: "1",
          },
        },
        'spin-slow': {
          to: { transform: 'rotate(360deg)' }
        },
        'spin-reverse': {
          to: { transform: 'rotate(-360deg)' }
        },
        'pulse-gentle': {
          '0%, 100%': {
            opacity: '0.9',
            transform: 'scale(1)',
            boxShadow: '0 10px 25px -5px rgba(0, 0, 0, 0.1)'
          },
          '50%': {
            opacity: '1',
            transform: 'scale(1.02)',
            boxShadow: '0 15px 35px -5px rgba(0, 0, 0, 0.15)'
          }
        },
        'float-gentle': {
          '0%, 100%': { transform: 'translateY(0px)' },
          '50%': { transform: 'translateY(-10px)' }
        },
        'glow-pulse': {
          '0%, 100%': { boxShadow: '0 0 5px rgba(57, 143, 15, 0.2)' },
          '50%': { boxShadow: '0 0 20px rgba(57, 143, 15, 0.4), 0 0 30px rgba(57, 143, 15, 0.2)' }
        },
        'bounce-soft': {
          '0%, 20%, 53%, 80%, 100%': { transform: 'translate3d(0, 0, 0)' },
          '40%, 43%': { transform: 'translate3d(0, -5px, 0)' },
          '70%': { transform: 'translate3d(0, -3px, 0)' },
          '90%': { transform: 'translate3d(0, -1px, 0)' }
        },
        'fade-in-up': {
          from: {
            opacity: '0',
            transform: 'translate3d(0, 30px, 0)'
          },
          to: {
            opacity: '1',
            transform: 'translate3d(0, 0, 0)'
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